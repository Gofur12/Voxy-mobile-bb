package me.cortex.voxy.client.core.rendering.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.cortex.voxy.client.core.gl.GlBuffer;
import me.cortex.voxy.client.core.gl.GlFence;
import me.cortex.voxy.client.core.gl.GlPersistentMappedBuffer;
import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.AllocationArena;
import me.cortex.voxy.common.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.Consumer;

import static me.cortex.voxy.common.util.AllocationArena.SIZE_LIMIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_READ_BIT;

public class DownloadStream {
    private static final int BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    private static final int CLIENT_MAPPED_BUFFER_BARRIER_BIT = 0x00004000;
    public interface DownloadResultConsumer {
        void consume(long ptr, long size);
    }

    private final AllocationArena allocationArena = new AllocationArena();
    private final GlPersistentMappedBuffer downloadBuffer;
    private final boolean persistentMapping;
    private final long contextGeneration;
    private boolean closed;
    private final Deque<CpuDownload> cpuDownloads = new ArrayDeque<>();

    private final Deque<DownloadFrame> frames = new ArrayDeque<>();
    private final LongArrayList thisFrameAllocations = new LongArrayList();
    private final Deque<DownloadData> downloadList = new ArrayDeque<>();
    private final ArrayList<DownloadData> thisFrameDownloadList = new ArrayList<>();

    public DownloadStream(long size) {
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Download stream size must be between 1 and Integer.MAX_VALUE");
        }
        var backend = GlBackendRuntime.requireBackend();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        var capabilities = backend.capabilities();
        this.persistentMapping = capabilities.bufferStorage && capabilities.persistentMapping;
        if (this.persistentMapping) {
            this.downloadBuffer = new GlPersistentMappedBuffer(size, GL_MAP_READ_BIT);
        } else {
            this.downloadBuffer = null;
        }
        this.allocationArena.setLimit(size);
    }

    private void requireCurrentContext() {
        if (this.closed) throw new IllegalStateException("DownloadStream has been shut down");
        GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream");
    }

    private long caddr = -1;
    private long offset = 0;

    //Pulls the entire buffer from the gpu
    public void download(GlBuffer buffer, DownloadResultConsumer resultConsumer) {
        this.download(buffer, 0, buffer.size(), resultConsumer);
    }

    public void download(GlBuffer buffer, Consumer<MemoryBuffer> resultConsumer) {
        this.download(buffer, 0, buffer.size(), resultConsumer);
    }

    public void download(GlBuffer buffer, long downloadOffset, long size, Consumer<MemoryBuffer> consumer) {
        this.download(buffer, downloadOffset, size, (ptr,size2)-> {
            consumer.accept(MemoryBuffer.createUntrackedUnfreeableRawFrom(ptr, size));
        });
    }

    public void download(GlBuffer buffer, long downloadOffset, long size, DownloadResultConsumer resultConsumer) {
        this.requireCurrentContext();
        if (size > Integer.MAX_VALUE || size <= 0 || downloadOffset < 0
                || downloadOffset > buffer.size() || size > buffer.size() - downloadOffset) {
            throw new IllegalArgumentException("Invalid download range");
        }

        if (!this.persistentMapping) {
            this.downloadCpu(buffer, downloadOffset, size, resultConsumer);
            return;
        }

        long addr;
        if (this.caddr == -1 || !this.allocationArena.expand(this.caddr, (int) size)) {
            this.caddr = this.allocationArena.alloc((int) size);
            if (this.caddr == SIZE_LIMIT) {
                Logger.warn("Download stream full, committing and waiting for old download frames");
                this.commit();
                int attempts = 10;
                while (--attempts != 0 && this.caddr == SIZE_LIMIT) {
                    GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").finish();
                    this.tick();
                    this.caddr = this.allocationArena.alloc((int) size);
                }
                if (this.caddr == SIZE_LIMIT) {
                    throw new IllegalStateException("Could not allocate a download segment after waiting for previous frames");
                }
            }
            this.thisFrameAllocations.add(this.caddr);
            this.offset = size;
            addr = this.caddr;
        } else {
            addr = this.caddr + this.offset;
            this.offset += size;
        }

        if (this.caddr + size > this.downloadBuffer.size()) {
            throw new IllegalStateException("Download allocation exceeds mapped buffer capacity");
        }

        this.downloadList.add(new DownloadData(buffer, addr, downloadOffset, size, resultConsumer));
        this.commit();
    }

    /**
     * GLES 3.x fallback: map the source range for read, copy it into native
     * memory, unmap immediately, and deliver the stable CPU copy on tick().
     * Mapping may synchronize with the GPU, so this path is intentionally
     * synchronous rather than pretending to provide persistent asynchronous readback.
     */
    private void downloadCpu(GlBuffer buffer, long sourceOffset, long size, DownloadResultConsumer consumer) {
        GlBackend backend = GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream");
        long destination = MemoryUtil.nmemAlloc(size);
        if (destination == 0L) {
            throw new OutOfMemoryError("Unable to allocate CPU download staging memory (" + size + " bytes)");
        }

        long mapped = 0L;
        Throwable failure = null;
        try {
            mapped = backend.mapBufferRange(buffer.id, sourceOffset, size, GL_MAP_READ_BIT);
            if (mapped == 0L) {
                throw new IllegalStateException("Backend returned a null pointer for a readable buffer mapping");
            }
            MemoryUtil.memCopy(mapped, destination, size);
        } catch (RuntimeException | Error readbackFailure) {
            failure = readbackFailure;
        } finally {
            if (mapped != 0L) {
                try {
                    if (!backend.unmapBuffer(buffer.id)) {
                        failure = appendFailure(failure,
                                new IllegalStateException("GPU buffer contents became invalid while unmapping readback range"));
                    }
                } catch (RuntimeException | Error unmapFailure) {
                    failure = appendFailure(failure, unmapFailure);
                }
            }
        }
        if (failure != null) {
            MemoryUtil.nmemFree(destination);
            rethrowFailure(failure);
        }
        this.cpuDownloads.add(new CpuDownload(destination, size, consumer));
    }

    public void commit() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            return;
        }
        if (this.downloadList.isEmpty()) {
            return;
        }
        GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").memoryBarrier(BUFFER_UPDATE_BARRIER_BIT);
        for (var entry : this.downloadList) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").copyBufferSubData(entry.target.id, this.downloadBuffer.id, entry.targetOffset, entry.downloadStreamOffset, entry.size);
        }
        GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").memoryBarrier(CLIENT_MAPPED_BUFFER_BARRIER_BIT | BUFFER_UPDATE_BARRIER_BIT);
        this.thisFrameDownloadList.addAll(this.downloadList);
        this.downloadList.clear();
        this.caddr = -1;
        this.offset = 0;
    }

    public void tick() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            Throwable failure = null;
            while (!this.cpuDownloads.isEmpty()) {
                CpuDownload download = this.cpuDownloads.removeFirst();
                try {
                    download.resultConsumer.consume(download.address, download.size);
                } catch (RuntimeException | Error callbackFailure) {
                    failure = appendFailure(failure, callbackFailure);
                } finally {
                    MemoryUtil.nmemFree(download.address);
                }
            }
            rethrowFailure(failure);
            return;
        }

        this.commit();
        if (!this.thisFrameAllocations.isEmpty()) {
            this.frames.add(new DownloadFrame(new GlFence(), new LongArrayList(this.thisFrameAllocations), new ArrayList<>(this.thisFrameDownloadList)));
            this.thisFrameAllocations.clear();
            this.thisFrameDownloadList.clear();
        }

        Throwable failure = null;
        while (!this.frames.isEmpty()) {
            DownloadFrame frame = this.frames.peekFirst();
            if (!frame.fence.signaled()) break;
            this.frames.removeFirst();
            for (var data : frame.data) {
                try {
                    data.resultConsumer.consume(this.downloadBuffer.addr() + data.downloadStreamOffset, data.size);
                } catch (RuntimeException | Error callbackFailure) {
                    failure = appendFailure(failure, callbackFailure);
                }
            }
            try {
                frame.allocations.forEach(this.allocationArena::free);
            } catch (RuntimeException | Error cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
            try {
                frame.fence.free();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
        }
        rethrowFailure(failure);
    }

    // Synchronous fallback has already completed its GPU readback by this point.
    public void waitDiscard() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            while (!this.cpuDownloads.isEmpty()) {
                MemoryUtil.nmemFree(this.cpuDownloads.removeFirst().address);
            }
            return;
        }

        GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").finish();

        // The GPU is idle now: discard queued callbacks and release every
        // allocation, including copies committed since the last tick/fence.
        this.downloadList.clear();
        this.thisFrameDownloadList.clear();
        Throwable failure = null;
        for (int i = 0; i < this.thisFrameAllocations.size(); i++) {
            try {
                this.allocationArena.free(this.thisFrameAllocations.getLong(i));
            } catch (RuntimeException | Error cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
        }
        this.thisFrameAllocations.clear();
        this.caddr = -1;
        this.offset = 0;

        while (!this.frames.isEmpty()) {
            DownloadFrame frame = this.frames.removeFirst();
            for (int i = 0; i < frame.allocations.size(); i++) {
                try {
                    this.allocationArena.free(frame.allocations.getLong(i));
                } catch (RuntimeException | Error cleanupFailure) {
                    failure = appendFailure(failure, cleanupFailure);
                }
            }
            try {
                frame.fence.free();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
        }
        rethrowFailure(failure);
    }

    private static Throwable appendFailure(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void rethrowFailure(Throwable failure) {
        if (failure == null) return;
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
        throw new IllegalStateException(failure);
    }

    public void flushWaitClear() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            this.tick();
            if (!this.cpuDownloads.isEmpty()) {
                throw new IllegalStateException("CPU download queue was not drained");
            }
            return;
        }
        GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").finish();
        this.tick();
        var fence = new GlFence();
        GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").finish();
        while (!fence.signaled()) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").finish();
            Thread.onSpinWait();
        }
        fence.free();
        this.tick();
        if (!this.frames.isEmpty()) {
            throw new IllegalStateException();
        }
    }

    private record CpuDownload(long address, long size, DownloadResultConsumer resultConsumer) {}
    private record DownloadFrame(GlFence fence, LongArrayList allocations, ArrayList<DownloadData> data) {}
    private record DownloadData(GlBuffer target, long downloadStreamOffset, long targetOffset, long size, DownloadResultConsumer resultConsumer) {}

    // Created only after the graphics backend is active.
    public static volatile DownloadStream INSTANCE;

    public static synchronized DownloadStream initialize() {
        GlBackendRuntime.requireBackend();
        long generation = GlBackendRuntime.getContextGeneration();
        if (INSTANCE != null && INSTANCE.contextGeneration == generation && !INSTANCE.closed) return INSTANCE;
        DownloadStream previous = INSTANCE;
        INSTANCE = null;
        if (previous != null) previous.closeResources();
        DownloadStream created = new DownloadStream(1 << 25);
        INSTANCE = created;
        return created;
    }

    public static DownloadStream requireInstance() {
        DownloadStream stream = INSTANCE;
        if (stream == null) throw new IllegalStateException("DownloadStream has not been initialized for the active graphics context");
        stream.requireCurrentContext();
        return stream;
    }

    public static synchronized void shutdown() {
        DownloadStream previous = INSTANCE;
        INSTANCE = null;
        if (previous != null) previous.closeResources();
    }

    private void closeResources() {
        if (this.closed) return;
        Throwable failure = null;
        boolean contextCurrent = GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration);
        if (contextCurrent) {
            try { this.flushWaitClear(); } catch (RuntimeException | Error flushFailure) { failure = appendFailure(failure, flushFailure); }
            try { GlBackendRuntime.requireBackend(this.contextGeneration, "DownloadStream").finish(); }
            catch (RuntimeException | Error finishFailure) { failure = appendFailure(failure, finishFailure); }
        }
        this.closed = true;
        while (!this.cpuDownloads.isEmpty()) MemoryUtil.nmemFree(this.cpuDownloads.removeFirst().address);
        this.downloadList.clear();
        this.thisFrameDownloadList.clear();
        this.thisFrameAllocations.clear();
        this.caddr = -1;
        this.offset = 0;
        while (!this.frames.isEmpty()) {
            DownloadFrame frame = this.frames.removeFirst();
            try { frame.fence.free(); } catch (RuntimeException | Error fenceFailure) { failure = appendFailure(failure, fenceFailure); }
        }
        if (this.downloadBuffer != null) {
            try { this.downloadBuffer.free(); } catch (RuntimeException | Error bufferFailure) { failure = appendFailure(failure, bufferFailure); }
        }
        rethrowFailure(failure);
    }

    // Global download stream
}

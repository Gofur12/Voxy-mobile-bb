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
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL30C.GL_MAP_READ_BIT;
import static org.lwjgl.opengl.GL42.GL_BUFFER_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.opengl.GL45.glCopyNamedBufferSubData;

public class DownloadStream {
    public interface DownloadResultConsumer {
        void consume(long ptr, long size);
    }

    private final AllocationArena allocationArena = new AllocationArena();
    private final GlPersistentMappedBuffer downloadBuffer;
    private final boolean persistentMapping;
    private final Deque<CpuDownload> cpuDownloads = new ArrayDeque<>();

    private final Deque<DownloadFrame> frames = new ArrayDeque<>();
    private final LongArrayList thisFrameAllocations = new LongArrayList();
    private final Deque<DownloadData> downloadList = new ArrayDeque<>();
    private final ArrayList<DownloadData> thisFrameDownloadList = new ArrayList<>();

    public DownloadStream(long size) {
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Download stream size must be between 1 and Integer.MAX_VALUE");
        }
        var capabilities = GlBackendRuntime.requireBackend().capabilities();
        this.persistentMapping = capabilities.bufferStorage && capabilities.persistentMapping;
        if (this.persistentMapping) {
            this.downloadBuffer = new GlPersistentMappedBuffer(size, GL_MAP_READ_BIT);
        } else {
            this.downloadBuffer = null;
        }
        this.allocationArena.setLimit(size);
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
                    glFinish();
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
        GlBackend backend = GlBackendRuntime.requireBackend();
        long destination = MemoryUtil.nmemAlloc(size);
        if (destination == 0L) {
            throw new OutOfMemoryError("Unable to allocate CPU download staging memory (" + size + " bytes)");
        }

        long mapped = 0L;
        boolean mappedSuccessfully = false;
        try {
            mapped = backend.mapBufferRange(buffer.id, sourceOffset, size, GL_MAP_READ_BIT);
            if (mapped == 0L) {
                throw new IllegalStateException("Backend returned a null pointer for a readable buffer mapping");
            }
            mappedSuccessfully = true;
            MemoryUtil.memCopy(mapped, destination, size);
        } finally {
            if (mappedSuccessfully && !backend.unmapBuffer(buffer.id)) {
                MemoryUtil.nmemFree(destination);
                throw new IllegalStateException("GPU buffer contents became invalid while unmapping readback range");
            }
            if (!mappedSuccessfully) {
                MemoryUtil.nmemFree(destination);
            }
        }
        this.cpuDownloads.add(new CpuDownload(destination, size, consumer));
    }

    public void commit() {
        if (!this.persistentMapping) {
            return;
        }
        if (this.downloadList.isEmpty()) {
            return;
        }
        glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);
        for (var entry : this.downloadList) {
            glCopyNamedBufferSubData(entry.target.id, this.downloadBuffer.id, entry.targetOffset, entry.downloadStreamOffset, entry.size);
        }
        glMemoryBarrier(GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT);
        this.thisFrameDownloadList.addAll(this.downloadList);
        this.downloadList.clear();
        this.caddr = -1;
        this.offset = 0;
    }

    public void tick() {
        if (!this.persistentMapping) {
            while (!this.cpuDownloads.isEmpty()) {
                CpuDownload download = this.cpuDownloads.removeFirst();
                try {
                    download.resultConsumer.consume(download.address, download.size);
                } finally {
                    MemoryUtil.nmemFree(download.address);
                }
            }
            return;
        }

        this.commit();
        if (!this.thisFrameAllocations.isEmpty()) {
            this.frames.add(new DownloadFrame(new GlFence(), new LongArrayList(this.thisFrameAllocations), new ArrayList<>(this.thisFrameDownloadList)));
            this.thisFrameAllocations.clear();
            this.thisFrameDownloadList.clear();
        }

        while (!this.frames.isEmpty()) {
            if (!this.frames.peek().fence.signaled()) {
                break;
            }
            var frame = this.frames.pop();
            for (var data : frame.data) {
                data.resultConsumer.consume(this.downloadBuffer.addr() + data.downloadStreamOffset, data.size);
            }
            frame.allocations.forEach(this.allocationArena::free);
            frame.fence.free();
        }
    }

    // Synchronous fallback has already completed its GPU readback by this point.
    public void waitDiscard() {
        if (!this.persistentMapping) {
            while (!this.cpuDownloads.isEmpty()) {
                MemoryUtil.nmemFree(this.cpuDownloads.removeFirst().address);
            }
            return;
        }
        glFinish();
        var fence = new GlFence();
        glFinish();
        while (!fence.signaled()) Thread.onSpinWait();
        fence.free();
        while (!this.frames.isEmpty()) {
            var frame = this.frames.pop();
            while (!frame.fence.signaled()) Thread.onSpinWait();
            frame.allocations.forEach(this.allocationArena::free);
            frame.fence.free();
        }
    }

    public void flushWaitClear() {
        if (!this.persistentMapping) {
            this.tick();
            if (!this.cpuDownloads.isEmpty()) {
                throw new IllegalStateException("CPU download queue was not drained");
            }
            return;
        }
        glFinish();
        this.tick();
        var fence = new GlFence();
        glFinish();
        while (!fence.signaled()) {
            glFinish();
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

    // Global download stream
    public static final DownloadStream INSTANCE = new DownloadStream(1<<25);//32 mb download buffer
}

package me.cortex.voxy.client.core.rendering.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.cortex.voxy.client.core.gl.GlBuffer;
import me.cortex.voxy.client.core.gl.GlFence;
import me.cortex.voxy.client.core.gl.GlPersistentMappedBuffer;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.AllocationArena;
import me.cortex.voxy.common.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.Deque;

import static me.cortex.voxy.common.util.AllocationArena.SIZE_LIMIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_UNSYNCHRONIZED_BIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_FLUSH_EXPLICIT_BIT;
import static org.lwjgl.opengl.GL44C.GL_CLIENT_STORAGE_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_COHERENT_BIT;

public class UploadStream {
    // Upload offsets are copied into target buffers; they are not SSBO binding offsets.
    // 16-byte alignment is sufficient and avoids probing desktop-only Capabilities.
    public static final int BASE_ALLOCATION_ALIGNEMENT = 16;

    private final AllocationArena allocationArena = new AllocationArena();
    private final GlPersistentMappedBuffer uploadBuffer;
    private final long cpuStagingAddress;
    private final long capacity;
    private final boolean persistentMapping;
    private final long contextGeneration;
    private boolean closed;

    private final Deque<UploadFrame> frames = new ArrayDeque<>();
    private final LongArrayList thisFrameAllocations = new LongArrayList();
    private final Deque<UploadData> uploadList = new ArrayDeque<>();

    private static final boolean USE_COHERENT = false;

    public UploadStream(long size) {
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Upload stream size must be between 1 and Integer.MAX_VALUE");
        }
        this.capacity = size;
        var backend = GlBackendRuntime.requireBackend();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        var capabilities = backend.capabilities();
        this.persistentMapping = capabilities.bufferStorage && capabilities.persistentMapping;
        if (this.persistentMapping) {
            this.uploadBuffer = new GlPersistentMappedBuffer(size, GL_CLIENT_STORAGE_BIT | GL_MAP_WRITE_BIT
                    | GL_MAP_UNSYNCHRONIZED_BIT | (USE_COHERENT ? GL_MAP_COHERENT_BIT : GL_MAP_FLUSH_EXPLICIT_BIT))
                    .name("UploadStream");
            this.cpuStagingAddress = 0L;
        } else {
            this.uploadBuffer = null;
            this.cpuStagingAddress = MemoryUtil.nmemAlloc(size);
            if (this.cpuStagingAddress == 0L) {
                throw new OutOfMemoryError("Unable to allocate CPU upload staging memory (" + size + " bytes)");
            }
        }
        this.allocationArena.setLimit(size);
    }

    private void requireCurrentContext() {
        if (this.closed) throw new IllegalStateException("UploadStream has been shut down");
        GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream");
    }

    private long caddr = -1;
    private long offset = 0;

    public void upload(GlBuffer buffer, long destOffset, MemoryBuffer data) {
        data.cpyTo(this.upload(buffer, destOffset, data.size));
    }

    public long uploadTo(GlBuffer buffer) {
        return this.upload(buffer, 0, buffer.size());
    }

    public long upload(GlBuffer buffer, long destOffset, long size) {
        this.requireCurrentContext();
        buffer.assertContextCurrent();
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Upload size is outside the supported range");
        }
        if (destOffset < 0 || destOffset > buffer.size() || size > buffer.size() - destOffset) {
            throw new IllegalArgumentException("Upload destination range is outside the target buffer");
        }
        long uploadOffset = this.rawUploadAddress((int) size);
        this.uploadList.add(new UploadData(buffer, uploadOffset, destOffset, size));
        return this.uploadAddress(uploadOffset);
    }

    /**
     * Allocates a raw pointer into the upload buffer without registering a destination copy.
     * This is only meaningful when persistent GPU mapping is available. CPU staging mode has
     * no corresponding GPU buffer, so accepting this call would silently discard the bytes
     * when commit() resets the staging arena.
     */
    public long rawUpload(int size) {
        if (!this.persistentMapping) {
            throw new UnsupportedOperationException("Raw upload requires persistent GPU mapping; use upload(target, offset, size) in CPU staging mode");
        }
        return this.uploadAddress(this.rawUploadAddress(size));
    }

    private long uploadAddress(long allocationOffset) {
        return this.persistentMapping
                ? this.uploadBuffer.addr() + allocationOffset
                : this.cpuStagingAddress + allocationOffset;
    }

    public long rawUploadAddress(int size) {
        this.requireCurrentContext();
        if (size < 0) {
            throw new IllegalStateException("Negative size");
        }
        if (size == 0) {
            throw new IllegalArgumentException("Zero-sized upload allocations are not supported");
        }
        size = alignUp(size, BASE_ALLOCATION_ALIGNEMENT);
        if (size > (this.persistentMapping ? this.uploadBuffer.size() : this.allocationArena.getLimit())) {
            throw new IllegalArgumentException("Upload allocation exceeds stream capacity");
        }

        if (!this.persistentMapping) {
            long allocation = this.allocationArena.alloc(size);
            if (allocation == SIZE_LIMIT) {
                throw new IllegalStateException("CPU upload staging arena is full; commit uploads before allocating more");
            }
            try {
                this.thisFrameAllocations.add(allocation);
            } catch (RuntimeException | Error trackingFailure) {
                try {
                    this.allocationArena.free(allocation);
                } catch (RuntimeException | Error cleanupFailure) {
                    if (trackingFailure != cleanupFailure) trackingFailure.addSuppressed(cleanupFailure);
                }
                throw trackingFailure;
            }
            return allocation;
        }

        long addr;
        if (this.caddr == -1 || !this.allocationArena.expand(this.caddr, size)) {
            if ((!USE_COHERENT) && this.caddr != -1) {
                GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").flushMappedBufferRange(this.uploadBuffer.id, this.caddr, this.offset);
            }
            this.caddr = this.allocationArena.alloc(size);
            if (this.caddr == SIZE_LIMIT) {
                Logger.error("Upload stream full, preemptively waiting for old upload frames");
                int attempts = 10;
                while (--attempts != 0 && this.caddr == SIZE_LIMIT) {
                    GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").finish();
                    this.tick(false);
                    this.caddr = this.allocationArena.alloc(size);
                }
                if (this.caddr == SIZE_LIMIT) {
                    throw new IllegalStateException("Could not allocate upload segment after waiting for previous frames");
                }
            }
            try {
                this.thisFrameAllocations.add(this.caddr);
            } catch (RuntimeException | Error trackingFailure) {
                try {
                    this.allocationArena.free(this.caddr);
                } catch (RuntimeException | Error cleanupFailure) {
                    if (trackingFailure != cleanupFailure) trackingFailure.addSuppressed(cleanupFailure);
                }
                this.caddr = -1;
                this.offset = 0;
                throw trackingFailure;
            }
            this.offset = size;
            addr = this.caddr;
        } else {
            addr = this.caddr + this.offset;
            this.offset += size;
        }

        if (this.caddr + size > this.uploadBuffer.size()) {
            throw new IllegalStateException("Upload allocation exceeds mapped buffer capacity");
        }
        return addr;
    }

    public void commit() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            for (var entry : this.uploadList) {
                GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").uploadBuffer(entry.target.id, entry.targetOffset,
                        entry.size, this.cpuStagingAddress + entry.uploadOffset);
            }
            this.uploadList.clear();
            // uploadBuffer is a synchronous client-memory upload call: once it returns,
            // the source bytes may be reused without a GPU fence.
            this.thisFrameAllocations.clear();
            this.allocationArena.reset();
            this.allocationArena.setLimit(this.capacity);
            return;
        }

        if ((!USE_COHERENT) && this.caddr != -1) {
            GlBackendRuntime.requireBackend().flushMappedBufferRange(this.uploadBuffer.id, this.caddr, this.offset);
        }
        if (this.uploadList.isEmpty()) {
            // Raw uploads may have allocated and written a segment without
            // registering a target-copy entry. Close that segment before tick()
            // fences it so the next frame cannot append into in-flight memory.
            this.caddr = -1;
            this.offset = 0;
            return;
        }
        GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").memoryBarrier(0x00000200); // GL_BUFFER_UPDATE_BARRIER_BIT
        for (var entry : this.uploadList) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").copyBufferSubData(this.uploadBuffer.id, entry.target.id,
                    entry.uploadOffset, entry.targetOffset, entry.size);
        }
        this.uploadList.clear();
        GlBackendRuntime.requireBackend().memoryBarrier(0x00000200);
        this.caddr = -1;
        this.offset = 0;
    }

    public void tick() {
        this.tick(true);
    }

    private void tick(boolean commit) {
        if (commit) {
            this.commit();
        }
        if (!this.persistentMapping) {
            return;
        }
        if (!this.thisFrameAllocations.isEmpty()) {
            // Keep the allocation list authoritative until the fence and frame
            // are both enqueued successfully.
            LongArrayList allocations = new LongArrayList(this.thisFrameAllocations);
            GlFence fence = new GlFence();
            try {
                this.frames.add(new UploadFrame(fence, allocations));
            } catch (RuntimeException | Error enqueueFailure) {
                try {
                    fence.free();
                } catch (RuntimeException | Error fenceFailure) {
                    if (enqueueFailure != fenceFailure) enqueueFailure.addSuppressed(fenceFailure);
                }
                throw enqueueFailure;
            }
            this.thisFrameAllocations.clear();
        }
        while (!this.frames.isEmpty()) {
            if (!this.frames.peek().fence.signaled()) {
                break;
            }
            var frame = this.frames.pop();
            frame.allocations.forEach(this.allocationArena::free);
            frame.fence.free();
        }
    }

    public long getBaseAddress() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            throw new UnsupportedOperationException("CPU staging mode has no GPU-mapped base buffer");
        }
        return this.uploadBuffer.addr();
    }

    public int getRawBufferId() {
        this.requireCurrentContext();
        if (!this.persistentMapping) {
            throw new UnsupportedOperationException("CPU staging mode has no GPU upload buffer object");
        }
        return this.uploadBuffer.id;
    }

    private record UploadFrame(GlFence fence, LongArrayList allocations) {}
    private record UploadData(GlBuffer target, long uploadOffset, long targetOffset, long size) {}

    // Created only after the graphics backend is active; never allocate GPU state at class load.
    public static volatile UploadStream INSTANCE;

    public static synchronized UploadStream initialize() {
        GlBackendRuntime.requireBackend();
        long generation = GlBackendRuntime.getContextGeneration();
        if (INSTANCE != null && INSTANCE.contextGeneration == generation && !INSTANCE.closed) return INSTANCE;
        UploadStream previous = INSTANCE;
        INSTANCE = null;
        if (previous != null) previous.closeResources();
        UploadStream created = new UploadStream(1 << 26);
        INSTANCE = created;
        return created;
    }

    public static UploadStream requireInstance() {
        UploadStream stream = INSTANCE;
        if (stream == null) throw new IllegalStateException("UploadStream has not been initialized for the active graphics context");
        stream.requireCurrentContext();
        return stream;
    }

    public static synchronized void shutdown() {
        UploadStream previous = INSTANCE;
        INSTANCE = null;
        if (previous != null) previous.closeResources();
    }

    private void closeResources() {
        if (this.closed) return;
        this.closed = true;
        Throwable failure = null;
        boolean contextCurrent = GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration);
        // CPU staging uploads consume the client-memory source synchronously.
        // Persistent-mapped GPU segments, however, must not be unmapped or
        // deleted until the owning context confirms that queued work is idle.
        boolean gpuQuiesced = !this.persistentMapping;
        if (contextCurrent) {
            try {
                this.commitForShutdown();
            } catch (RuntimeException | Error e) {
                failure = appendFailure(failure, e);
            }
            if (this.persistentMapping) {
                try {
                    GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").finish();
                    gpuQuiesced = true;
                } catch (RuntimeException | Error e) {
                    failure = appendFailure(failure, e);
                }
            }
        }

        if (gpuQuiesced) {
            while (!this.frames.isEmpty()) {
                try { this.frames.removeFirst().fence.free(); } catch (RuntimeException | Error e) { failure = appendFailure(failure, e); }
            }
            if (this.uploadBuffer != null) {
                try { this.uploadBuffer.free(); } catch (RuntimeException | Error e) { failure = appendFailure(failure, e); }
            }
        } else {
            // Context loss or failed finish: discard Java references but never
            // issue GL deletion against resources that may still be in use.
            this.frames.clear();
        }
        this.thisFrameAllocations.clear();
        this.uploadList.clear();
        this.caddr = -1;
        this.offset = 0;
        if (this.cpuStagingAddress != 0L) MemoryUtil.nmemFree(this.cpuStagingAddress);
        rethrowFailure(failure);
    }

    private void commitForShutdown() {
        if (!this.persistentMapping) {
            for (var entry : this.uploadList) GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").uploadBuffer(entry.target.id, entry.targetOffset, entry.size, this.cpuStagingAddress + entry.uploadOffset);
            return;
        }
        if (!USE_COHERENT && this.caddr != -1) GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").flushMappedBufferRange(this.uploadBuffer.id, this.caddr, this.offset);
        if (!this.uploadList.isEmpty()) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").memoryBarrier(0x00000200);
            for (var entry : this.uploadList) GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").copyBufferSubData(this.uploadBuffer.id, entry.target.id, entry.uploadOffset, entry.targetOffset, entry.size);
            GlBackendRuntime.requireBackend(this.contextGeneration, "UploadStream").memoryBarrier(0x00000200);
        }
    }

    private static Throwable appendFailure(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void rethrowFailure(Throwable failure) {
        if (failure instanceof RuntimeException e) throw e;
        if (failure instanceof Error e) throw e;
    }

    public static long alignUp(long val, long alignment) {
        return ((val + alignment - 1) / alignment) * alignment;
    }

    public static int alignUp(int val, int alignment) {
        return ((val + alignment - 1) / alignment) * alignment;
    }

    public static int alignUpAlloc(int val) {
        return alignUp(val, BASE_ALLOCATION_ALIGNEMENT);
    }
}

package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.MemoryBuffer;
import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.ARBSparseBuffer.GL_SPARSE_STORAGE_BIT_ARB;

public class GlBuffer extends TrackedObject {
    public final int id;
    private final long size;
    private final int flags;

    private static int COUNT;
    private static long TOTAL_SIZE;

    public GlBuffer(long size) {
        this(size, 0);
    }

    public GlBuffer(MemoryBuffer buffer) {
        this(buffer.size, 0, false, buffer.address);
    }

    public GlBuffer(long size, boolean zero) {
        this(size, 0, zero);
    }

    public GlBuffer(long size, int flags) {
        this(size, flags, true);
    }

    public GlBuffer(long size, int flags, boolean zero) {
        this(size, flags, zero, 0);
    }

    private GlBuffer(long size, int flags, boolean zero, long data) {
        this.flags = flags;
        this.size = size;
        this.id = GlBackendRuntime.requireBackend().createBuffer(size, data, flags);
        if ((flags&GL_SPARSE_STORAGE_BIT_ARB)==0 && zero) {
            this.zero();
        }

        COUNT++;
        TOTAL_SIZE += size;
    }

    @Override
    public void free() {
        this.free0();
        GlBackendRuntime.requireBackend().deleteBuffer(this.id);

        COUNT--;
        TOTAL_SIZE -= this.size;
    }

    public boolean isSparse() {
        return (this.flags&GL_SPARSE_STORAGE_BIT_ARB)!=0;
    }

    public long size() {
        return this.size;
    }

    /**
     * Fills the complete buffer with zero bytes using bounded CPU staging.
     * This deliberately avoids desktop-only named-buffer clear operations.
     */
    public GlBuffer zero() {
        this.uploadRepeated(0, this.size, 0, true);
        return this;
    }

    public GlBuffer zeroRange(long offset, long size) {
        this.validateRange(offset, size);
        this.uploadRepeated(offset, size, 0, true);
        return this;
    }

    /**
     * Repeats the supplied 32-bit value over the buffer, matching the old
     * GL_R32UI clear semantics without requiring glClearBufferData.
     */
    public GlBuffer fill(int data) {
        if ((this.size & 3L) != 0L) {
            throw new IllegalStateException("32-bit fill requires a buffer size divisible by 4");
        }
        this.uploadRepeated(0, this.size, data, false);
        return this;
    }

    private void validateRange(long offset, long length) {
        if (offset < 0 || length < 0 || offset > this.size || length > this.size - offset) {
            throw new IllegalArgumentException("Buffer upload range is outside the allocation");
        }
    }

    private void uploadRepeated(long offset, long length, int pattern, boolean zero) {
        this.validateRange(offset, length);
        if (length == 0) return;

        final int chunkCapacity = 64 * 1024;
        final long staging = zero
                ? MemoryUtil.nmemCalloc(1, chunkCapacity)
                : MemoryUtil.nmemAlloc(chunkCapacity);
        if (staging == 0L) {
            throw new OutOfMemoryError("Unable to allocate 64 KiB buffer staging block");
        }

        try {
            if (!zero) {
                for (int i = 0; i < chunkCapacity; i += Integer.BYTES) {
                    MemoryUtil.memPutInt(staging + i, pattern);
                }
            }

            GlBackend backend = GlBackendRuntime.requireBackend();
            long remaining = length;
            long destination = offset;
            while (remaining > 0) {
                long amount = Math.min(remaining, chunkCapacity);
                if (!zero && (amount & 3L) != 0L) {
                    throw new IllegalArgumentException("32-bit fill range must be divisible by 4");
                }
                backend.uploadBuffer(this.id, destination, amount, staging);
                destination += amount;
                remaining -= amount;
            }
        } finally {
            MemoryUtil.nmemFree(staging);
        }
    }

    public static int getCount() {
        return COUNT;
    }

    public static long getTotalSize() {
        return TOTAL_SIZE;
    }

    public GlBuffer name(String name) {
        return GlDebug.name(name, this);
    }
}

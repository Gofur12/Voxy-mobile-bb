package me.cortex.voxy.client.core.rendering.util;

import me.cortex.voxy.client.core.gl.GlBuffer;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.common.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;


//Has a base index buffer of 16380 quads, and also a 1 cube byte index buffer at the end
public class SharedIndexBuffer {
    public static final int CUBE_INDEX_OFFSET = (1<<16)*6*2;
    /**
     * GPU-backed variants are created only after the current graphics backend
     * is initialized. They are nullable between context lifetimes.
     */
    public static volatile SharedIndexBuffer INSTANCE;
    public static volatile SharedIndexBuffer INSTANCE_BYTE;
    public static volatile SharedIndexBuffer INSTANCE_BB_BYTE;

    private final GlBuffer indexBuffer;
    private final long contextGeneration;

    /**
     * Creates the shared index-buffer variants for the active graphics context.
     * Construction is transactional: partially created buffers are released
     * and no static reference is published if any variant fails.
     */
    public static synchronized void initialize() {
        GlBackendRuntime.requireBackend();
        long generation = GlBackendRuntime.getContextGeneration();
        if (INSTANCE != null && INSTANCE_BYTE != null && INSTANCE_BB_BYTE != null
                && INSTANCE.contextGeneration == generation
                && INSTANCE_BYTE.contextGeneration == generation
                && INSTANCE_BB_BYTE.contextGeneration == generation) {
            return;
        }

        shutdown();

        SharedIndexBuffer shortIndices = null;
        SharedIndexBuffer byteIndices = null;
        SharedIndexBuffer byteCubeIndices = null;
        try {
            shortIndices = new SharedIndexBuffer();
            byteIndices = new SharedIndexBuffer(true);
            byteCubeIndices = new SharedIndexBuffer(true, true);

            INSTANCE = shortIndices;
            INSTANCE_BYTE = byteIndices;
            INSTANCE_BB_BYTE = byteCubeIndices;
        } catch (RuntimeException | Error failure) {
            freeAfterFailedInitialization(byteCubeIndices, failure);
            freeAfterFailedInitialization(byteIndices, failure);
            freeAfterFailedInitialization(shortIndices, failure);
            throw failure;
        }
    }

    private static void freeAfterFailedInitialization(SharedIndexBuffer buffer, Throwable failure) {
        if (buffer == null) return;
        try {
            buffer.free();
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
        }
    }

    /**
     * Releases all shared index buffers. Safe after context loss because the
     * underlying GlBuffer wrapper skips deletion for stale context generations.
     */
    public static synchronized void shutdown() {
        SharedIndexBuffer shortIndices = INSTANCE;
        SharedIndexBuffer byteIndices = INSTANCE_BYTE;
        SharedIndexBuffer byteCubeIndices = INSTANCE_BB_BYTE;
        INSTANCE = null;
        INSTANCE_BYTE = null;
        INSTANCE_BB_BYTE = null;

        Throwable failure = null;
        failure = freeCollecting(shortIndices, failure);
        if (byteIndices != shortIndices) failure = freeCollecting(byteIndices, failure);
        if (byteCubeIndices != shortIndices && byteCubeIndices != byteIndices) {
            failure = freeCollecting(byteCubeIndices, failure);
        }
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
    }

    private static Throwable freeCollecting(SharedIndexBuffer buffer, Throwable failure) {
        if (buffer == null) return failure;
        try {
            buffer.free();
        } catch (RuntimeException | Error cleanupFailure) {
            if (failure == null) return cleanupFailure;
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
        }
        return failure;
    }

    public SharedIndexBuffer() {
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        this.indexBuffer = new GlBuffer((1<<16)*6*2 + 6*2*3);
        var quadIndexBuff = generateQuadIndicesShort(16380);
        var cubeBuff = generateCubeIndexBuffer();

        long ptr = UploadStream.INSTANCE.upload(this.indexBuffer, 0, this.indexBuffer.size());
        quadIndexBuff.cpyTo(ptr);
        cubeBuff.cpyTo((1<<16)*2*6 + ptr);

        quadIndexBuff.free();
        cubeBuff.free();
        UploadStream.INSTANCE.commit();
    }

    private SharedIndexBuffer(boolean type2) {
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        this.indexBuffer = new GlBuffer((1<<8)*6 + 6*2*3);
        var quadIndexBuff = generateQuadIndicesByte(63);
        var cubeBuff = generateCubeIndexBuffer();

        long ptr = UploadStream.INSTANCE.upload(this.indexBuffer, 0, this.indexBuffer.size());
        quadIndexBuff.cpyTo(ptr);
        cubeBuff.cpyTo((1<<8)*6 + ptr);

        quadIndexBuff.free();
        cubeBuff.free();
        UploadStream.INSTANCE.commit();
    }

    private SharedIndexBuffer(boolean type2, boolean type3) {
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        this.indexBuffer = new GlBuffer(6*2*3*(256/8));
        var cubeBuff = generateByteCubesIndexBuffer(256/8);

        cubeBuff.cpyTo(UploadStream.INSTANCE.upload(this.indexBuffer, 0, this.indexBuffer.size()));
        UploadStream.INSTANCE.commit();
        cubeBuff.free();
    }

    private static MemoryBuffer generateCubeIndexBuffer() {
        var buffer = new MemoryBuffer(6*2*3);
        long ptr = buffer.address;
        MemoryUtil.memSet(ptr, 0, 6*2*3 );

        //Bottom face
        MemoryUtil.memPutByte(ptr++, (byte) 0);
        MemoryUtil.memPutByte(ptr++, (byte) 1);
        MemoryUtil.memPutByte(ptr++, (byte) 2);
        MemoryUtil.memPutByte(ptr++, (byte) 3);
        MemoryUtil.memPutByte(ptr++, (byte) 2);
        MemoryUtil.memPutByte(ptr++, (byte) 1);

        //top face
        MemoryUtil.memPutByte(ptr++, (byte) 6);
        MemoryUtil.memPutByte(ptr++, (byte) 5);
        MemoryUtil.memPutByte(ptr++, (byte) 4);
        MemoryUtil.memPutByte(ptr++, (byte) 5);
        MemoryUtil.memPutByte(ptr++, (byte) 6);
        MemoryUtil.memPutByte(ptr++, (byte) 7);

        //north face
        MemoryUtil.memPutByte(ptr++, (byte) 0);
        MemoryUtil.memPutByte(ptr++, (byte) 4);
        MemoryUtil.memPutByte(ptr++, (byte) 1);
        MemoryUtil.memPutByte(ptr++, (byte) 5);
        MemoryUtil.memPutByte(ptr++, (byte) 1);
        MemoryUtil.memPutByte(ptr++, (byte) 4);

        //south face
        MemoryUtil.memPutByte(ptr++, (byte) 3);
        MemoryUtil.memPutByte(ptr++, (byte) 6);
        MemoryUtil.memPutByte(ptr++, (byte) 2);
        MemoryUtil.memPutByte(ptr++, (byte) 6);
        MemoryUtil.memPutByte(ptr++, (byte) 3);
        MemoryUtil.memPutByte(ptr++, (byte) 7);

        //west face
        MemoryUtil.memPutByte(ptr++, (byte) 2);
        MemoryUtil.memPutByte(ptr++, (byte) 4);
        MemoryUtil.memPutByte(ptr++, (byte) 0);
        MemoryUtil.memPutByte(ptr++, (byte) 4);
        MemoryUtil.memPutByte(ptr++, (byte) 2);
        MemoryUtil.memPutByte(ptr++, (byte) 6);

        //east face
        MemoryUtil.memPutByte(ptr++, (byte) 1);
        MemoryUtil.memPutByte(ptr++, (byte) 5);
        MemoryUtil.memPutByte(ptr++, (byte) 3);
        MemoryUtil.memPutByte(ptr++, (byte) 7);
        MemoryUtil.memPutByte(ptr++, (byte) 3);
        MemoryUtil.memPutByte(ptr++, (byte) 5);

        return buffer;
    }

    private static MemoryBuffer generateByteCubesIndexBuffer(int cnt) {
        var buffer = new MemoryBuffer((long) cnt *6*2*3);
        long ptr = buffer.address;
        MemoryUtil.memSet(ptr, 0, buffer.size);

        for (int i = 0; i < cnt; i++) {
            int j = i*8;

            //Bottom face
            MemoryUtil.memPutByte(ptr++, (byte) (0+j));
            MemoryUtil.memPutByte(ptr++, (byte) (1+j));
            MemoryUtil.memPutByte(ptr++, (byte) (2+j));
            MemoryUtil.memPutByte(ptr++, (byte) (3+j));
            MemoryUtil.memPutByte(ptr++, (byte) (2+j));
            MemoryUtil.memPutByte(ptr++, (byte) (1+j));

            //top face
            MemoryUtil.memPutByte(ptr++, (byte) (6+j));
            MemoryUtil.memPutByte(ptr++, (byte) (5+j));
            MemoryUtil.memPutByte(ptr++, (byte) (4+j));
            MemoryUtil.memPutByte(ptr++, (byte) (5+j));
            MemoryUtil.memPutByte(ptr++, (byte) (6+j));
            MemoryUtil.memPutByte(ptr++, (byte) (7+j));

            //north face
            MemoryUtil.memPutByte(ptr++, (byte) (0+j));
            MemoryUtil.memPutByte(ptr++, (byte) (4+j));
            MemoryUtil.memPutByte(ptr++, (byte) (1+j));
            MemoryUtil.memPutByte(ptr++, (byte) (5+j));
            MemoryUtil.memPutByte(ptr++, (byte) (1+j));
            MemoryUtil.memPutByte(ptr++, (byte) (4+j));

            //south face
            MemoryUtil.memPutByte(ptr++, (byte) (3+j));
            MemoryUtil.memPutByte(ptr++, (byte) (6+j));
            MemoryUtil.memPutByte(ptr++, (byte) (2+j));
            MemoryUtil.memPutByte(ptr++, (byte) (6+j));
            MemoryUtil.memPutByte(ptr++, (byte) (3+j));
            MemoryUtil.memPutByte(ptr++, (byte) (7+j));

            //west face
            MemoryUtil.memPutByte(ptr++, (byte) (2+j));
            MemoryUtil.memPutByte(ptr++, (byte) (4+j));
            MemoryUtil.memPutByte(ptr++, (byte) (0+j));
            MemoryUtil.memPutByte(ptr++, (byte) (4+j));
            MemoryUtil.memPutByte(ptr++, (byte) (2+j));
            MemoryUtil.memPutByte(ptr++, (byte) (6+j));

            //east face
            MemoryUtil.memPutByte(ptr++, (byte) (1+j));
            MemoryUtil.memPutByte(ptr++, (byte) (5+j));
            MemoryUtil.memPutByte(ptr++, (byte) (3+j));
            MemoryUtil.memPutByte(ptr++, (byte) (7+j));
            MemoryUtil.memPutByte(ptr++, (byte) (3+j));
            MemoryUtil.memPutByte(ptr++, (byte) (5+j));
        }

        return buffer;
    }

    public static MemoryBuffer generateQuadIndicesByte(int quadCount) {
        if ((quadCount*4) >= 1<<8) {
            throw new IllegalArgumentException("Quad count to large");
        }
        MemoryBuffer buffer = new MemoryBuffer(quadCount * 6L);
        long ptr = buffer.address;
        for(int i = 0; i < quadCount*4; i += 4) {
            MemoryUtil.memPutByte(ptr + (0), (byte) (i + 1));
            MemoryUtil.memPutByte(ptr + (1), (byte) (i + 2));
            MemoryUtil.memPutByte(ptr + (2), (byte) (i + 0));
            MemoryUtil.memPutByte(ptr + (3), (byte) (i + 1));
            MemoryUtil.memPutByte(ptr + (4), (byte) (i + 3));
            MemoryUtil.memPutByte(ptr + (5), (byte) (i + 2));

            ptr += 6;
        }

        return buffer;
    }
    public static MemoryBuffer generateQuadIndicesShort(int quadCount) {
        if ((quadCount*4) >= 1<<16) {
            throw new IllegalArgumentException("Quad count to large");
        }
        MemoryBuffer buffer = new MemoryBuffer(quadCount * 6L * 2);
        long ptr = buffer.address;
        for(int i = 0; i < quadCount*4; i += 4) {
            MemoryUtil.memPutShort(ptr + (0*2), (short) (i + 1));
            MemoryUtil.memPutShort(ptr + (1*2), (short) (i + 2));
            MemoryUtil.memPutShort(ptr + (2*2), (short) (i + 0));
            MemoryUtil.memPutShort(ptr + (3*2), (short) (i + 1));
            MemoryUtil.memPutShort(ptr + (4*2), (short) (i + 3));
            MemoryUtil.memPutShort(ptr + (5*2), (short) (i + 2));

            ptr += 6 * 2;
        }

        return buffer;
    }

    public static MemoryBuffer generateQuadIndicesInt(int quadCount) {
        MemoryBuffer buffer = new MemoryBuffer(quadCount * 6L * 2);
        long ptr = buffer.address;
        for(int i = 0; i < quadCount*4; i += 4) {
            MemoryUtil.memPutInt(ptr + (0*4), i);
            MemoryUtil.memPutInt(ptr + (1*4), (i + 1));
            MemoryUtil.memPutInt(ptr + (2*4), (i + 2));
            MemoryUtil.memPutInt(ptr + (3*4), (i + 1));
            MemoryUtil.memPutInt(ptr + (4*4), (i + 3));
            MemoryUtil.memPutInt(ptr + (5*4), (i + 2));
            ptr += 6 * 4;
        }
        return buffer;
    }

    public int id() {
        GlBackendRuntime.requireBackend(this.contextGeneration, "SharedIndexBuffer");
        return this.indexBuffer.id;
    }

    public void free() {
        this.indexBuffer.free();
    }
}

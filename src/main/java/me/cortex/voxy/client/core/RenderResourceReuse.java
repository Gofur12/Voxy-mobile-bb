package me.cortex.voxy.client.core;

import me.cortex.voxy.client.core.gl.Capabilities;
import me.cortex.voxy.client.core.gl.GlBuffer;
import me.cortex.voxy.client.core.gl.GlTexture;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.client.core.model.ModelFactory;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.ThreadUtils;

import java.util.ArrayList;

import static org.lwjgl.opengl.ARBSparseBuffer.GL_SPARSE_STORAGE_BIT_ARB;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11C.*;

//System to allow reuse/recycling of render buffer/texture allocations
// specifically the geometry buffer and texture atlas allocation
public class RenderResourceReuse {
    private static final ArrayList<GlTexture> MODEL_TEXTURE_CACHE = new ArrayList<>();
    private static final ArrayList<GlBuffer> GEOMETRY_BUFFER_CACHE = new ArrayList<>();
    private static long cacheContextGeneration = -1;

    private static long requireCurrentGeneration() {
        GlBackendRuntime.requireBackend();
        return GlBackendRuntime.getContextGeneration();
    }

    private static void synchronizeCacheGeneration(long generation) {
        if (cacheContextGeneration == -1) {
            cacheContextGeneration = generation;
        } else if (cacheContextGeneration != generation) {
            clearResourcesInternal();
            cacheContextGeneration = generation;
        }
    }

    // Clears cached wrappers even after context loss; stale wrappers skip GL deletion.
    public static synchronized void clearResources() {
        clearResourcesInternal();
        cacheContextGeneration = -1;
    }

    private static void clearResourcesInternal() {
        ArrayList<GlTexture> textures = new ArrayList<>(MODEL_TEXTURE_CACHE);
        ArrayList<GlBuffer> buffers = new ArrayList<>(GEOMETRY_BUFFER_CACHE);
        MODEL_TEXTURE_CACHE.clear();
        GEOMETRY_BUFFER_CACHE.clear();

        Throwable failure = null;
        for (GlTexture texture : textures) {
            try {
                texture.free();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
        }
        for (GlBuffer buffer : buffers) {
            try {
                buffer.free();
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
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
    }

    public static synchronized GlTexture getOrCreateModelStoreTextureAtlas() {
        long generation = requireCurrentGeneration();
        synchronizeCacheGeneration(generation);
        if (!MODEL_TEXTURE_CACHE.isEmpty()) {
            GlTexture cached = MODEL_TEXTURE_CACHE.remove(0);
            cached.assertContextCurrent();
            cached.assertAllocated();
            return cached.zero();
        }

        GlTexture atlas = new GlTexture();
        try {
            return atlas.store(GL_RGBA8,
                    Integer.numberOfTrailingZeros(ModelFactory.MODEL_TEXTURE_SIZE),
                    ModelFactory.MODEL_TEXTURE_SIZE*3*256,
                    ModelFactory.MODEL_TEXTURE_SIZE*2*256)
                    .name("ModelTextures");
        } catch (RuntimeException | Error failure) {
            try {
                atlas.free();
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public static synchronized void giveBackModelStoreTextureAtlas(GlTexture texture) {
        if (texture == null) throw new NullPointerException("texture");
        long generation = requireCurrentGeneration();
        synchronizeCacheGeneration(generation);
        texture.assertContextCurrent();
        texture.assertAllocated();
        MODEL_TEXTURE_CACHE.add(texture);
    }

    static synchronized GlBuffer getOrCreateGeometryBuffer() {
        long generation = requireCurrentGeneration();
        synchronizeCacheGeneration(generation);
        if (!GEOMETRY_BUFFER_CACHE.isEmpty()) {
            GlBuffer cached = GEOMETRY_BUFFER_CACHE.remove(0);
            cached.assertContextCurrent();
            return cached;
        }

        Capabilities capabilities = Capabilities.require();
        long capacity = getGeometryBufferSize(capabilities);
        long driverMemory = -1;
        if (capabilities.canQueryGpuMemory) {
            driverMemory = capabilities.getFreeDedicatedGpuMemory();
        }

        GlBuffer buffer = null;
        glGetError();//Clear any errors
        if (!(capabilities.isNvidia && ThreadUtils.isWindows && capabilities.sparseBuffer)) {//This hack makes it so it doesnt crash on renderdoc
            buffer = new GlBuffer(capacity, false);//Only do this if we are not on nvidia
            //TODO: FIXME: TEST, see if the issue is that we are trying to zero the entire buffer, try only zeroing increments
            // or dont zero it at all
        } else {
            Logger.info("Running on nvidia, using workaround sparse buffer allocation");
        }
        int error = glGetError();
        if (error != GL_NO_ERROR || buffer == null) {
            if ((buffer == null || error == GL_OUT_OF_MEMORY) && capabilities.sparseBuffer) {
                if (buffer != null) {
                    Logger.error("Failed to allocate geometry buffer, attempting workaround with sparse buffers");
                    buffer.free();
                }
                buffer = new GlBuffer(capacity, GL_SPARSE_STORAGE_BIT_ARB);
                //buffer.zero();
                error = glGetError();
                if (error != GL_NO_ERROR) {
                    buffer.free();
                    throw new IllegalStateException("Unable to allocate geometry buffer using workaround, got gl error " + error + ". Failed to allocate buffer of size " + capacity);
                }
            } else {
                throw new IllegalStateException("Unable to allocate geometry buffer, got gl error " + error + ". Failed to allocate buffer of size " + capacity);
            }
        }
        String extra = "";
        if (driverMemory != -1) {
            extra = ", driver stated " + (driverMemory / (1024 * 1024)) + "Mb of free memory";
        }
        Logger.info("Allocated new geometry buffer: " + buffer.size() + ", isSparse: " + buffer.isSparse() + extra);
        return buffer;
    }

    static synchronized void giveBackGeometryBuffer(GlBuffer geometryBuffer) {
        if (geometryBuffer == null) throw new NullPointerException("geometryBuffer");
        long generation = requireCurrentGeneration();
        synchronizeCacheGeneration(generation);
        geometryBuffer.assertContextCurrent();
        GEOMETRY_BUFFER_CACHE.add(geometryBuffer);
    }

    private static long getGeometryBufferSize(Capabilities capabilities) {
        long geometryCapacity = Math.min((1L << (64 - Long.numberOfLeadingZeros(capabilities.ssboMaxSize - 1))) << 1, 1L << 32) - 1024;
        if (capabilities.isIntel) {
            geometryCapacity = Math.max(geometryCapacity, 1L << 30);//intel moment, force min 1gb
        }
        if (capabilities.isNvidia && ThreadUtils.isLinux) {
            geometryCapacity = Math.min(geometryCapacity, 2000L * 1024L * 1024L);//nvidia linux moment, force max 2gb heap
        }

        geometryCapacity = Math.max(512 * 1024 * 1024, geometryCapacity);//min of 512 mb

        //Limit to available dedicated memory if possible
        if (capabilities.canQueryGpuMemory) {
            //512mb less than available,
            long limit = capabilities.getFreeDedicatedGpuMemory() - (long) (1.5 * 1024 * 1024 * 1024);//1.5gb vram buffer
            // Give a minimum of 512 mb requirement
            limit = Math.max(512 * 1024 * 1024, limit);
            geometryCapacity = Math.min(geometryCapacity, limit);
        }

        var override = System.getProperty("voxy.geometryBufferSizeOverrideMB", "");
        if (!override.isEmpty()) {
            geometryCapacity = Long.parseLong(override) * 1024L * 1024L;
        }
        return geometryCapacity;
    }
}

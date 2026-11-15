package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

import static org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_READ_BIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_UNSYNCHRONIZED_BIT;
import static org.lwjgl.opengl.GL30C.GL_MAP_FLUSH_EXPLICIT_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_COHERENT_BIT;
import static org.lwjgl.opengl.GL44C.GL_CLIENT_STORAGE_BIT;

/**
 * Desktop persistent-mapped buffer wrapper.
 *
 * This class is only valid when the selected backend advertises persistent
 * mapping support. Backends without that feature must use ordinary GlBuffer
 * storage and an explicit staging/readback path instead.
 */
public class GlPersistentMappedBuffer extends TrackedObject {
    public final int id;
    private final long size;
    private final long addr;

    public GlPersistentMappedBuffer(long size, int flags) {
        if (size <= 0) throw new IllegalArgumentException("Persistent buffer size must be positive");
        var backend = GlBackendRuntime.requireBackend();
        var capabilities = backend.capabilities();
        if (!capabilities.bufferStorage || !capabilities.persistentMapping) {
            throw new UnsupportedOperationException("Selected backend does not support persistent mapped buffers");
        }

        int storageFlags = GL_MAP_PERSISTENT_BIT
                | (flags & (GL_MAP_COHERENT_BIT | GL_MAP_WRITE_BIT | GL_MAP_READ_BIT | GL_CLIENT_STORAGE_BIT));
        this.id = backend.createBuffer(size, 0L, storageFlags);
        this.size = size;

        long mappedAddress;
        try {
            int mapFlags = (flags & (GL_MAP_WRITE_BIT | GL_MAP_READ_BIT
                    | GL_MAP_UNSYNCHRONIZED_BIT | GL_MAP_FLUSH_EXPLICIT_BIT))
                    | GL_MAP_PERSISTENT_BIT;
            mappedAddress = backend.mapBufferRange(this.id, 0, size, mapFlags);
            if (mappedAddress == 0L) throw new IllegalStateException("Failed to map persistent buffer");
        } catch (RuntimeException | Error failure) {
            try {
                backend.deleteBuffer(this.id);
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        this.addr = mappedAddress;
    }

    @Override
    public void free() {
        this.free0();
        var backend = GlBackendRuntime.requireBackend();
        Throwable failure = null;
        try {
            if (!backend.unmapBuffer(this.id)) {
                failure = new IllegalStateException("Persistent buffer contents became invalid while unmapping");
            }
        } catch (RuntimeException | Error unmapFailure) {
            failure = unmapFailure;
        } finally {
            try {
                backend.deleteBuffer(this.id);
            } catch (RuntimeException | Error deleteFailure) {
                if (failure == null) failure = deleteFailure;
                else if (failure != deleteFailure) failure.addSuppressed(deleteFailure);
            }
        }
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
    }

    public long size() {
        return this.size;
    }

    public long addr() {
        return this.addr;
    }

    public GlPersistentMappedBuffer name(String name) {
        return GlDebug.name(name, this);
    }
}

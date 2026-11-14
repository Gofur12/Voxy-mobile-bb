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
public class GlPersistentMappedBuffer extends TrackedObject {
    public final int id;
    private final long size;
    private final long addr;
    public GlPersistentMappedBuffer(long size, int flags) {
        this.id = GlBackendRuntime.requireBackend().createBuffer(size, 0L, GL_MAP_PERSISTENT_BIT|(flags&(GL_MAP_COHERENT_BIT|GL_MAP_WRITE_BIT|GL_MAP_READ_BIT|GL_CLIENT_STORAGE_BIT)));
        this.size = size;
        long mappedAddress;
        try {
            mappedAddress = GlBackendRuntime.requireBackend().mapBufferRange(this.id, 0, size,
                    (flags&(GL_MAP_WRITE_BIT|GL_MAP_READ_BIT|GL_MAP_UNSYNCHRONIZED_BIT|GL_MAP_FLUSH_EXPLICIT_BIT))|GL_MAP_PERSISTENT_BIT);
            if (mappedAddress == 0L) throw new IllegalStateException("Failed to map persistent buffer");
        } catch (RuntimeException | Error failure) {
            GlBackendRuntime.requireBackend().deleteBuffer(this.id);
            throw failure;
        }
        this.addr = mappedAddress;
    }

    @Override
    public void free() {
        this.free0();
        GlBackendRuntime.requireBackend().unmapBuffer(this.id);
        GlBackendRuntime.requireBackend().deleteBuffer(this.id);
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

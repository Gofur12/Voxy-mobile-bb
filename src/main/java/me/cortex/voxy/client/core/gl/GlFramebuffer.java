package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;

public class GlFramebuffer extends TrackedObject {
    public final int id;
    private final long contextGeneration;
    public GlFramebuffer() {
        var backend = GlBackendRuntime.requireBackend();
        this.id = backend.createFramebuffer();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
    }

    public GlFramebuffer bind(int attachment, GlTexture texture) {
        return this.bind(attachment, texture, 0);
    }

    public GlFramebuffer bind(int attachment, int texture) {
        return this.bind(attachment, texture, 0);
    }

    public GlFramebuffer bind(int attachment, int texture, int lvl) {
        GlBackendRuntime.requireBackend(this.contextGeneration, "GlFramebuffer").attachTexture2D(this.id, attachment, texture, lvl);
        return this;
    }

    public GlFramebuffer bind(int attachment, GlTexture texture, int lvl) {
        texture.assertContextCurrent();
        GlBackendRuntime.requireBackend(this.contextGeneration, "GlFramebuffer").attachTexture2D(this.id, attachment, texture.id, lvl);
        return this;
    }

    public GlFramebuffer bind(int attachment, GlRenderBuffer buffer) {
        buffer.assertContextCurrent();
        GlBackendRuntime.requireBackend(this.contextGeneration, "GlFramebuffer").attachRenderbuffer(this.id, attachment, buffer.id);
        return this;
    }

    public GlFramebuffer setDrawBuffers(int... buffers) {
        GlBackendRuntime.requireBackend(this.contextGeneration, "GlFramebuffer").setFramebufferDrawBuffers(this.id, buffers);
        return this;
    }

    @Override
    public void free() {
        super.free0();
        if (GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration)) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "GlFramebuffer").deleteFramebuffer(this.id);
        }
    }

    public GlFramebuffer verify() {
        int code = GlBackendRuntime.requireBackend(this.contextGeneration, "GlFramebuffer").getFramebufferStatus(this.id);
        if (code != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete with error code: " + code);
        }
        return this;
    }


    public GlFramebuffer name(String name) {
        return GlDebug.name(name, this);
    }
}

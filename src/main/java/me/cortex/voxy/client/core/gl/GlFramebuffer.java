package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

public class GlFramebuffer extends TrackedObject {
    public final int id;
    public GlFramebuffer() {
        this.id = GlBackendRuntime.requireBackend().createFramebuffer();
    }

    public GlFramebuffer bind(int attachment, GlTexture texture) {
        return this.bind(attachment, texture, 0);
    }

    public GlFramebuffer bind(int attachment, int texture) {
        return this.bind(attachment, texture, 0);
    }

    public GlFramebuffer bind(int attachment, int texture, int lvl) {
        GlBackendRuntime.requireBackend().attachTexture2D(this.id, attachment, texture, lvl);
        return this;
    }

    public GlFramebuffer bind(int attachment, GlTexture texture, int lvl) {
        glNamedFramebufferTexture(this.id, attachment, texture.id, lvl);
        return this;
    }

    public GlFramebuffer bind(int attachment, GlRenderBuffer buffer) {
        GlBackendRuntime.requireBackend().attachRenderbuffer(this.id, attachment, buffer.id);
        return this;
    }

    public GlFramebuffer setDrawBuffers(int... buffers) {
        GlBackendRuntime.requireBackend().setFramebufferDrawBuffers(this.id, buffers);
        return this;
    }

    @Override
    public void free() {
        super.free0();
        GlBackendRuntime.requireBackend().deleteFramebuffer(this.id);
    }

    public GlFramebuffer verify() {
        int code = GlBackendRuntime.requireBackend().getFramebufferStatus(this.id);
        if (code != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete with error code: " + code);
        }
        return this;
    }


    public GlFramebuffer name(String name) {
        return GlDebug.name(name, this);
    }
}

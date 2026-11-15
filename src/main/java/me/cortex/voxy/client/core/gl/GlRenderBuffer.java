package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

public class GlRenderBuffer extends TrackedObject {
    public final int id;

    public GlRenderBuffer(int format, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Renderbuffer dimensions must be positive");
        }
        GlBackend backend = GlBackendRuntime.requireBackend();
        this.id = backend.createRenderbuffer();
        try {
            backend.allocateRenderbuffer(this.id, format, width, height);
        } catch (RuntimeException | Error allocationFailure) {
            try {
                backend.deleteRenderbuffer(this.id);
            } catch (RuntimeException | Error cleanupFailure) {
                if (allocationFailure != cleanupFailure) allocationFailure.addSuppressed(cleanupFailure);
            }
            throw allocationFailure;
        }
    }

    @Override
    public void free() {
        super.free0();
        GlBackendRuntime.requireBackend().deleteRenderbuffer(this.id);
    }
}

package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

public class GlRenderBuffer extends TrackedObject {
    public final int id;
    private final long contextGeneration;

    public GlRenderBuffer(int format, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Renderbuffer dimensions must be positive");
        }
        GlBackend backend = GlBackendRuntime.requireBackend();
        this.id = backend.createRenderbuffer();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
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
        if (GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration)) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "GlRenderBuffer").deleteRenderbuffer(this.id);
        }
    }
}

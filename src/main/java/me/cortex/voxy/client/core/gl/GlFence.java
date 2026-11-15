package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

public class GlFence extends TrackedObject {
    private final long fence;
    private final long contextGeneration;
    private boolean signaled;

    public GlFence() {
        this.fence = GlBackendRuntime.requireBackend().createFence();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
    }

    public boolean signaled() {
        var backend = GlBackendRuntime.requireBackend(this.contextGeneration, "GlFence");
        if (!this.signaled) {
            this.signaled = backend.isFenceSignaled(this.fence);
        }
        return this.signaled;
    }

    @Override
    public void free() {
        super.free0();
        if (GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration)) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "GlFence").deleteFence(this.fence);
        }
    }
}

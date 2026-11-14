package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

public class GlFence extends TrackedObject {
    private final long fence;
    private boolean signaled;

    public GlFence() {
        this.fence = GlBackendRuntime.requireBackend().createFence();
    }

    public boolean signaled() {
        if (!this.signaled) {
            this.signaled = GlBackendRuntime.requireBackend().isFenceSignaled(this.fence);
        }
        return this.signaled;
    }

    @Override
    public void free() {
        super.free0();
        GlBackendRuntime.requireBackend().deleteFence(this.fence);
    }
}

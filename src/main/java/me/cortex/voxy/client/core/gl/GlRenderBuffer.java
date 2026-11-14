package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

public class GlRenderBuffer extends TrackedObject {
    public final int id;

    public GlRenderBuffer(int format, int width, int height) {
        this.id = GlBackendRuntime.requireBackend().createRenderbuffer();
        GlBackendRuntime.requireBackend().allocateRenderbuffer(this.id, format, width, height);
    }

    @Override
    public void free() {
        super.free0();
        GlBackendRuntime.requireBackend().deleteRenderbuffer(this.id);
    }
}

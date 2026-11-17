package me.cortex.voxy.client.core.gl.backend.desktop;

import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendProvider;
import org.lwjgl.opengl.GL;

/** Provider for an already-current desktop OpenGL 4.5 context. */
public final class DesktopGlBackendProvider implements GlBackendProvider {
    @Override
    public String id() {
        return "desktop-opengl-45";
    }

    @Override
    public GlBackend createIfSupported() {
        try {
            if (!GL.getCapabilities().OpenGL45) {
                return null;
            }
            return new DesktopGlBackend();
        } catch (IllegalStateException noCurrentContext) {
            return null;
        }
    }
}

package me.cortex.voxy.client.core.gl.backend;

/**
 * Capability-based renderer tier selection.
 *
 * This class only selects a compatible implementation tier. It does not
 * create a renderer or claim that an Android/GLES implementation exists.
 */
public enum GlRendererPath {
    DESKTOP_MDIC,
    GLES_COMPUTE_LOD_CANDIDATE,
    GLES_BASIC_RASTER_CANDIDATE,
    UNSUPPORTED;

    /** True only when this repository contains a usable renderer implementation. */
    public boolean isImplemented() {
        return this == DESKTOP_MDIC;
    }

    /** Selects a tier only when its renderer implementation is present. */
    public static GlRendererPath selectImplemented(GlCapabilities capabilities) {
        GlRendererPath candidate = select(capabilities);
        return candidate.isImplemented() ? candidate : UNSUPPORTED;
    }

    public static GlRendererPath select(GlCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("GL capabilities must not be null");
        }
        if (capabilities.supportsDesktopMdicPath()) {
            return DESKTOP_MDIC;
        }
        if (capabilities.supportsGlesComputeLodPath()) {
            return GLES_COMPUTE_LOD_CANDIDATE;
        }
        if (capabilities.supportsGles3BasicRasterPath()) {
            return GLES_BASIC_RASTER_CANDIDATE;
        }
        return UNSUPPORTED;
    }
}

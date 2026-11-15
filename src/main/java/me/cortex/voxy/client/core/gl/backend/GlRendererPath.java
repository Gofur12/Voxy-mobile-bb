package me.cortex.voxy.client.core.gl.backend;

/**
 * Capability-based renderer tier selection.
 *
 * This class only selects a compatible implementation tier. It does not
 * create a renderer or claim that an Android/GLES implementation exists.
 */
public enum GlRendererPath {
    DESKTOP_MDIC,
    GLES_COMPUTE_LOD,
    GLES_BASIC_RASTER,
    UNSUPPORTED;

    public static GlRendererPath select(GlCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("GL capabilities must not be null");
        }
        if (capabilities.supportsDesktopMdicPath()) {
            return DESKTOP_MDIC;
        }
        if (capabilities.supportsGlesComputeLodPath()) {
            return GLES_COMPUTE_LOD;
        }
        if (capabilities.supportsGles3BasicRasterPath()) {
            return GLES_BASIC_RASTER;
        }
        return UNSUPPORTED;
    }
}

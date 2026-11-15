package me.cortex.voxy.client.core.gl.backend;

/**
 * Semantic zero-clear categories for supported texture storage formats.
 *
 * This enum deliberately avoids API-specific GL constants. Each backend maps
 * these categories to the transfer representation supported by its graphics API.
 */
public enum GlTextureClearType {
    RED_FLOAT,
    RG_FLOAT,
    RGBA_FLOAT,
    RED_UNSIGNED_INT,
    DEPTH_FLOAT,
    DEPTH_STENCIL_UNSIGNED_INT_24_8,
    DEPTH_STENCIL_FLOAT32_UNSIGNED_INT_24_8
}

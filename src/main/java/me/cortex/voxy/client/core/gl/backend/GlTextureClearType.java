package me.cortex.voxy.client.core.gl.backend;

/**
 * Semantic zero-clear categories for supported texture storage formats.
 *
 * This enum deliberately avoids API-specific GL constants. Each backend maps
 * these categories to the transfer representation supported by its graphics API.
 */
public enum GlTextureClearType {
    RED_FLOAT(Attachment.COLOR, ValueType.FLOAT),
    RG_FLOAT(Attachment.COLOR, ValueType.FLOAT),
    RGBA_FLOAT(Attachment.COLOR, ValueType.FLOAT),
    RED_UNSIGNED_INT(Attachment.COLOR, ValueType.UNSIGNED_INTEGER),
    DEPTH_FLOAT(Attachment.DEPTH, ValueType.NONE),
    DEPTH_STENCIL_UNSIGNED_INT_24_8(Attachment.DEPTH_STENCIL, ValueType.NONE),
    DEPTH_STENCIL_FLOAT32_UNSIGNED_INT_24_8(Attachment.DEPTH_STENCIL, ValueType.NONE);

    /** The framebuffer attachment class required to clear this texture. */
    public enum Attachment {
        COLOR,
        DEPTH,
        DEPTH_STENCIL
    }

    /**
     * Scalar category for color clears. Depth and depth-stencil clears use
     * dedicated depth clear operations and therefore report NONE.
     */
    public enum ValueType {
        FLOAT,
        UNSIGNED_INTEGER,
        NONE
    }

    private final Attachment attachment;
    private final ValueType valueType;

    GlTextureClearType(Attachment attachment, ValueType valueType) {
        this.attachment = attachment;
        this.valueType = valueType;
    }

    public Attachment attachment() {
        return this.attachment;
    }

    public ValueType valueType() {
        return this.valueType;
    }
}

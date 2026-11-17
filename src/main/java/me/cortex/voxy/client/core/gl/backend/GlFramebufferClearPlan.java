package me.cortex.voxy.client.core.gl.backend;

/**
 * API-neutral decision plan for clearing one texture through a temporary
 * framebuffer when direct texture-clear commands are unavailable.
 *
 * This class only selects the semantic clear operation. A concrete backend
 * remains responsible for attachment setup, framebuffer completeness checks,
 * state preservation/restoration, and issuing the API-specific clear call.
 */
public final class GlFramebufferClearPlan {
    public enum Operation {
        COLOR_FLOAT,
        COLOR_UNSIGNED_INTEGER,
        DEPTH_FLOAT,
        DEPTH_STENCIL_FLOAT_UNSIGNED_INTEGER
    }

    private final GlTextureClearType.Attachment attachment;
    private final Operation operation;

    private GlFramebufferClearPlan(GlTextureClearType.Attachment attachment, Operation operation) {
        this.attachment = attachment;
        this.operation = operation;
    }

    public static GlFramebufferClearPlan forType(GlTextureClearType clearType) {
        if (clearType == null) {
            throw new IllegalArgumentException("Texture clear type must not be null");
        }

        return switch (clearType) {
            case RED_FLOAT, RG_FLOAT, RGBA_FLOAT ->
                    new GlFramebufferClearPlan(GlTextureClearType.Attachment.COLOR, Operation.COLOR_FLOAT);
            case RED_UNSIGNED_INT ->
                    new GlFramebufferClearPlan(GlTextureClearType.Attachment.COLOR, Operation.COLOR_UNSIGNED_INTEGER);
            case DEPTH_FLOAT ->
                    new GlFramebufferClearPlan(GlTextureClearType.Attachment.DEPTH, Operation.DEPTH_FLOAT);
            case DEPTH_STENCIL_UNSIGNED_INT_24_8, DEPTH_STENCIL_FLOAT32_UNSIGNED_INT_24_8 ->
                    new GlFramebufferClearPlan(GlTextureClearType.Attachment.DEPTH_STENCIL,
                            Operation.DEPTH_STENCIL_FLOAT_UNSIGNED_INTEGER);
        };
    }

    public GlTextureClearType.Attachment attachment() {
        return this.attachment;
    }

    public Operation operation() {
        return this.operation;
    }
}

package me.cortex.voxy.client.core.gl.backend;

/**
 * Immutable snapshot of caller-visible state that a framebuffer clear fallback
 * may need to change and therefore must restore.
 *
 * Values are API-neutral. A backend is responsible for translating its native
 * state into this snapshot and restoring it in a finally block.
 */
public final class GlFramebufferClearState {
    private final int drawFramebuffer;
    private final int readFramebuffer;
    private final int drawBuffer;
    private final boolean scissorEnabled;
    private final int scissorX;
    private final int scissorY;
    private final int scissorWidth;
    private final int scissorHeight;
    private final boolean colorMaskRed;
    private final boolean colorMaskGreen;
    private final boolean colorMaskBlue;
    private final boolean colorMaskAlpha;
    private final boolean depthMask;
    private final int stencilFrontWriteMask;
    private final int stencilBackWriteMask;

    public GlFramebufferClearState(
            int drawFramebuffer,
            int readFramebuffer,
            int drawBuffer,
            boolean scissorEnabled,
            int scissorX,
            int scissorY,
            int scissorWidth,
            int scissorHeight,
            boolean colorMaskRed,
            boolean colorMaskGreen,
            boolean colorMaskBlue,
            boolean colorMaskAlpha,
            boolean depthMask,
            int stencilFrontWriteMask,
            int stencilBackWriteMask) {
        if (scissorWidth < 0 || scissorHeight < 0) {
            throw new IllegalArgumentException("Scissor dimensions must not be negative");
        }
        this.drawFramebuffer = drawFramebuffer;
        this.readFramebuffer = readFramebuffer;
        this.drawBuffer = drawBuffer;
        this.scissorEnabled = scissorEnabled;
        this.scissorX = scissorX;
        this.scissorY = scissorY;
        this.scissorWidth = scissorWidth;
        this.scissorHeight = scissorHeight;
        this.colorMaskRed = colorMaskRed;
        this.colorMaskGreen = colorMaskGreen;
        this.colorMaskBlue = colorMaskBlue;
        this.colorMaskAlpha = colorMaskAlpha;
        this.depthMask = depthMask;
        this.stencilFrontWriteMask = stencilFrontWriteMask;
        this.stencilBackWriteMask = stencilBackWriteMask;
    }

    public int drawFramebuffer() { return this.drawFramebuffer; }
    public int readFramebuffer() { return this.readFramebuffer; }
    public int drawBuffer() { return this.drawBuffer; }
    public boolean scissorEnabled() { return this.scissorEnabled; }
    public int scissorX() { return this.scissorX; }
    public int scissorY() { return this.scissorY; }
    public int scissorWidth() { return this.scissorWidth; }
    public int scissorHeight() { return this.scissorHeight; }
    public boolean colorMaskRed() { return this.colorMaskRed; }
    public boolean colorMaskGreen() { return this.colorMaskGreen; }
    public boolean colorMaskBlue() { return this.colorMaskBlue; }
    public boolean colorMaskAlpha() { return this.colorMaskAlpha; }
    public boolean depthMask() { return this.depthMask; }
    public int stencilFrontWriteMask() { return this.stencilFrontWriteMask; }
    public int stencilBackWriteMask() { return this.stencilBackWriteMask; }
}

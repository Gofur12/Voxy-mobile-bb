package me.cortex.voxy.client.core.gl.backend;

/** Backend-neutral buffer usage hints. These are not raw GL enum values. */
public final class GlBufferUsage {
    private GlBufferUsage() {}

    public static final int STATIC = 1;
    public static final int DYNAMIC = 2;
    public static final int STREAM = 3;
}

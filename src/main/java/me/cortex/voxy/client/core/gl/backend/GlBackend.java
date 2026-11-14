package me.cortex.voxy.client.core.gl.backend;

/**
 * Backend-neutral entry point for Voxy's GPU operations.
 *
 * Implementations own the graphics context and all API-specific calls.
 * This interface intentionally contains no LWJGL or platform classes so
 * Android and desktop implementations can be loaded independently.
 *
 * Resource handles are backend-owned integers and are valid only for the
 * lifetime of the creating backend/context.
 */
public interface GlBackend extends AutoCloseable {
    String name();

    GlCapabilities capabilities();

    int createBuffer(long size, int usageFlags);

    void deleteBuffer(int handle);

    void uploadBuffer(int handle, long offset, long size, long sourceAddress);

    void bindBuffer(int target, int handle);

    int createTexture2D();

    void allocateTexture2D(int handle, int levels, int internalFormat, int width, int height);

    void deleteTexture(int handle);

    int createFramebuffer();

    void attachTexture2D(int framebuffer, int attachment, int texture, int level);

    boolean isFramebufferComplete(int framebuffer);

    void deleteFramebuffer(int framebuffer);

    int createVertexArray();

    void bindVertexArray(int handle);

    void deleteVertexArray(int handle);

    void memoryBarrier(int barrierFlags);

    void finish();

    @Override
    void close();
}

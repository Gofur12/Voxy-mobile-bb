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

    int createBuffer(long size, long initialDataAddress, int storageFlags);

    void clearBuffer(int handle, int internalFormat, int format, int type, long dataAddress);

    void clearBufferRange(int handle, int internalFormat, long offset, long size, int format, int type, long dataAddress);

    void pixelStore(int parameter, int value);

    void deleteBuffer(int handle);

    void uploadBuffer(int handle, long offset, long size, long sourceAddress);

    void bindBuffer(int target, int handle);

    int createTexture(int target);

    int createTextureView(int source, int target, int internalFormat,
                          int minLevel, int levelCount, int minLayer, int layerCount);

    void clearTextureImage(int handle, int level, int format, int type, long dataAddress);

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

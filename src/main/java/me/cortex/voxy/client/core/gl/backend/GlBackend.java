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

    int createShader(int shaderType);

    void setShaderSource(int shader, String source);

    void compileShader(int shader);

    boolean isShaderCompiled(int shader);

    String getShaderInfoLog(int shader);

    void deleteShader(int shader);

    int createProgram();

    void attachShader(int program, int shader);

    void detachShader(int program, int shader);

    void linkProgram(int program);

    boolean isProgramLinked(int program);

    String getProgramInfoLog(int program);

    void deleteProgram(int program);

    void useProgram(int program);

    int createBuffer(long size, long initialDataAddress, int storageFlags);

    void clearBuffer(int handle, int internalFormat, int format, int type, long dataAddress);

    void clearBufferRange(int handle, int internalFormat, long offset, long size, int format, int type, long dataAddress);

    void pixelStore(int parameter, int value);

    void deleteBuffer(int handle);

    void uploadBuffer(int handle, long offset, long size, long sourceAddress);

    long mapBufferRange(int handle, long offset, long length, int accessFlags);

    boolean unmapBuffer(int handle);

    long createFence();

    boolean isFenceSignaled(long fence);

    void deleteFence(long fence);

    void bindBuffer(int target, int handle);

    void bindBufferBase(int target, int index, int buffer);

    void bindBufferRange(int target, int index, int buffer, long offset, long size);

    void bindTextureUnit(int unit, int texture);

    void bindSampler(int unit, int sampler);

    int createTexture(int target);

    int createTextureView(int source, int target, int internalFormat,
                          int minLevel, int levelCount, int minLayer, int layerCount);

    void clearTextureImage(int handle, int level, int format, int type, long dataAddress);

    void allocateTexture2D(int handle, int levels, int internalFormat, int width, int height);

    void deleteTexture(int handle);

    int createFramebuffer();

    void attachTexture2D(int framebuffer, int attachment, int texture, int level);

    void attachRenderbuffer(int framebuffer, int attachment, int renderbuffer);

    void setFramebufferDrawBuffers(int framebuffer, int[] buffers);

    int getFramebufferStatus(int framebuffer);

    void deleteFramebuffer(int framebuffer);

    int createRenderbuffer();

    void allocateRenderbuffer(int renderbuffer, int format, int width, int height);

    void deleteRenderbuffer(int renderbuffer);

    int createVertexArray();

    void bindVertexArray(int handle);

    void bindVertexArrayVertexBuffer(int vertexArray, int bindingIndex, int buffer, long offset, int stride);

    void bindVertexArrayElementBuffer(int vertexArray, int buffer);

    void enableVertexArrayAttribute(int vertexArray, int attributeIndex);

    void setVertexArrayIntegerAttributeFormat(int vertexArray, int attributeIndex, int componentCount, int type, int relativeOffset);

    void setVertexArrayFloatAttributeFormat(int vertexArray, int attributeIndex, int componentCount, int type, boolean normalized, int relativeOffset);

    void deleteVertexArray(int handle);

    void memoryBarrier(int barrierFlags);

    void finish();

    @Override
    void close();
}

package me.cortex.voxy.client.core.gl.backend.desktop;

import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL45C;

/**
 * Desktop OpenGL 4.5 implementation of the backend contract.
 *
 * The Minecraft/loader owns the context. This adapter must be created and
 * used only on a thread where that context is current.
 */
public final class DesktopGlBackend implements GlBackend {
    private final GlCapabilities capabilities;

    public DesktopGlBackend() {
        org.lwjgl.opengl.GLCapabilities gl = GL.getCapabilities();
        if (!gl.OpenGL45) {
            throw new IllegalStateException("Desktop Voxy backend requires OpenGL 4.5");
        }

        int major = GL11C.glGetInteger(GL30C.GL_MAJOR_VERSION);
        int minor = GL11C.glGetInteger(GL30C.GL_MINOR_VERSION);
        this.capabilities = new GlCapabilities(
                major,
                minor,
                false,
                gl.GL_ARB_buffer_storage,
                gl.GL_ARB_buffer_storage,
                gl.GL_ARB_compute_shader,
                gl.GL_ARB_shader_storage_buffer_object,
                gl.GL_ARB_shader_image_load_store,
                gl.GL_ARB_multi_draw_indirect,
                gl.GL_ARB_indirect_parameters,
                gl.GL_ARB_texture_view,
                gl.GL_KHR_shader_subgroup,
                gl.GL_ARB_gpu_shader_int64,
                gl.OpenGL32
        );
    }

    @Override
    public String name() {
        return "desktop-opengl-45";
    }

    @Override
    public GlCapabilities capabilities() {
        return this.capabilities;
    }

    @Override
    public int createShader(int shaderType) {
        return GL20C.glCreateShader(shaderType);
    }

    @Override
    public void setShaderSource(int shader, String source) {
        GL20C.glShaderSource(shader, source);
    }

    @Override
    public void compileShader(int shader) {
        GL20C.glCompileShader(shader);
    }

    @Override
    public boolean isShaderCompiled(int shader) {
        return GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == GL20C.GL_TRUE;
    }

    @Override
    public String getShaderInfoLog(int shader) {
        return GL20C.glGetShaderInfoLog(shader);
    }

    @Override
    public void deleteShader(int shader) {
        GL20C.glDeleteShader(shader);
    }

    @Override
    public int createProgram() {
        return GL20C.glCreateProgram();
    }

    @Override
    public void attachShader(int program, int shader) {
        GL20C.glAttachShader(program, shader);
    }

    @Override
    public void detachShader(int program, int shader) {
        GL20C.glDetachShader(program, shader);
    }

    @Override
    public void linkProgram(int program) {
        GL20C.glLinkProgram(program);
    }

    @Override
    public boolean isProgramLinked(int program) {
        return GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == GL20C.GL_TRUE;
    }

    @Override
    public String getProgramInfoLog(int program) {
        return GL20C.glGetProgramInfoLog(program);
    }

    @Override
    public void deleteProgram(int program) {
        GL20C.glDeleteProgram(program);
    }

    @Override
    public void useProgram(int program) {
        GL20C.glUseProgram(program);
    }

    @Override
    public int createBuffer(long size, long initialDataAddress, int storageFlags) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
        int buffer = GL45C.glCreateBuffers();
        try {
            GL45C.nglNamedBufferStorage(buffer, size, initialDataAddress, storageFlags);
            return buffer;
        } catch (RuntimeException | Error failure) {
            GL15C.glDeleteBuffers(buffer);
            throw failure;
        }
    }

    @Override
    public void clearBuffer(int handle, int internalFormat, int format, int type, long dataAddress) {
        GL45C.nglClearNamedBufferData(handle, internalFormat, format, type, dataAddress);
    }

    @Override
    public void clearBufferRange(int handle, int internalFormat, long offset, long size, int format, int type, long dataAddress) {
        GL45C.nglClearNamedBufferSubData(handle, internalFormat, offset, size, format, type, dataAddress);
    }

    @Override
    public void pixelStore(int parameter, int value) {
        GL11C.glPixelStorei(parameter, value);
    }

    @Override
    public void deleteBuffer(int handle) {
        GL15C.glDeleteBuffers(handle);
    }

    @Override
    public void uploadBuffer(int handle, long offset, long size, long sourceAddress) {
        if (offset < 0 || size < 0 || sourceAddress == 0L) {
            throw new IllegalArgumentException("Invalid buffer upload range or source address");
        }
        GL45C.nglNamedBufferSubData(handle, offset, size, sourceAddress);
    }

    @Override
    public long mapBufferRange(int handle, long offset, long length, int accessFlags) {
        if (offset < 0 || length <= 0) throw new IllegalArgumentException("Invalid mapped buffer range");
        return GL45C.nglMapNamedBufferRange(handle, offset, length, accessFlags);
    }

    @Override
    public boolean unmapBuffer(int handle) {
        return GL45C.glUnmapNamedBuffer(handle);
    }

    @Override
    public long createFence() {
        long fence = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        if (fence == 0L) throw new IllegalStateException("Failed to create GPU fence");
        return fence;
    }

    @Override
    public boolean isFenceSignaled(long fence) {
        int result = GL32C.glClientWaitSync(fence, 0, 0L);
        if (result == GL32C.GL_ALREADY_SIGNALED || result == GL32C.GL_CONDITION_SATISFIED) return true;
        if (result == GL32C.GL_TIMEOUT_EXPIRED) return false;
        throw new IllegalStateException("GPU fence wait failed with status: " + result);
    }

    @Override
    public void deleteFence(long fence) {
        GL32C.glDeleteSync(fence);
    }

    @Override
    public void bindBuffer(int target, int handle) {
        GL15C.glBindBuffer(target, handle);
    }

    @Override
    public void bindBufferBase(int target, int index, int buffer) {
        GL30C.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void bindBufferRange(int target, int index, int buffer, long offset, long size) {
        if (offset < 0 || size <= 0) throw new IllegalArgumentException("Invalid indexed buffer range");
        GL30C.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void bindTextureUnit(int unit, int texture) {
        GL45C.glBindTextureUnit(unit, texture);
    }

    @Override
    public void bindSampler(int unit, int sampler) {
        GL33C.glBindSampler(unit, sampler);
    }

    @Override
    public int createTexture(int target) {
        return GL45C.glCreateTextures(target);
    }

    @Override
    public int createTextureView(int source, int target, int internalFormat,
                                 int minLevel, int levelCount, int minLayer, int layerCount) {
        int view = GL11C.glGenTextures();
        try {
            GL45C.glTextureView(view, target, source, internalFormat,
                    minLevel, levelCount, minLayer, layerCount);
            return view;
        } catch (RuntimeException | Error failure) {
            GL11C.glDeleteTextures(view);
            throw failure;
        }
    }

    @Override
    public void clearTextureImage(int handle, int level, int format, int type, long dataAddress) {
        GL45C.nglClearTexImage(handle, level, format, type, dataAddress);
    }

    @Override
    public void allocateTexture2D(int handle, int levels, int internalFormat, int width, int height) {
        if (levels <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions and mip levels must be positive");
        }
        GL45C.glTextureStorage2D(handle, levels, internalFormat, width, height);
    }

    @Override
    public void deleteTexture(int handle) {
        GL11C.glDeleteTextures(handle);
    }

    @Override
    public int createFramebuffer() {
        return GL45C.glCreateFramebuffers();
    }

    @Override
    public void attachTexture2D(int framebuffer, int attachment, int texture, int level) {
        GL45C.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
    }

    @Override
    public void attachRenderbuffer(int framebuffer, int attachment, int renderbuffer) {
        GL45C.glNamedFramebufferRenderbuffer(framebuffer, attachment, GL30C.GL_RENDERBUFFER, renderbuffer);
    }

    @Override
    public void setFramebufferDrawBuffers(int framebuffer, int[] buffers) {
        GL45C.glNamedFramebufferDrawBuffers(framebuffer, buffers);
    }

    @Override
    public int getFramebufferStatus(int framebuffer) {
        return GL45C.glCheckNamedFramebufferStatus(framebuffer, GL30C.GL_FRAMEBUFFER);
    }

    @Override
    public void deleteFramebuffer(int handle) {
        GL30C.glDeleteFramebuffers(handle);
    }

    @Override
    public int createRenderbuffer() {
        return GL45C.glCreateRenderbuffers();
    }

    @Override
    public void allocateRenderbuffer(int renderbuffer, int format, int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Renderbuffer dimensions must be positive");
        GL45C.glNamedRenderbufferStorage(renderbuffer, format, width, height);
    }

    @Override
    public void deleteRenderbuffer(int renderbuffer) {
        GL30C.glDeleteRenderbuffers(renderbuffer);
    }

    @Override
    public int createVertexArray() {
        return GL45C.glCreateVertexArrays();
    }

    @Override
    public void bindVertexArray(int handle) {
        GL30C.glBindVertexArray(handle);
    }

    @Override
    public void bindVertexArrayVertexBuffer(int vertexArray, int bindingIndex, int buffer, long offset, int stride) {
        GL45C.glVertexArrayVertexBuffer(vertexArray, bindingIndex, buffer, offset, stride);
    }

    @Override
    public void bindVertexArrayElementBuffer(int vertexArray, int buffer) {
        GL45C.glVertexArrayElementBuffer(vertexArray, buffer);
    }

    @Override
    public void enableVertexArrayAttribute(int vertexArray, int attributeIndex) {
        GL45C.glEnableVertexArrayAttrib(vertexArray, attributeIndex);
    }

    @Override
    public void setVertexArrayIntegerAttributeFormat(int vertexArray, int attributeIndex, int componentCount, int type, int relativeOffset) {
        GL45C.glVertexArrayAttribIFormat(vertexArray, attributeIndex, componentCount, type, relativeOffset);
    }

    @Override
    public void setVertexArrayFloatAttributeFormat(int vertexArray, int attributeIndex, int componentCount, int type, boolean normalized, int relativeOffset) {
        GL45C.glVertexArrayAttribFormat(vertexArray, attributeIndex, componentCount, type, normalized, relativeOffset);
    }

    @Override
    public void deleteVertexArray(int handle) {
        GL30C.glDeleteVertexArrays(handle);
    }

    @Override
    public void memoryBarrier(int barrierFlags) {
        GL42C.glMemoryBarrier(barrierFlags);
    }

    @Override
    public void finish() {
        GL11C.glFinish();
    }

    /**
     * The graphics context is owned by Minecraft. Closing this adapter must
     * never destroy or detach that context.
     */
    @Override
    public void close() {
        // No context ownership; no native resource is held by this adapter.
    }
}

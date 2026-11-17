package me.cortex.voxy.client.core.rendering.post;

import me.cortex.voxy.client.core.RenderProperties;
import me.cortex.voxy.client.core.gl.shader.Shader;
import me.cortex.voxy.client.core.gl.shader.ShaderType;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.client.core.rendering.util.SharedIndexBuffer;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL45C.glCreateVertexArrays;
import static org.lwjgl.opengl.GL45C.glDeleteVertexArrays;

public class FullscreenBlit {
    private final Shader shader;
    private final int emptyVao;
    private final long contextGeneration;
    private boolean deleted;

    public FullscreenBlit(RenderProperties properties, String fragId) {
        this(properties, fragId, b->{});
    }

    public FullscreenBlit(RenderProperties properties, String vertId, String fragId) {
        this(properties, vertId, fragId, b->{});
    }

    public <T extends Shader> FullscreenBlit(RenderProperties properties, String fragId, Consumer<Shader.Builder<T>> applyer) {
        this(properties, "voxy:post/fullscreen.vert", fragId, applyer);
    }

    public <T extends Shader> FullscreenBlit(RenderProperties properties, String vertId, String fragId, Consumer<Shader.Builder<T>> applyer) {
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        GlBackendRuntime.requireBackend(this.contextGeneration, "FullscreenBlit");
        this.shader = ((Shader.Builder<T>)Shader.make())
                .apply(properties::apply)
                .add(ShaderType.VERTEX, vertId)
                .add(ShaderType.FRAGMENT, fragId)
                .apply(applyer)
                .compile();

        int vao = 0;
        try {
            vao = glCreateVertexArrays();
            if (vao == 0) {
                throw new IllegalStateException("Failed to create fullscreen blit VAO");
            }
        } catch (RuntimeException | Error failure) {
            try {
                this.shader.free();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        this.emptyVao = vao;
    }

    public void bind() {
        this.requireActive();
        this.shader.bind();
    }

    public void blit() {
        this.requireActive();
        glBindVertexArray(this.emptyVao);
        this.shader.bind();
        SharedIndexBuffer indexBuffer = SharedIndexBuffer.INSTANCE_BYTE;
        if (indexBuffer == null) {
            throw new IllegalStateException("SharedIndexBuffer is not initialized for the active GL context");
        }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.id());
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0);
        glBindVertexArray(0);
    }

    public void delete() {
        if (this.deleted) {
            return;
        }
        this.deleted = true;
        RuntimeException failure = null;
        try {
            this.shader.free();
        } catch (RuntimeException e) {
            failure = e;
        }
        if (GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration)) {
            try {
                glDeleteVertexArrays(this.emptyVao);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void requireActive() {
        if (this.deleted) {
            throw new IllegalStateException("FullscreenBlit has been deleted");
        }
        GlBackendRuntime.requireBackend(this.contextGeneration, "FullscreenBlit");
    }
}

package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

import java.util.Arrays;

import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class GlVertexArray extends TrackedObject {
    /**
     * Legacy shared VAO used by callers that only need a VAO bound.
     * Kept on the legacy LWJGL path because this static field may be
     * initialized before GlBackendRuntime has a current backend/context.
     */
    public static final int STATIC_VAO = glGenVertexArrays();

    public final int id;
    private final long contextGeneration;
    private int[] indices = new int[0];
    private int stride;

    public GlVertexArray() {
        this.id = GlBackendRuntime.requireBackend(this.contextGeneration, "GlVertexArray").createVertexArray();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
    }

    @Override
    public void free() {
        super.free0();
        GlBackendRuntime.requireBackend().deleteVertexArray(this.id);
    }

    public void bind() {
        GlBackendRuntime.requireBackend().bindVertexArray(this.id);
    }

    public GlVertexArray bindBuffer(int buffer) {
        if (this.stride <= 0) {
            throw new IllegalStateException("Vertex stride must be set to a positive value before binding a vertex buffer");
        }
        for (int index : this.indices) {
            GlBackendRuntime.requireBackend().bindVertexArrayVertexBuffer(this.id, index, buffer, 0, this.stride);
        }
        return this;
    }

    public GlVertexArray bindElementBuffer(int buffer) {
        GlBackendRuntime.requireBackend().bindVertexArrayElementBuffer(this.id, buffer);
        return this;
    }

    public GlVertexArray setStride(int stride) {
        if (stride <= 0) {
            throw new IllegalArgumentException("Vertex stride must be positive");
        }
        this.stride = stride;
        return this;
    }

    public GlVertexArray setI(int index, int type, int count, int offset) {
        validateAttribute(index, count, offset);
        GlBackendRuntime.requireBackend().enableVertexArrayAttribute(this.id, index);
        GlBackendRuntime.requireBackend().setVertexArrayIntegerAttributeFormat(this.id, index, count, type, offset);
        this.addIndex(index);
        return this;
    }

    public GlVertexArray setF(int index, int type, int count, int offset) {
        return this.setF(index, type, count, false, offset);
    }

    public GlVertexArray setF(int index, int type, int count, boolean normalize, int offset) {
        validateAttribute(index, count, offset);
        GlBackendRuntime.requireBackend().enableVertexArrayAttribute(this.id, index);
        GlBackendRuntime.requireBackend().setVertexArrayFloatAttributeFormat(this.id, index, count, type, normalize, offset);
        this.addIndex(index);
        return this;
    }

    private static void validateAttribute(int index, int count, int offset) {
        if (index < 0) {
            throw new IllegalArgumentException("Vertex attribute index must not be negative");
        }
        if (count < 1 || count > 4) {
            throw new IllegalArgumentException("Vertex attribute component count must be between 1 and 4");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Vertex attribute offset must not be negative");
        }
    }

    private void addIndex(int index) {
        for (int i : this.indices) {
            if (i == index) {
                return;
            }
        }
        this.indices = Arrays.copyOf(this.indices, this.indices.length + 1);
        this.indices[this.indices.length - 1] = index;
    }
}

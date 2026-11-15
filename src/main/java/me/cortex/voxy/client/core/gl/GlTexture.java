package me.cortex.voxy.client.core.gl;

import me.cortex.voxy.common.util.TrackedObject;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;

import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL45C.*;

public class GlTexture extends TrackedObject {
    public final int id;
    private final int type;
    private int format;
    private int width;
    private int height;
    private int levels;
    private boolean hasAllocated;
    private final long contextGeneration;

    private static int COUNT;
    private static long ESTIMATED_TOTAL_SIZE;

    public GlTexture() {
        this(GL_TEXTURE_2D);
    }

    public GlTexture(int type) {
        this.id = GlBackendRuntime.requireBackend().createTexture(type);
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        this.type = type;
        COUNT++;
    }

    private GlTexture(int type, int existingId) {
        this.id = existingId;
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        this.type = type;
        COUNT++;
    }

    public GlTexture store(int format, int levels, int width, int height) {
        if (this.hasAllocated) {
            throw new IllegalStateException("Texture already allocated");
        }
        if (this.type != GL_TEXTURE_2D) {
            throw new IllegalStateException("Only 2D texture storage is supported by this wrapper");
        }
        if (width <= 0 || height <= 0 || levels <= 0) {
            throw new IllegalArgumentException("Texture dimensions and mip level count must be positive");
        }
        int maxLevels = 32 - Integer.numberOfLeadingZeros(Math.max(width, height));
        if (levels > maxLevels) {
            throw new IllegalArgumentException("Mip level count " + levels + " exceeds maximum " + maxLevels);
        }

        long estimatedSize = this.estimateSize(format, levels, width, height);
        long updatedEstimatedTotal = Math.addExact(ESTIMATED_TOTAL_SIZE, estimatedSize);
        GlBackendRuntime.requireBackend(this.contextGeneration, "GlTexture").allocateTexture2D(this.id, levels, format, width, height);

        // Publish wrapper state only after backend allocation succeeds.
        this.format = format;
        this.width = width;
        this.height = height;
        this.levels = levels;
        this.hasAllocated = true;
        ESTIMATED_TOTAL_SIZE = updatedEstimatedTotal;
        return this;
    }

    public GlTexture createView() {
        this.assertAllocated();
        int viewId = GlBackendRuntime.requireBackend(this.contextGeneration, "GlTexture").createTextureView(
                this.id, this.type, this.format, 0, 1, 0, 1);
        return new GlTexture(this.type, viewId);
    }

    @Override
    public void free() {
        boolean allocated = this.hasAllocated;
        long estimatedSize = allocated ? this.getEstimatedSize() : 0L;
        this.hasAllocated = false;
        super.free0();
        try {
            if (GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration)) {
                GlBackendRuntime.requireBackend(this.contextGeneration, "GlTexture").deleteTexture(this.id);
            }
        } finally {
            COUNT--;
            if (allocated) ESTIMATED_TOTAL_SIZE -= estimatedSize;
        }
    }

    public GlTexture name(String name) {
        this.assertAllocated();
        this.assertContextCurrent();
        return GlDebug.name(name, this);
    }

    public int getWidth() {
        this.assertAllocated();
        return this.width;
    }

    public int getHeight() {
        this.assertAllocated();
        return this.height;
    }

    public int getLevels() {
        this.assertAllocated();
        return this.levels;
    }

    public int getFormat() {
        this.assertAllocated();
        return this.format;
    }

    public int getPixelTransferFormat() {
        this.assertAllocated();
        return switch (this.format) {
            case GL_RGBA8 -> GL_RGBA;
            case GL_RG16F -> GL_RG;
            case GL_R32UI -> GL_RED_INTEGER;
            case GL_R32F -> GL_RED;
            case GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT32F,GL_DEPTH_COMPONENT32 -> GL_DEPTH_COMPONENT;
            case GL_DEPTH24_STENCIL8 -> GL_DEPTH_STENCIL;
            default -> throw new IllegalStateException("Unknown format");
        };
    }

    private long getEstimatedSize() {
        this.assertAllocated();
        return this.estimateSize(this.format, this.levels, this.width, this.height);
    }

    private long estimateSize(int format, int levels, int width, int height) {
        long elemSize = switch (format) {
            case GL_R32UI, GL_RGBA8, GL_DEPTH24_STENCIL8, GL_R32F, GL_RG16F,
                    GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT32 -> 4;
            default -> throw new IllegalStateException("Unknown texture element size for format: " + format);
        };

        long size = 0;
        for (int lvl = 0; lvl < levels; lvl++) {
            long levelWidth = Math.max(((long) width) >> lvl, 1L);
            long levelHeight = Math.max(((long) height) >> lvl, 1L);
            size = Math.addExact(size, Math.multiplyExact(Math.multiplyExact(levelWidth, levelHeight), elemSize));
        }
        return size;
    }

    public void assertContextCurrent() {
        GlBackendRuntime.requireBackend(this.contextGeneration, "GlTexture");
    }

    public void assertAllocated() {
        if (!this.hasAllocated) {
            throw new IllegalStateException("Texture not yet allocated");
        }
    }

    public GlTexture zero() {
        this.assertAllocated();
        int type = switch (this.format) {
            case GL_R32UI -> GL_UNSIGNED_INT;
            case GL_RGBA8 -> GL_INT;
            case GL_R32F,GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT32F,GL_DEPTH_COMPONENT32, GL_RG16F -> GL_FLOAT;
            case GL_DEPTH24_STENCIL8 -> GL_UNSIGNED_INT_24_8;
            case GL_DEPTH32F_STENCIL8 -> GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
            default -> throw new IllegalStateException("Unknown format");
        };
        for (int lvl = 0; lvl < this.levels; lvl++) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "GlTexture").clearTextureImage(
                    this.id, lvl, this.getPixelTransferFormat(), type, 0);
        }
        return this;
    }

    public static int getCount() {
        return COUNT;
    }

    public static long getEstimatedTotalSize() {
        return ESTIMATED_TOTAL_SIZE;
    }
}

// 1.20.1 replacement of the 26.3 LightMapHelper: only getLightmapTextureId() differs (no RenderPearl GpuTexture).
package me.cortex.voxy.client.core.rendering.util;

import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL33.glDeleteSamplers;
import static org.lwjgl.opengl.GL33.glSamplerParameteri;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;
import static org.lwjgl.opengl.GL45.glCreateSamplers;

/**
 * Binds Minecraft's lightmap texture with Voxy's linear/clamp sampler.
 * The sampler is lazy and belongs to the active GL context generation.
 */
public final class LightMapHelper {
    private static int lightmapSampler;
    private static long samplerGeneration = -1L;

    private LightMapHelper() {}

    private static synchronized int requireSampler() {
        long generation = GlBackendRuntime.getContextGeneration();
        GlBackendRuntime.requireBackend(generation, "LightMapHelper sampler");

        if (lightmapSampler != 0 && samplerGeneration == generation) {
            return lightmapSampler;
        }

        // A handle from a previous generation is invalid; do not delete it
        // through the replacement context.
        lightmapSampler = 0;
        samplerGeneration = -1L;

        int sampler = glCreateSamplers();
        if (sampler == 0) {
            throw new IllegalStateException("Failed to create Voxy lightmap sampler");
        }
        try {
            glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glSamplerParameteri(sampler, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        } catch (RuntimeException | Error failure) {
            try {
                glDeleteSamplers(sampler);
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }

        lightmapSampler = sampler;
        samplerGeneration = generation;
        return sampler;
    }

    public static void bind(int lightingIndex) {
        if (lightingIndex < 0) {
            throw new IllegalArgumentException("lightingIndex must be non-negative");
        }
        int sampler = requireSampler();
        glBindSampler(lightingIndex, sampler);
        glBindTextureUnit(lightingIndex, getLightmapTextureId());
    }

    /**
     * Releases the sampler while its owning context is still active.
     * Safe to call repeatedly and after context loss.
     */
    public static synchronized void shutdown() {
        int sampler = lightmapSampler;
        long generation = samplerGeneration;
        lightmapSampler = 0;
        samplerGeneration = -1L;

        if (sampler != 0 && GlBackendRuntime.isContextGenerationCurrent(generation)) {
            glDeleteSamplers(sampler);
        }
    }

    public static int getLightmapTextureId() {
        // 1.20.1: the lightmap is LightTexture#lightTexture (a DynamicTexture); the field is made accessible
        // in voxy-1.20.1.accesswidener. Must be called on the render thread (AbstractTexture#getId).
        return Minecraft.getInstance().gameRenderer.lightTexture().lightTexture.getId();
    }
}

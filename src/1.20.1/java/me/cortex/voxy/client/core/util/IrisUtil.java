package me.cortex.voxy.client.core.util;

import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.Viewport;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.shadows.ShadowRenderer;

import java.io.IOException;

/**
 * Iris utility bridge for the legacy Fabric 1.20.1 profile.
 * Viewport capture hooks are intentionally not installed until a matching
 * Iris 1.7.x integration is implemented.
 */
public class IrisUtil {
    public static final boolean IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");
    public static final boolean SHADER_SUPPORT = true;

    private static boolean irisShadowActive0() {
        return ShadowRenderer.ACTIVE;
    }

    public static boolean irisShadowActive() {
        return IRIS_INSTALLED && irisShadowActive0();
    }

    public static void clearIrisSamplers() {
        if (IRIS_INSTALLED) {
            for (int i = 0; i < 16; i++) {
                IrisRenderSystem.bindSamplerToUnit(i, 0);
            }
        }
    }

    public static void reload() {
        if (!IRIS_INSTALLED) return;
        try {
            if (IrisApi.getInstance().isShaderPackInUse()
                    || IrisApi.getInstance().getConfig().areShadersEnabled()) {
                Iris.reload();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean irisShaderPackEnabled() {
        return IRIS_INSTALLED && Iris.getCurrentPack().isPresent();
    }

    public static boolean irisShadersEnabledInConfig() {
        return IRIS_INSTALLED && !Iris.getCurrentPack().isEmpty();
    }

    public static void disableIrisShaders() {
        if (IRIS_INSTALLED) {
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
        }
    }
}

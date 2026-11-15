package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.20.1 replacement for MixinLevelExtractor: on 1.20.1 setLevel/allChanged
 * live directly on LevelRenderer (which MixinLevelRenderer already makes an IVoxyRenderSystemHolder).
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRendererLifecycle {
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void voxy$onSetLevel(ClientLevel level, CallbackInfo ci) {
        ((IVoxyRenderSystemHolder) (Object) this).voxy$setWorld(level);
    }

    @Inject(method = "allChanged", at = @At("HEAD"))
    private void voxy$reload(CallbackInfo ci) {
        ((IVoxyRenderSystemHolder) (Object) this).voxy$shutdownRenderer();
        ((IVoxyRenderSystemHolder) (Object) this).voxy$createRenderer();
    }
}

package me.cortex.voxy.client.mixin.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import me.cortex.voxy.client.VoxyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 1.20.1 variant: RenderSystem.initRenderer(int, boolean), no RenderPearl GpuDevice. */
@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "initRenderer(IZ)V", order = 900, at = @At("RETURN"))
    private static void voxy$injectInit(int debugVerbosity, boolean synchronous, CallbackInfo ci) {
        VoxyClient.initVoxyClient();
    }
}

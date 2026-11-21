package me.cortex.voxy.client.mixin.minecraft.session;

import me.cortex.voxy.client.ClientSessionEvents;
import me.cortex.voxy.client.VoxyClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 1.20.1 variant: no RenderPearl backend wrapper, disconnect(Screen) signature. */
@Mixin(Minecraft.class)
public class MixinMinecraft {
    // Minecraft 1.20.1 has no RenderSystem.initRenderer(GpuDevice); initialize once the client loop starts with its GL context active.
    @Inject(method = "run", at = @At("HEAD"))
    private void voxy$initGraphics(CallbackInfo ci) {
        VoxyClient.initVoxyClient();
    }

    @Inject(method = "method_18096", at = @At("TAIL"), remap = false)
    private void voxy$injectWorldClose(CallbackInfo ci) {
        if (ClientSessionEvents.inSession) {
            ClientSessionEvents.sessionEnd();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void voxy$shutdownGraphicsResources(CallbackInfo ci) {
        if (ClientSessionEvents.inSession) {
            ClientSessionEvents.sessionEnd();
        }
        VoxyClient.shutdownGraphicsResources();
    }
}

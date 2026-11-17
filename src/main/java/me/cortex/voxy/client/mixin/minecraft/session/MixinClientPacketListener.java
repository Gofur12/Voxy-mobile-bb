package me.cortex.voxy.client.mixin.minecraft.session;

import me.cortex.voxy.client.ClientSessionEvents;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    // The packet's spawn-info accessor differs across Minecraft versions.
    // Running at TAIL is stable for both the legacy 1.20.1 packet and newer targets.
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void voxy$init(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (!ClientSessionEvents.inSession) {
            ClientSessionEvents.sessionStart();
        }
    }
}

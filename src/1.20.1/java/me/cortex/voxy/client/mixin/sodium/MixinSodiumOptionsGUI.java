package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.config.VoxyConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a direct Voxy settings entry to Sodium's Video Settings screen.
 * Mod Menu remains available as a secondary entry point.
 */
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI", remap = false)
public abstract class MixinSodiumOptionsGUI extends Screen {
    protected MixinSodiumOptionsGUI(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void voxy$addVideoSettingsButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("Voxy Settings"), button ->
                Minecraft.getInstance().setScreen(new VoxyConfigScreen((Screen) (Object) this)))
                .bounds(Math.max(8, this.width - 112), 6, 104, 20)
                .build());
    }
}

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
 * Adds a direct Voxy settings entry to Sodium 0.5.x Video Settings.
 * SodiumOptionsGUI inherits Screen.init() instead of declaring its own init method,
 * so hook Screen and restrict the button to the Sodium options screen.
 */
@Mixin(Screen.class)
public abstract class MixinSodiumOptionsGUI {
    @Inject(method = "init", at = @At("TAIL"))
    private void voxy$addVideoSettingsButton(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!"me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI".equals(
                screen.getClass().getName())) {
            return;
        }

        screen.addRenderableWidget(Button.builder(Component.literal("Voxy Settings"), button ->
                Minecraft.getInstance().setScreen(new VoxyConfigScreen(screen)))
                .bounds(Math.max(8, screen.width - 112), 6, 104, 20)
                .build());
    }
}

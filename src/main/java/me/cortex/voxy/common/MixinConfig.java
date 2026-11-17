package me.cortex.voxy.common;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MixinConfig implements IMixinConfigPlugin {
    private String mixinPackage;
    private final Set<String> fallThrough;

    public MixinConfig() {
        this.fallThrough = Set.of();
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage + ".";//we want to get rid of the .
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixin) {
        // GPUSelect loads GDI32/Kernel32 and patches Windows driver selection.
        // Do not apply it on Linux, macOS, Android, or other non-Windows runtimes.
        if (mixin.endsWith(".minecraft.util.MixinGPUSelect") && !isWindowsRuntime()) {
            return false;
        }
        if (mixin.startsWith(this.mixinPackage)) {
            var pth = mixin.substring(this.mixinPackage.length());
            if (!pth.contains(".")) return true;
            var targetMod = pth.substring(0, pth.indexOf('.'));
            //filter out fallthrough and if the mod is actually loaded
            if (this.fallThrough.contains(targetMod)) return true;
            if (FabricLoader.getInstance().isModLoaded(targetMod)) return true;
            //need to remove it cause it shouldnt be applied
            return false;
        }
        return true;
    }

    private static boolean isWindowsRuntime() {
        return propertyContains("os.name", "windows");
    }

    private static boolean propertyContains(String property, String value) {
        return System.getProperty(property, "").toLowerCase(Locale.ROOT).contains(value);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

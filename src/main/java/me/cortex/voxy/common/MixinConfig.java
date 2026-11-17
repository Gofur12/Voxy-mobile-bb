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
        // GPUSelect is Windows-specific (GPUSelectorWindows2 and Win32 thread priority).
        // Its constructor injection is not present on the Android 1.20.1 runtime.
        if (mixin.endsWith(".minecraft.util.MixinGPUSelect") && isAndroidRuntime()) {
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

    private static boolean isAndroidRuntime() {
        return propertyContains("os.name", "android")
                || propertyContains("os.version", "android")
                || propertyContains("java.vendor", "android")
                || propertyContains("java.runtime.name", "android")
                || propertyContains("java.vm.name", "dalvik");
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

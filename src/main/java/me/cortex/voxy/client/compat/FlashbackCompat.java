package me.cortex.voxy.client.compat;

import me.cortex.voxy.common.Logger;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public class FlashbackCompat {
    public static final boolean FLASHBACK_INSTALLED = FabricLoader.getInstance().isModLoaded("flashback");

    public static Path getReplayStoragePath() {
        if (!FLASHBACK_INSTALLED) {
            return null;
        }
        return getReplayStoragePath0();
    }

    /*
     * Flashback's internal Java packages are not a stable compile-time API and
     * differ between supported Minecraft generations. Resolve the optional
     * integration reflectively so Voxy can still compile when Flashback is
     * absent from a target profile.
     */
    private static Path getReplayStoragePath0() {
        try {
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback");
            Object replayServer = flashback.getMethod("getReplayServer").invoke(null);
            if (replayServer == null) {
                return null;
            }

            Object meta = replayServer.getClass().getMethod("getMetadata").invoke(replayServer);
            if (!(meta instanceof IFlashbackMeta voxyMeta)) {
                return null;
            }

            var path = voxyMeta.getVoxyPath();
            if (path == null) {
                return null;
            }
            Logger.info("Flashback replay server exists and meta exists");
            if (path.exists()) {
                Logger.info("Flashback voxy path exists in filesystem, using this as lod data source");
                return path.toPath();
            }
            Logger.warn("Flashback meta had voxy path saved but path doesnt exist");
            return null;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            Logger.warn("Flashback compatibility unavailable: " + e.getClass().getSimpleName());
            return null;
        }
    }
}

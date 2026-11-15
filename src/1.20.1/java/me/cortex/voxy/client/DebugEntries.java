package me.cortex.voxy.client;

/**
 * Legacy 1.20.1 debug-HUD adapter.
 *
 * The extra debug entry registry used by the modern Minecraft client does not
 * exist on 1.20.1. Keep the initializer callable while the legacy debug overlay
 * implementation is developed separately; this does not alter renderer startup.
 */
public final class DebugEntries {
    private DebugEntries() {
    }

    public static void init() {
        // Optional Voxy debug HUD integration is not registered on this target.
    }
}

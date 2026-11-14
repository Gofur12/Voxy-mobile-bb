package me.cortex.voxy.client.core.gl.backend;

import me.cortex.voxy.common.Logger;
import java.util.Objects;

/** Selects and owns the backend adapter for Minecraft's current graphics context. */
public final class GlBackendRuntime {
    private static GlBackend backend;
    private static Thread initializationThread;
    private GlBackendRuntime() {}

    /** Must be called after renderer initialization on a thread with the context current. */
    public static synchronized boolean initialize(GlBackendProvider... providers) {
        Objects.requireNonNull(providers, "providers");
        if (backend != null) return true;
        for (GlBackendProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            try {
                GlBackend candidate = provider.createIfSupported();
                if (candidate != null) {
                    backend = candidate;
                    initializationThread = Thread.currentThread();
                    Logger.info("Selected Voxy GPU backend: " + candidate.name());
                    return true;
                }
            } catch (RuntimeException failure) {
                Logger.warn("Voxy GPU backend provider '" + provider.id() + "' failed: " + failure.getMessage());
            }
        }
        Logger.warn("No compatible Voxy GPU backend was found for the current context");
        return false;
    }

    /** Returns the selected backend; its matching graphics context must be current. */
    public static synchronized GlBackend requireBackend() {
        if (backend == null) throw new IllegalStateException("Voxy GPU backend is not initialized");
        return backend;
    }

    public static synchronized boolean isInitialized() { return backend != null; }
    public static synchronized boolean isInitializationThread() {
        return backend != null && initializationThread == Thread.currentThread();
    }
}
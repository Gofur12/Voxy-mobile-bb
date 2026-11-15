package me.cortex.voxy.client.core.gl.backend;

import me.cortex.voxy.common.Logger;

import java.util.Objects;

/**
 * Selects and owns the backend adapter for Minecraft's current graphics context.
 *
 * A backend is bound to the context and thread on which it was initialized.
 * Context loss must be reported through {@link #onContextLost()} before a
 * replacement context is initialized.
 */
public final class GlBackendRuntime {
    private static GlBackend backend;
    private static Thread initializationThread;
    private static long contextGeneration;

    private GlBackendRuntime() {}

    /**
     * Must be called after renderer initialization on a thread with the
     * intended graphics context current. Repeated calls for the same context
     * are idempotent; call onContextLost() before selecting a backend for a
     * replacement context.
     */
    public static synchronized boolean initialize(GlBackendProvider... providers) {
        Objects.requireNonNull(providers, "providers");
        if (backend != null) {
            if (initializationThread != Thread.currentThread()) {
                throw new IllegalStateException("Voxy GPU backend is already bound to another thread");
            }
            return true;
        }

        for (GlBackendProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            GlBackend candidate = null;
            try {
                candidate = provider.createIfSupported();
                if (candidate != null) {
                    backend = candidate;
                    initializationThread = Thread.currentThread();
                    contextGeneration++;
                    Logger.info("Selected Voxy GPU backend: " + candidate.name()
                            + " (context generation " + contextGeneration + ")");
                    return true;
                }
            } catch (RuntimeException failure) {
                // A provider that constructed a backend but failed before
                // returning it still owns cleanup of its partial construction.
                Logger.warn("Voxy GPU backend provider '" + provider.id()
                        + "' failed: " + failure.getMessage());
            }
        }
        Logger.warn("No compatible Voxy GPU backend was found for the current context");
        return false;
    }

    /**
     * Returns the selected backend. GPU operations must be issued on the
     * initialization thread while that backend's graphics context is current.
     */
    public static synchronized GlBackend requireBackend() {
        if (backend == null) {
            throw new IllegalStateException("Voxy GPU backend is not initialized");
        }
        if (initializationThread != Thread.currentThread()) {
            throw new IllegalStateException("Voxy GPU backend accessed from a thread other than its context thread");
        }
        return backend;
    }

    /**
     * Call when the graphics context has been lost or replaced. Do not issue
     * GL deletion calls here: handles belonging to a lost context are already
     * invalid. Higher-level renderer/resource owners must discard their old
     * wrappers and recreate resources after the next successful initialize.
     */
    public static synchronized void onContextLost() {
        backend = null;
        initializationThread = null;
    }

    /**
     * Gracefully closes a still-valid backend on its owning context thread.
     * Use onContextLost() instead when the context is already unavailable.
     */
    public static synchronized void shutdown() {
        if (backend == null) return;
        if (initializationThread != Thread.currentThread()) {
            throw new IllegalStateException("Voxy GPU backend must be shut down on its context thread");
        }
        GlBackend previous = backend;
        try {
            previous.close();
        } finally {
            backend = null;
            initializationThread = null;
        }
    }

    public static synchronized boolean isInitialized() {
        return backend != null;
    }

    public static synchronized boolean isInitializationThread() {
        return backend != null && initializationThread == Thread.currentThread();
    }

    /** Monotonically increases each time a new backend/context is initialized. */
    public static synchronized long getContextGeneration() {
        return contextGeneration;
    }
}

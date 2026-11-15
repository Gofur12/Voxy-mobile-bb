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
    /**
     * Lifecycle of the backend binding, not a claim that renderer resources
     * have been reconstructed. LOST requires higher-level owners to discard
     * stale resources before a replacement backend is used.
     */
    public enum State {
        UNINITIALIZED,
        ACTIVE,
        CONTEXT_LOST,
        SHUTDOWN
    }

    private static State state = State.UNINITIALIZED;
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
        if (state == State.SHUTDOWN) {
            throw new IllegalStateException("Voxy GPU backend runtime has been shut down");
        }
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
                    state = State.ACTIVE;
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
        state = State.CONTEXT_LOST;
    }

    /**
     * Gracefully closes a still-valid backend on its owning context thread.
     * Use onContextLost() instead when the context is already unavailable.
     */
    public static synchronized void shutdown() {
        if (backend == null) {
            state = State.SHUTDOWN;
            return;
        }
        if (initializationThread != Thread.currentThread()) {
            throw new IllegalStateException("Voxy GPU backend must be shut down on its context thread");
        }
        GlBackend previous = backend;
        try {
            previous.close();
        } finally {
            backend = null;
            initializationThread = null;
            state = State.SHUTDOWN;
        }
    }

    /** Returns the explicit backend lifecycle state. */
    public static synchronized State getState() {
        return state;
    }

    public static synchronized boolean isInitialized() {
        return backend != null;
    }

    public static synchronized boolean isInitializationThread() {
        return backend != null && initializationThread == Thread.currentThread();
    }

    /**
     * Returns whether a resource belongs to the currently initialized context.
     * This deliberately returns false after context loss, allowing wrappers to
     * release Java-side tracking without issuing invalid GL deletion calls.
     */
    public static synchronized boolean isContextGenerationCurrent(long generation) {
        return backend != null && contextGeneration == generation;
    }

    /**
     * Returns the backend only if the caller's resource belongs to the active
     * context generation and the caller is on its context thread.
     */
    public static synchronized GlBackend requireBackend(long generation, String resourceType) {
        GlBackend current = requireBackend();
        if (contextGeneration != generation) {
            throw new IllegalStateException(resourceType + " belongs to stale GL context generation "
                    + generation + "; current generation is " + contextGeneration);
        }
        return current;
    }

    /** Monotonically increases each time a new backend/context is initialized. */
    public static synchronized long getContextGeneration() {
        return contextGeneration;
    }
}

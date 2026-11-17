package me.cortex.voxy.client.core.gl.backend;

/**
 * Exception-safe scope for operations that temporarily modify framebuffer
 * state while clearing a texture through a framebuffer fallback.
 *
 * Use with try-with-resources:
 *
 * <pre>
 * try (GlFramebufferClearStateScope ignored =
 *              GlFramebufferClearStateScope.open(backend)) {
 *     // Bind and clear using a temporary framebuffer.
 * }
 * </pre>
 *
 * The backend captures the state when the scope is opened and restores it
 * exactly once when the scope is closed. Java's try-with-resources semantics
 * ensure close() is invoked when the scoped operation throws; if restoration
 * also fails, Java preserves the original failure and records the restore
 * failure as a suppressed exception.
 *
 * This helper does not create a framebuffer or issue a clear command.
 */
public final class GlFramebufferClearStateScope implements AutoCloseable {
    private final GlBackend backend;
    private final GlFramebufferClearState state;
    private boolean closed;

    private GlFramebufferClearStateScope(GlBackend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("GL backend must not be null");
        }
        this.backend = backend;
        this.state = backend.captureFramebufferClearState();
        if (this.state == null) {
            throw new IllegalStateException("GL backend returned a null framebuffer-clear state snapshot");
        }
    }

    /**
     * Captures the current framebuffer-related state and opens a restore scope.
     *
     * @throws UnsupportedOperationException when the backend has not implemented
     *                                        framebuffer state capture
     */
    public static GlFramebufferClearStateScope open(GlBackend backend) {
        return new GlFramebufferClearStateScope(backend);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        // Mark closed before invoking backend code so a failed restoration is
        // not accidentally retried against partially restored state.
        this.closed = true;
        this.backend.restoreFramebufferClearState(this.state);
    }
}

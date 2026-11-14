package me.cortex.voxy.client.core.gl.backend;

/**
 * Service-provider contract for a renderer backend.
 *
 * A provider must inspect the current context and return null when its
 * implementation is not compatible. Providers must not create a context.
 */
public interface GlBackendProvider {
    String id();

    /**
     * Creates a backend for the already-current graphics context.
     *
     * @return a backend, or null when this provider cannot safely support
     *         the current context.
     */
    GlBackend createIfSupported();
}

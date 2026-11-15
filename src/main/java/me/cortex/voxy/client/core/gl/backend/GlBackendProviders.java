package me.cortex.voxy.client.core.gl.backend;

import me.cortex.voxy.client.core.gl.backend.desktop.DesktopGlBackendProvider;
import me.cortex.voxy.common.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovers optional platform backend providers before falling back to the
 * desktop OpenGL implementation.
 *
 * A platform adapter can be shipped separately and register its provider in
 * META-INF/services/me.cortex.voxy.client.core.gl.backend.GlBackendProvider.
 * Providers must inspect an already-current context and return null when
 * unavailable; they must never create or take ownership of the host context.
 */
public final class GlBackendProviders {
    private static final String DESKTOP_PROVIDER_ID = "desktop-opengl-45";

    private GlBackendProviders() {}

    /**
     * Returns discovered non-desktop providers first, followed by the built-in
     * desktop fallback. The returned array is ready for GlBackendRuntime's
     * first-supported-provider selection.
     */
    public static GlBackendProvider[] discover() {
        List<GlBackendProvider> providers = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = GlBackendProvider.class.getClassLoader();
        }

        Iterator<GlBackendProvider> iterator = ServiceLoader
                .load(GlBackendProvider.class, loader)
                .iterator();

        while (true) {
            final GlBackendProvider provider;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                provider = iterator.next();
            } catch (ServiceConfigurationError failure) {
                Logger.warn("Unable to load an optional Voxy GPU backend provider: "
                        + failure.getMessage());
                break;
            }

            if (provider == null) {
                continue;
            }

            String id = provider.id();
            if (id == null || id.isBlank()) {
                Logger.warn("Ignoring Voxy GPU backend provider with an empty id");
                continue;
            }
            if (DESKTOP_PROVIDER_ID.equals(id)) {
                // The desktop fallback is always appended last, so an optional
                // service entry cannot accidentally pre-empt platform adapters.
                continue;
            }
            if (ids.add(id)) {
                providers.add(provider);
            } else {
                Logger.warn("Ignoring duplicate Voxy GPU backend provider id: " + id);
            }
        }

        providers.add(new DesktopGlBackendProvider());
        return providers.toArray(new GlBackendProvider[0]);
    }
}

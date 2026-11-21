package me.cortex.voxy.client.core.gl.backend;

/**
 * Immutable snapshot of features actually exposed by the current GL context.
 * A feature must be true only after version/extension checks and, where needed,
 * a small runtime probe. Renderer/vendor names are not capability evidence.
 *
 * This class deliberately uses Java 17-compatible syntax for the 1.20.1 line.
 */
public final class GlCapabilities {
    public final int majorVersion;
    public final int minorVersion;
    public final boolean embeddedProfile;
    public final boolean bufferStorage;
    public final boolean persistentMapping;
    public final boolean computeShaders;
    public final boolean shaderStorageBuffers;
    public final boolean imageLoadStore;
    public final boolean multiDrawIndirect;
    public final boolean indirectParameters;
    public final boolean textureViews;
    public final boolean subgroupOperations;
    public final boolean shaderInt64;
    public final boolean syncObjects;

    public GlCapabilities(
            int majorVersion,
            int minorVersion,
            boolean embeddedProfile,
            boolean bufferStorage,
            boolean persistentMapping,
            boolean computeShaders,
            boolean shaderStorageBuffers,
            boolean imageLoadStore,
            boolean multiDrawIndirect,
            boolean indirectParameters,
            boolean textureViews,
            boolean subgroupOperations,
            boolean shaderInt64,
            boolean syncObjects
    ) {
        if (majorVersion < 1 || minorVersion < 0) {
            throw new IllegalArgumentException("Invalid GL version");
        }
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.embeddedProfile = embeddedProfile;
        this.bufferStorage = bufferStorage;
        this.persistentMapping = persistentMapping;
        this.computeShaders = computeShaders;
        this.shaderStorageBuffers = shaderStorageBuffers;
        this.imageLoadStore = imageLoadStore;
        this.multiDrawIndirect = multiDrawIndirect;
        this.indirectParameters = indirectParameters;
        this.textureViews = textureViews;
        this.subgroupOperations = subgroupOperations;
        this.shaderInt64 = shaderInt64;
        this.syncObjects = syncObjects;
    }

    /**
     * Minimum context-version gate for a future GLES 3.x raster renderer.
     *
     * This only confirms the API generation. It does not claim that a GLES
     * renderer exists or that desktop-only resource/shader code is portable.
     */
    public boolean supportsGles3BasicRasterPath() {
        return this.embeddedProfile
                && (this.majorVersion > 3 || (this.majorVersion == 3 && this.minorVersion >= 0));
    }

    public boolean supportsComputeLodPath() {
        return this.computeShaders && this.shaderStorageBuffers && this.imageLoadStore;
    }

    /** Compute-based LOD candidate for an embedded context (for example GLES 3.1+). */
    public boolean supportsGlesComputeLodPath() {
        return this.embeddedProfile && this.supportsGles3BasicRasterPath() && this.supportsComputeLodPath();
    }

    /**
     * The current MDIC renderer is a desktop compute/indirect-draw pipeline.
     * Keep this predicate conservative: an embedded context must never be
     * selected for it, and every feature used by its GPU-driven path must be
     * present before renderer construction is attempted.
     */
    public boolean supportsDesktopMdicPath() {
        return missingDesktopMdicFeatures().isEmpty();
    }

    /**
     * Lists the exact desktop MDIC requirements absent from the active context.
     * This is diagnostic only; it never relaxes the renderer compatibility gate.
     */
    public java.util.List<String> missingDesktopMdicFeatures() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (this.majorVersion < 4 || (this.majorVersion == 4 && this.minorVersion < 6)) {
            missing.add("OpenGL 4.6 (Voxy LOD shaders use GLSL 460)");
        }
        if (this.embeddedProfile) {
            missing.add("desktop OpenGL profile (embedded/GLES context detected)");
        }
        if (!this.computeShaders) {
            missing.add("compute shaders (OpenGL 4.3 or ARB_compute_shader)");
        }
        if (!this.shaderStorageBuffers) {
            missing.add("shader storage buffers (OpenGL 4.3 or ARB_shader_storage_buffer_object)");
        }
        if (!this.imageLoadStore) {
            missing.add("image load/store (OpenGL 4.2 or ARB_shader_image_load_store)");
        }
        if (!this.multiDrawIndirect) {
            missing.add("multi-draw indirect (OpenGL 4.3 or ARB_multi_draw_indirect)");
        }
        if (!this.indirectParameters) {
            missing.add("indirect parameters (ARB_indirect_parameters)");
        }
        return java.util.Collections.unmodifiableList(missing);
    }
}

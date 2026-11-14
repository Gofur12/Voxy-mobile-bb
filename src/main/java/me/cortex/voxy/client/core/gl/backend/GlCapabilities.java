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

    public boolean supportsComputeLodPath() {
        return this.computeShaders && this.shaderStorageBuffers && this.imageLoadStore;
    }

    public boolean supportsDesktopMdicPath() {
        return !this.embeddedProfile
                && this.computeShaders
                && this.shaderStorageBuffers
                && this.multiDrawIndirect
                && this.indirectParameters;
    }
}

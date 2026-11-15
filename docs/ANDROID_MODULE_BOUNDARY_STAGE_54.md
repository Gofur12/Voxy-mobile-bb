# Android / Fabric Module Boundary — Stage 54

## Decision

Keep the current root project as the desktop Fabric build. Do not apply Android Gradle Plugin, Android SDK requirements, Android imports, or GLES binding dependencies to the root desktop source set.

The Android work must be an opt-in, independently buildable target. The repository currently has only one Fabric/Loom project and no Android module or Android SDK configuration. The root build selects Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.160.5+26.3, and Java release 25.

## Proposed project layout

This is the intended separation, not a claim that these modules already exist:

```text
voxy/
  common/                     # renderer-independent LOD/world/storage code
  versions/
    fabric-1.20.1/             # MC 1.20.1 mappings, mixins, Fabric/Sodium adapter
    fabric-26.3/               # current modern MC integration
  render/
    api/                       # backend-neutral GL contracts and resource semantics
    desktop-lwjgl/             # current desktop OpenGL implementation
    android-gles/              # GLES implementation; opt-in target
  docs/
```

Do not move source files mechanically into these directories yet. First establish package ownership, mixin boundaries, access wideners, resources, and dependencies for each Minecraft version. The current root source is still coupled to its modern Minecraft target.

## Module responsibilities

### common

May contain only code that does not import Minecraft client classes, Fabric, Sodium, Iris, LWJGL, Android, EGL, or GLES. Candidate areas include LOD data structures, serialization-independent hierarchy logic, CPU traversal, and geometry generation after their dependencies are verified.

### versions/fabric-1.20.1 and versions/fabric-26.3

Own Minecraft-version-specific entrypoints, mixins, mappings, Sodium/Iris integration, config UI integration, and resource registration. These are separate compatibility targets; a single dependency set must not be assumed to cover both.

The existing root project currently targets 26.3 / Java 25. The 1.20.1 target needs a separately pinned Fabric Loader, Fabric API, Sodium/Iris compatibility set, mappings, and Java 17 toolchain. Do not change the root version strings to 1.20.1 as a shortcut.

### render/api

Owns backend-neutral contracts. The existing `GlBackend` is a starting point, but it still exposes operations that are not core GLES 3.0 operations (for example texture views, clear-image operations, and desktop-style vertex binding semantics). Those methods need explicit capability/fallback semantics before the contract can be considered a portable API.

### render/desktop-lwjgl

Owns `DesktopGlBackend` and all desktop LWJGL imports. Android code must not load this implementation or resolve desktop native classifiers during client startup.

### render/android-gles

Owns GLES calls and a provider for an already-current context. It must not create or replace the launcher's EGL context. The provider must query the current context's actual GLES version and extensions, then reject unsupported features safely.

The exact Java binding strategy remains an integration decision: Android GLES APIs, an available LWJGL GLES binding, or a launcher-provided bridge are not interchangeable. No binding dependency is added until the target launcher/runtime contract is selected.

## Android launcher/context boundary

The first device profile is Android + Fabric 1.20.1 + GLES-capable renderer, initially Snapdragon 860 / Adreno 640. The launcher/renderer (for example MobileGlues, or a Zink/Kopper translation stack) owns context creation and presentation.

Voxy's Android provider should:
1. Run only after the renderer has made the intended GL context current.
2. Verify it is on the owning render thread.
3. Read actual API version and extension strings.
4. Build a capability snapshot from verified features, not the renderer's product name.
5. Return no backend when the required baseline is absent.
6. Never assume Zink/Kopper means desktop GL 4.5 is fully available.
7. Release only Voxy-owned resources; never destroy the launcher's context.

A MobileGlues-specific bridge may be needed if the launcher does not expose a stable current-context hook to Fabric mods. That API must be confirmed against the chosen MobileGlues version before implementation.

## Build isolation rules

- Default root build must continue to build the existing desktop target without Android SDK installed.
- Android target must be opt-in until its plugin, SDK, Java toolchain, and dependency set are pinned.
- Android-only dependencies must not leak into the desktop runtime classpath.
- Desktop LWJGL native classifiers must not be packaged into the Android runtime.
- Version-specific Minecraft/Fabric/Sodium dependencies must remain isolated per target.
- No module may silently compile against the wrong Minecraft version.
- The same source file may be shared only after it passes the lowest supported Java language/API baseline.

## Required decisions before writing the Android module

1. Confirm the first launcher/runtime integration point (MobileGlues version and how a Fabric mod can detect the current context/render thread).
2. Choose the GLES binding API available in that runtime.
3. Pin the 1.20.1 Fabric Loader, Fabric API, Sodium, Iris (if required), mappings, and Java 17 toolchain.
4. Identify common code that can be extracted without importing modern Minecraft APIs.
5. Define how the Android artifact is packaged and installed by the launcher.
6. Add a build job that compiles the opt-in Android target without affecting the root desktop build.

## Stage 54 acceptance

- [x] Root build configuration and Gradle wrapper inspected.
- [x] Confirmed there is no existing Android module, Android plugin, Android SDK setup, EGL integration, or GLES binding dependency.
- [x] Defined separate ownership for common logic, version adapters, desktop LWJGL, and Android GLES.
- [x] Defined the launcher-owned context boundary and fail-closed provider behavior.
- [ ] Android Gradle module created and compiled.
- [ ] Fabric 1.20.1 version target created and compiled.
- [ ] Current-context bridge confirmed against the chosen launcher.
- [ ] Android GLES backend implemented and tested on-device.

## Verification limits

This stage changes documentation only. It does not create a dummy Android module or add unbuildable Android plugin configuration. No Gradle build, unit tests, Minecraft launch, launcher integration test, or Android device test was run.

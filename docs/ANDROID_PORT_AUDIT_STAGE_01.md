# Android Port Audit — Stage 01

Date: 2026-11-14
Repository baseline: `main` at `1eb84b84f25b5d7923deb832a5a186be356bb6a3`
Audit branch: `android-port/stage-01-audit`

## Scope

This is an audit-only stage. It does not change the existing desktop renderer or claim Android support. The first implementation target is Fabric + Minecraft 1.20.1 on Android, with Snapdragon 860 / Adreno 640 as the first device profile.

## Confirmed blockers

1. **Single modern Minecraft target in the current build.** `gradle.properties` currently selects Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.160.5+26.3, Sodium 0.9.3-beta.1, and Java release 25. A 1.20.1 build needs a separate version-specific Gradle configuration and compatible dependency set; simply changing one version string is unsafe.
2. **Desktop OpenGL API assumptions.** `Capabilities.java` imports LWJGL OpenGL 4.5 entry points and queries desktop-only capabilities. `GlPersistentMappedBuffer.java` uses named buffer storage and persistent mapping. These APIs cannot be assumed on GLES 3.x.
3. **Compute and SSBO assumptions.** Capability probing compiles compute shaders and reads SSBO limits. The main render system also imports shader-storage buffer constants. These features must be capability-gated and paired with a real fallback path; returning false from a probe alone will not make the current renderer work.
4. **Renderer backend assumption.** `VoxyRenderSystem.getRenderBackendFactory()` currently selects `MDICSectionRenderer.FACTORY` unconditionally. That backend requires indirect multi-draw functionality and cannot be treated as a universal mobile backend.
5. **Desktop native dependencies.** `build.gradle` declares LWJGL native classifiers for Windows and Linux, with an optional Linux ARM64 path. It also packages RocksDB JNI and includes desktop-oriented SQLite JDBC handling. Android needs a dependency-by-dependency decision: remove, replace with a portable implementation, or provide a verified Android ABI build.
6. **Windows-only implementation present.** `GPUSelectorWindows2.java` directly imports Windows JNI/GDI APIs. It must not be loaded or referenced from Android initialization paths.

## Required architecture

- Keep Minecraft/Fabric integration version-specific.
- Keep world storage, LOD hierarchy, traversal, and geometry logic independent from renderer-specific GL calls where possible.
- Introduce a renderer capability profile and backend selection layer. Backend selection must be based on tested functions/extensions, not renderer-name guesses.
- Add a GLES-compatible backend as its own implementation. Do not route GLES through the current desktop MDIC backend.
- Keep desktop LWJGL code isolated so Android class loading does not resolve desktop-only classes.
- Treat Zink/Kopper as driver translation paths, not as proof that desktop OpenGL 4.5 entry points or all extensions are available.

## Implementation order after this audit

1. Extract a complete inventory of GL entry points, shader versions, native libraries, and OS-specific classes.
2. Define the first target build matrix for Fabric 1.20.1 and the required Java/Fabric/Sodium versions.
3. Add a clean Android source set/module boundary without modifying the existing desktop build.
4. Implement and test a conservative GLES backend, beginning with capability reporting and resource lifecycle.
5. Port the Voxy render pipeline incrementally, then add LOD streaming and settings integration.

## Acceptance criteria for Stage 01

- [x] Repository baseline and build configuration inspected.
- [x] Primary GL/native blockers identified from source.
- [x] First Android target and architecture boundaries documented.
- [ ] Build execution verified in an Android-capable environment (not available during this audit).

## Important limitation

This audit is not a working Android port and does not produce a playable JAR. Android support is only established after an Android-targeted build succeeds and the mod is tested in the intended Minecraft launcher and renderer.

# Fabric 1.20.1 Compatibility Audit — Stage 75

## Scope

This is a source/configuration audit only. It does not claim that the project builds for Minecraft 1.20.1, runs on Android, or has a GLES renderer.

## Current build target

The active `gradle.properties` selects:

- Minecraft: `26.3`
- Fabric Loader: `0.19.5`
- Fabric API: `0.160.5+26.3`
- Sodium: `mc26.3-0.9.3-beta.1-fabric`
- Lithium: `mc26.3-0.26.2-fabric`

The root `build.gradle` currently sets `targetJavaVersion = 25` and LWJGL `3.4.1`. Resource expansion derives the Minecraft and Sodium compatibility declarations in `fabric.mod.json` from those active properties. Consequently, changing only `minecraft_version` would leave the rest of the dependency/runtime profile inconsistent.

## 1.20.1 blockers

1. **No independent version profile exists.** There is no checked-in property set or Gradle task that selects a complete Minecraft 1.20.1 dependency matrix.
2. **Java target mismatch.** The active Java compile target is 25. Minecraft 1.20.1's runtime baseline is Java 17, so a 1.20.1 artifact must be compiled against Java 17-compatible source and dependencies. The whole source tree has not yet been audited for Java 17 language/API compatibility.
3. **Fabric/Sodium dependencies are pinned to 26.3.** Loader, Fabric API, Sodium, Lithium and optional integration versions need a coherent 1.20.1-compatible set. No versions are guessed in this audit.
4. **Desktop GL remains the only implemented backend.** `DesktopGlBackendProvider` requires a current desktop OpenGL 4.5 context. `DesktopGlBackend` uses OpenGL 4.5 DSA calls. No Android GLES provider or backend implementation is present.
5. **No Android runtime integration exists.** The repository has no Android Gradle module, EGL context bridge, launcher integration artifact, or Android device test in the current tree.

## Existing safeguards

- `GlRendererPath.selectImplemented()` only returns `DESKTOP_MDIC` when that renderer is implemented.
- GLES feature candidates are not treated as working renderer implementations.
- `VoxyClient` exits before legacy renderer initialization when no implemented path is available.
- Stage 74 fixed the four stale `STATIC_VAO` references; both CI workflows completed successfully on commit `426fe0d5`.

## Required next sequence

1. Add an explicit, independently selectable 1.20.1 Gradle/property profile without changing the default 26.3 build.
2. Pin a coherent Java 17, Fabric Loader, Fabric API, Sodium and optional dependency matrix for that profile.
3. Compile the complete source tree against the 1.20.1 mappings/API and resolve source compatibility errors.
4. Add version-specific mixin/access-widener compatibility checks.
5. Only after a clean 1.20.1 artifact exists, continue the Android GLES backend and renderer implementation.

## Verification status

- Default 26.3 Gradle build: passed in GitHub Actions on Stage 74 commit.
- 1.20.1 Gradle build: not run; profile does not exist.
- Java 17 full-source compatibility: not verified.
- Android launcher load: not tested.
- GLES rendering / LOD distance: not implemented or tested.

# Stage 55 — MobileGlues / EGL-GLES Integration Audit

## Scope

Audit the current backend discovery and initialization path for a renderer hosted by an Android Minecraft launcher (for example, MobileGlues). This stage is an audit and design decision, not an Android renderer implementation.

## Findings verified on `android-port/stage-01-audit`

### 1. Provider discovery is only a registration mechanism

`GlBackendProviders.discover()` loads optional implementations through Java `ServiceLoader`, then appends the desktop provider. `GlBackendProvider.createIfSupported()` is documented to inspect an already-current context and must not create or own that context.

This is a reasonable extension point for an adapter, but it does not itself discover EGL, attach LWJGL to GLES, or guarantee that the launcher's context is current.

### 2. Desktop fallback is LWJGL desktop OpenGL only

`DesktopGlBackendProvider` calls `org.lwjgl.opengl.GL.getCapabilities()` and requires `OpenGL45`. It catches the no-current-context `IllegalStateException`. This provider is not an Android GLES provider.

### 3. The current capability snapshot bypasses the backend abstraction

`Capabilities` imports and calls LWJGL desktop GL APIs directly, including `GL.getCapabilities()`, OpenGL 4.5 DSA calls, compute shader APIs, SSBO constants and NVX memory queries. Its constructor unconditionally compiles compute shaders and reads SSBO limits.

Therefore, even if a GLES provider is discovered and selected, `Capabilities.init()` is not currently safe for GLES 3.0/3.1. This is a concrete initialization blocker.

### 4. Voxy's startup gate excludes basic GLES raster

`VoxyClient.initVoxyClient()` currently evaluates support as:

```java
boolean systemSupported = Capabilities.require().compute
        && Capabilities.require().indirectParameters
        && !Capabilities.require().hasBrokenDepthSampler;
```

That gate rejects a GLES 3.0 raster backend by design. More importantly, simply weakening this boolean is unsafe: downstream renderer construction still assumes compute, SSBO, indirect drawing and desktop GL operations.

### 5. Backend interface is not yet sufficient for an honest GLES 3.0 implementation

The current `GlBackend` contract includes methods whose desktop semantics are not generally available in GLES 3.0, including texture views, texture image clearing, buffer clear/range operations, sync fences, persistent mapping/flush, indexed buffer binding, vertex-array DSA configuration, and memory barriers with desktop flag semantics.

A GLES adapter would need explicit capability/feature contracts and well-defined fallbacks. Unsupported operations must fail or route to CPU/basic-raster alternatives; silently emulating every operation is not valid.

## MobileGlues / launcher integration boundary

The safe boundary is:

1. The launcher owns EGL display, EGL context, surface lifecycle, and context-current transitions.
2. A Voxy platform adapter may inspect the already-current context on the render thread only.
3. The adapter must not call EGL context creation, make-current, swap, or destroy operations.
4. The adapter must query the actual GLES version/extensions through a supported GLES binding supplied by the Android host/module.
5. It must reject unsupported contexts cleanly and must not initialize desktop LWJGL `GLCapabilities` for an EGL/GLES context.
6. On context loss, Java-side wrappers are invalidated without issuing deletes against the lost context; after a replacement context is current, resources must be recreated.

This audit does not establish that MobileGlues exposes a stable public EGL/GLES API to this mod. That API and the launcher's classloader/native binding strategy must be confirmed before writing a concrete adapter. The same applies to Zink/Kopper/other renderers: they are not interchangeable assumptions and must be detected by the actual current API/version/extensions, not by launcher name alone.

## Required architecture before GLES code

- Introduce a backend-neutral capability snapshot obtained from the selected `GlBackend`, rather than constructing it from desktop `GL.getCapabilities()`.
- Separate renderer feature levels: desktop MDIC, GLES compute LOD (only when required GLES 3.1+ features exist), and basic GLES raster.
- Keep the existing MDIC pipeline behind its full feature gate.
- Implement a genuinely separate basic-raster path that does not instantiate compute/SSBO/indirect/Hi-Z resources.
- Split platform integration/build configuration only after the Android host API and Android module toolchain are known.

## Stage result

**Stage 55 is an integration audit only.** No Android EGL/GLES provider was added, no startup gate was weakened, and no claim is made that MobileGlues currently works. No Gradle build, unit test, Minecraft launch, or Android device test was run.

## Next stage

Stage 56 should refactor capability discovery behind the backend contract and add explicit feature-level capability data, preserving the current desktop path. This is a prerequisite for safely adding a GLES provider and renderer path.

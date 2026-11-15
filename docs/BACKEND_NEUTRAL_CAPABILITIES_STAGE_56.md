# Stage 56 — Backend-Neutral Embedded Capability Snapshot

## Changes

Updated `src/main/java/me/cortex/voxy/client/core/gl/Capabilities.java` so its constructor first obtains the active `GlBackend` and its `GlCapabilities` snapshot.

When the backend reports `embeddedProfile == true`, the constructor now:
- avoids calling `GL.getCapabilities()` and desktop-only LWJGL capability probes;
- copies compute, indirect-parameter, subgroup and shader-int64 feature flags from the backend-neutral snapshot;
- reports unsupported vendor-specific features conservatively as false;
- reports unavailable SSBO limit/alignment and GPU memory values as -1;
- does not run the desktop AMD depth-sampler compute probe.

The desktop branch remains on its existing LWJGL capability detection path.

## Important limits

- This change makes the legacy capability snapshot safe to construct for an embedded backend; it does not implement GLES API calls or prove a GLES provider exists.
- The legacy startup gate in `VoxyClient` still requires compute and indirect parameters. A GLES 3.0 basic-raster backend therefore remains disabled, intentionally.
- Existing downstream renderers still contain desktop GL calls and assumptions. Do not weaken the startup gate until a separate basic-raster renderer is implemented and all its resource/shader paths are routed through the backend.
- `sparseBuffer` is conservatively false for embedded contexts because the current backend capability structure does not expose a distinct sparse-buffer feature.
- Vendor identity and GPU-memory queries are not inferred from renderer names; embedded values remain unavailable until the backend contract explicitly exposes reliable values.

## Verification

The updated source was fetched back from `android-port/stage-01-audit` after the commits. No Gradle compilation, unit test, shader compilation, Minecraft launch, or Android device test was run. Java/API compatibility therefore remains unverified.

## Next stage

Stage 57 should separate the startup feature-level decision from the legacy desktop-only capability fields, while keeping the current MDIC path gated exactly as before. A basic-raster path must not instantiate MDIC/compute/SSBO resources.

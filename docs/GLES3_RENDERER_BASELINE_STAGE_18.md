# GLES 3.x Baseline and Renderer Architecture Audit — Stage 18

## Source findings

The current source does not yet contain a GLES section renderer.

The existing `MDICSectionRenderer` is not a suitable Android fallback because
its path uses compute shaders, SSBOs, image load/store, indirect command
generation, and indirect-parameter drawing.

The existing `BasicSectionGeometryData` is also not a portable GLES geometry
implementation: it allocates large buffers through desktop-oriented wrappers
and contains an ARB sparse-buffer commitment path.

The common `AbstractSectionRenderer` interface does provide a useful seam:
a new renderer can implement opaque rendering, draw-call building, temporal
rendering, translucent rendering, viewport creation, and resource cleanup.
However, this interface alone does not remove desktop GL calls in the
surrounding render pipeline, geometry upload, Hi-Z traversal, or shader code.

## Stage 18 code change

Added `GlCapabilities.supportsGles3BasicRasterPath()`.

It returns true only when the context is identified as an embedded profile
and reports GLES version 3.0 or newer. This is an API-generation gate for a
future conventional raster path; it is not a claim that all features or
resource paths are supported.

The method deliberately does not require compute shaders, SSBO, image
load/store, or indirect parameters. A future basic renderer is expected to
generate draw calls on the CPU and use GLES-compatible vertex/index buffers
and GLSL ES shaders.

## Proposed Android renderer direction

A minimal viable Android renderer should be a separate implementation, not a
modified MDIC shader pipeline:

1. CPU-side visibility/draw-list generation initially; no compute shader.
2. GLES 3.x vertex/index buffers and conventional draw calls.
3. GLSL ES 3.00-compatible vertex and fragment shaders.
4. No sparse buffer commitment, persistent mapping, texture views, or
   desktop DSA assumptions in its first version.
5. Bounded geometry uploads and explicit memory budgets for mobile GPUs.
6. A dedicated GLES-compatible viewport/resource path where the current
   Hi-Z and pipeline code cannot be reused safely.

## Important blockers still present

- `VoxyRenderSystem` currently constructs `BasicSectionGeometryData`
  directly and selects only MDIC.
- `AbstractRenderPipeline` and its implementations still contain direct
  LWJGL/OpenGL calls and desktop-specific operations.
- `Capabilities.INSTANCE` initializes using desktop OpenGL 4.5 APIs and
  compute-shader probes.
- The current backend interface does not yet abstract all operations required
  by the complete render pipeline.

## Verification and scope

- Inspected `AbstractSectionRenderer`, `BasicSectionGeometryData`,
  `IGeometryData`, `AbstractRenderPipeline`, `RenderPipelineFactory`,
  `MDICSectionRenderer`, and `Capabilities`.
- Added only the GLES 3.x basic-raster capability predicate.
- No GLES renderer, CPU draw-list implementation, or Android backend was
  created in this stage.
- No Gradle compilation, automated tests, or Minecraft Android runtime tests
  were run.

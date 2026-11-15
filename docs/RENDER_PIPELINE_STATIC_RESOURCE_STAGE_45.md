# Stage 45 — render-pipeline static resource ownership

## Scope

Stage 44 identified two resources in `AbstractRenderPipeline` that were created at class initialization and were not associated with a renderer instance or graphics-context generation:

- A shared GL depth sampler created by `glGenSamplers()`.
- A process-lifetime native scratch allocation used for matrix uniform uploads.

## Changes

### Depth sampler

- Replaced the class-static sampler handle with an instance-owned `depthSampler`.
- Captures the active `GlBackendRuntime` context generation when constructing the pipeline.
- Creates/configures the sampler as part of pipeline construction, after the depth/stencil setup shader is constructed.
- If sampler creation/configuration fails, attempts to release partially-created pipeline resources and preserves cleanup errors as suppressed exceptions.
- Deletes the sampler from `free0()` only when the original context generation is still current.
- Sampler deletion is in a `finally` path so it is attempted even if another pipeline resource cleanup throws.

### Matrix upload scratch memory

- Removed the static `MemoryUtil.nmemAlloc` pointer.
- `transformBlitDepth()` now uses a 16-float `MemoryStack` buffer for the two matrix uniform uploads.
- The temporary memory is scoped to the method call and released automatically; there is no longer a class-lifetime native allocation to leak across reloads.

## Verification

Fetched the updated `AbstractRenderPipeline.java` from branch `android-port/stage-01-audit` after the commits. Confirmed:

- No static `DEPTH_SAMPLER` field remains.
- No static `SCRATCH` pointer remains.
- Matrix uploads use `MemoryStack` and the FloatBuffer `glUniformMatrix4fv` overload.
- Sampler deletion checks the captured context generation.
- No `MemoryUtil` or raw `nglUniformMatrix4fv` reference remains in this file.

## Remaining limitations

- This does not migrate sampler operations into `GlBackend`; the implementation still uses desktop LWJGL OpenGL calls.
- It does not make the pipeline GLES-compatible.
- Other static GL resources elsewhere in the renderer have not been audited by this stage.
- No Gradle build, automated test, Minecraft launch, or Android-device test was run.

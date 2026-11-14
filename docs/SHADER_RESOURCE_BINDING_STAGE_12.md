# Stage 12 — Shader resource binding backend

## Scope

This stage moves the resource-binding calls used by `AutoBindingShader`
behind `GlBackend`:

- indexed UBO/SSBO binding by base;
- indexed UBO/SSBO binding by byte range;
- texture-unit binding;
- sampler binding.

The desktop backend implements these operations with core OpenGL calls.
`AutoBindingShader` no longer calls the LWJGL binding functions directly.
The OpenGL enum values for uniform and shader-storage buffer targets remain
API constants, not backend calls.

## Range correctness

The existing automatic binding representation uses a size of `-1` to mean
"the remainder of the buffer". When a non-zero offset is supplied, this
stage resolves that sentinel to `buffer.size() - offset` and validates the
result before issuing a range bind. A zero-offset whole-buffer binding
continues to use `bindBufferBase`.

## Still outstanding

- No GLES/Android backend exists yet; the new methods currently have only
  the desktop OpenGL implementation.
- Uniform location lookup and uniform value setters are not migrated.
- Texture creation/storage, framebuffer operations, and buffer allocation
  still contain desktop-only assumptions already documented in earlier
  stages.
- Shader source compatibility is unchanged: Voxy still loads GLSL 460 core,
  and compute/SSBO/image/subgroup/mesh/task features need capability-aware
  variants or algorithmic fallbacks.
- Binding state caching/multibind optimization is not implemented.

## Validation status

The source changes are committed to the feature branch. No Gradle build,
automated test, desktop runtime test, or Android/Minecraft runtime test was
run. This stage does not establish GLES compatibility.

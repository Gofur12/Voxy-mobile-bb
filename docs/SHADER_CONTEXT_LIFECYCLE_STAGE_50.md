# Shader Context Lifecycle — Stage 50

## Audit findings

- `VoxySamplers` does not allocate static GL sampler handles. It registers Iris dynamic samplers whose suppliers resolve the current Voxy pipeline depth texture when queried.
- `Shader` owned a GL program but did not record the context generation that created it. Its old `bind()` and `free()` used whichever backend happened to be current.
- `AutoBindingShader` checked only whether referenced buffers/textures were freed, not whether they belonged to the active GL context generation.

## Changes

Files:
- `src/main/java/me/cortex/voxy/client/core/gl/shader/Shader.java`
- `src/main/java/me/cortex/voxy/client/core/gl/shader/AutoBindingShader.java`

`Shader` now:
- records `GlBackendRuntime.getContextGeneration()` when its program wrapper is constructed;
- validates that the shader is not freed and its originating context is active before binding or naming;
- deletes its GL program only when its originating context generation is still current;
- still marks the Java wrapper freed when the context has already been lost, without issuing an invalid GL deletion.

`AutoBindingShader` now:
- validates its own context before naming;
- validates buffer and texture bindings against their own context generations before binding them.

## Scope

This is resource-lifecycle hardening only. It does not translate desktop GLSL, add GLES shader compilation, or make the MDIC renderer compatible with Android. Iris sampler registration remains delegated to Iris.

## Verification

Updated files were fetched back from the working branch and inspected for the generation guard and resource-context checks.

No Gradle compilation, automated test, Minecraft launch, or Android device test was run.

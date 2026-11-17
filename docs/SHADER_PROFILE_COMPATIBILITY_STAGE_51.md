# Shader Profile Compatibility — Stage 51

## Findings

The current shader path had two concrete compatibility problems:

1. `ShaderLoader.parse()` discarded each root shader asset's `#version` line and prepended `#version 460 core` unconditionally. This silently upgraded assets that declare GLSL 450 to GLSL 460.
2. `Shader.Builder` sent shader source to the backend without first checking whether the source's requested GLSL profile/version matched the active backend context.

The shader assets demonstrate that a simple GLSL ES version substitution is not a valid port:
- Compute stages are used by Hi-Z reduction, command generation, SSAO, prefix sum, scatter, and buffer copy.
- The Hi-Z shader requires subgroup arithmetic/basic/clustered operations.
- Compute pipelines use SSBOs and image load/store.
- Some desktop raster shaders use `GL_ARB_gpu_shader_int64`, NV fragment barycentrics, or other desktop/vendor extensions.
- The current MDIC path depends on compute and indirect-draw features; it is not a GLES 3.0 renderer.

## Changes

Files:
- `src/main/java/me/cortex/voxy/client/core/gl/shader/ShaderLoader.java`
- `src/main/java/me/cortex/voxy/client/core/gl/shader/Shader.java`

Changes:
- The shader loader now preserves the root asset's own `#version` directive. Imported files continue to have their version directives stripped, so the root shader controls the compilation profile.
- If a root shader has no version directive, the loader retains the prior `#version 460 core` fallback.
- Before compiling a stage, `Shader.Builder` validates that the source has a parseable GLSL version directive.
- The validator checks desktop-vs-embedded profile agreement and checks the requested GLSL major/minor version against the active backend's reported GL major/minor version.
- Unsupported profile/version combinations now fail with a targeted Java exception before driver compilation.

## Android implications

This is an early guard and source-correctness improvement, not an Android renderer:
- It does not translate desktop GLSL to GLSL ES.
- It does not rewrite SSBO/image/compute/subgroup code into GLES-compatible alternatives.
- It does not add a GLES backend or raster fallback.
- A future GLES backend must report truthful embedded-profile/version capabilities and must select a distinct shader set or a genuinely compatible fallback path.

## Verification

The changed source files were fetched back from the working branch and inspected. Shader assets were inspected to confirm the listed feature families.

No Gradle compilation, shader-driver compilation, automated tests, Minecraft launch, or Android device test was run.

# Uniform Call-site Audit — Stage 15

## Purpose

Stage 15 checks whether the uniform API should be expanded after the Stage 13–14 migrations. It deliberately does not add unused setters just to increase the backend interface.

## Inspected source files

- `core/gl/backend/GlBackend.java`
- `core/gl/backend/desktop/DesktopGlBackend.java`
- `core/rendering/util/HiZBuffer.java`
- `core/rendering/util/HiZBuffer2.java`
- `core/gl/shader/Shader.java`
- `core/gl/shader/AutoBindingShader.java`
- `client/iris/VoxyUniforms.java`

## Findings

The inspected direct OpenGL uniform setter call sites in Voxy's in-repository renderer code are:

| Source | Operation | Backend status |
|---|---|---|
| `HiZBuffer.java` | integer uniform | Routed through `GlBackend.setUniform1i` |
| `HiZBuffer2.java` | integer uniform | Routed through `GlBackend.setUniform1i` |
| `HiZBuffer2.java` | vec2 float uniform | Routed through `GlBackend.setUniform2f` |

The desktop backend implements those methods with `GL20C.glUniform1i` and `GL20C.glUniform2f`.

`AutoBindingShader` binds UBO/SSBO ranges and texture/sampler resources through the backend; these are resource-binding operations, not uniform-value setters. `VoxyUniforms` is an Iris Shader API integration and does not expose direct LWJGL uniform setter calls in the inspected source.

No additional float, vector, matrix, or uniform-block setter was added because no verified in-repository direct setter call site requiring it was found in this targeted audit. Adding speculative API surface would not advance Android compatibility.

## Important scope limitation

This is a targeted audit of the listed integration and rendering files, not a claim that every Java source file in the repository has been exhaustively searched. A repository-wide call-site scan and compilation remain outstanding.

## Next engineering priority

The main Android blocker is no longer a missing basic uniform setter. The larger incompatibilities remain:

- GLSL 4.50/4.60 and compute shader dependence
- SSBO and image load/store assumptions
- OpenGL 4.5 direct-state-access calls outside migrated wrappers
- indirect draw / ARB indirect parameters and synchronization behavior

The next implementation work should address a real GLES capability boundary and a minimal render path, rather than add unused uniform methods.

## Verification

- Source files fetched from branch `android-port/stage-01-audit`.
- No Gradle build, automated test, desktop runtime test, or Android/Minecraft runtime test was run for this audit.

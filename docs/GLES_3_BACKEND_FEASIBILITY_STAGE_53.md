# GLES 3.0 Backend Feasibility Audit — Stage 53

## Result

Stage 53 is an implementation-boundary audit, not a GLES backend implementation. The current repository cannot yet produce an Android GLES backend safely: its only Gradle target is a Fabric/Loom desktop mod, its Java compile target is 25, its Minecraft dependency is 26.3, and the GL adapter is built around LWJGL desktop OpenGL. No Android source set, Android SDK/plugin, EGL context owner, or GLES binding dependency is configured in this project.

Do not interpret `GLES_BASIC_RASTER_CANDIDATE` as a functioning renderer. It currently means only that the capability snapshot reports an embedded GL version of at least 3.0.

## Verified source boundary

- `GlBackend` is a backend-neutral interface, but its method set includes desktop-style operations that GLES 3.0 does not expose directly.
- `DesktopGlBackendProvider` calls `org.lwjgl.opengl.GL.getCapabilities()` and requires desktop OpenGL 4.5.
- `DesktopGlBackend` uses DSA calls such as named buffer storage, named framebuffer attachments, texture storage, texture-unit binding, and vertex-array DSA. These cannot be forwarded as-is to GLES 3.0.
- `GlTexture`, `GlBuffer`, and `GlFramebuffer` still import desktop OpenGL constants directly. A backend adapter alone is not enough to make all resource wrappers platform-neutral.
- `ShaderLoader` defaults a missing root version directive to `#version 460 core`. The current shader assets also contain desktop GLSL and advanced features; preserving the version line does not translate shader language or remove unsupported operations.
- The existing renderer path selection is only a capability selector. The current LOD pipeline still depends on compute/SSBO/image-load-store/indirect draw paths and direct desktop GL calls.

## GLES 3.0 operation mapping

| Backend operation | GLES 3.0 feasibility | Required implementation strategy |
|---|---|---|
| Shader/program compile/link | Core | GLES shader enums and GLSL ES source; do not reuse desktop GLSL unchanged |
| Mutable buffer create/upload | Core | Bind target then `glBufferData` / `glBufferSubData`; storage flags must be zero |
| Buffer clear / clear range | No core equivalent | CPU staging upload, or a separately validated extension path |
| Buffer copy | Core | Bind copy-read/write targets and call `glCopyBufferSubData` |
| Map/unmap range | Core | Bind target and use GLES map-range calls; validate access flags |
| Fence sync | Core | GLES sync APIs, with context/thread ownership checks |
| Texture-unit binding | Core, different API | `glActiveTexture` + `glBindTexture`, restoring active unit as needed |
| Sampler objects | Core | GLES sampler API |
| Texture storage 2D | Core | Bind texture and call `glTexStorage2D` |
| Texture views | Extension-dependent | Require and verify `OES_texture_view`; otherwise reject or redesign callers |
| Clear texture image | No core equivalent | Explicit fallback (e.g. framebuffer clear) with format/attachment validation |
| Framebuffer attachments/status | Core | Bind framebuffer and use GLES framebuffer attachment/status APIs |
| Renderbuffer storage | Core | Bind renderbuffer and use GLES storage API |
| VAO creation/bind | Core | GLES VAO API |
| VAO vertex format | No core DSA equivalent | Bind VAO and array/element buffers, then configure attribute pointers |
| Memory barrier | GLES 3.0 limited | Only use barriers supported by the actual API version/extensions; GLES 3.0 has no compute shader path |
| Finish | Core | GLES finish |

## Required split before implementation

1. Keep the existing Fabric desktop module isolated; do not add Android imports or Android plugin configuration to its production source set.
2. Introduce a separately buildable Android adapter module/source set with an explicit Android SDK and GLES binding strategy, after the target launcher/runtime integration is chosen.
3. Define a narrow GLES 3.0 backend implementation that rejects unsupported operations instead of silently emulating them incorrectly.
4. Move GL constants out of platform-specific imports in shared wrappers, or define a verified cross-API constant contract.
5. Replace desktop DSA wrapper assumptions with bind-to-edit backend methods where semantics can be preserved.
6. Build a separate GLSL ES 3.00 shader set and a basic raster renderer. Do not feed the existing GLSL 450/460 assets through a text substitution pass.
7. Add unit tests for capability decisions and backend call mapping, then compile and run on a real GLES 3.0 context before enabling the renderer path.

## Explicit blockers for long-distance LOD

GLES 3.0 alone does not provide compute shaders, shader storage buffers, or image load/store. Therefore the current GPU-driven Voxy LOD pipeline cannot be ported as a direct GLES 3.0 backend. A useful Android path needs either a deliberately redesigned CPU-driven/basic raster LOD renderer for GLES 3.0, or a separate GLES 3.1+ compute implementation with its own shader and feature validation.

## Stage 53 verification

Source files inspected on branch `android-port/stage-01-audit`: `GlCapabilities`, `GlBackend`, `GlBackendProvider`, `GlBackendRuntime`, `DesktopGlBackendProvider`, `DesktopGlBackend`, `ShaderLoader`, `Shader`, `GlBuffer`, `GlTexture`, `GlFramebuffer`, `build.gradle`, and `gradle.properties`.

No Java source implementation was added because the project has no Android/GLES build target or current-context integration to compile against. No Gradle build, unit test, Minecraft launch, GLES context test, or Android device test was run. The next valid implementation step is to establish the Android module and runtime/context boundary, not to add a fake GLES adapter to the desktop Fabric module.

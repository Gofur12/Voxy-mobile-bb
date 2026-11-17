# Stage 62 — GLES framebuffer texture-clear design audit

## Scope

Audit the typed texture-clear contract introduced in Stage 61 and determine the safe GLES 3.x implementation boundary. This stage is a design/audit checkpoint; it does not claim to add a GLES backend.

## Current implementation

- `GlTexture.zero()` maps each supported internal format to a semantic `GlTextureClearType` and clears every mip level through one context-validated `GlBackend`.
- `GlBackend.clearTextureImage(handle, level, clearType)` is an abstract backend operation. Its contract can remain API-neutral: each backend is responsible for clearing the requested texture level to zero.
- `DesktopGlBackend` currently implements this with desktop `glClearTexImage` (LWJGL `GL45C`). That is valid for its OpenGL 4.5 requirement, but is not an implementation for GLES.
- The current framebuffer methods use desktop DSA entry points (`glCreateFramebuffers`, `glNamedFramebufferTexture`, `glCheckNamedFramebufferStatus`). A future GLES backend cannot reuse those methods as-is.

## Clear-type mapping

| Texture internal format | Semantic clear type | GLES clear operation candidate |
|---|---|---|
| `GL_RGBA8` | `RGBA_FLOAT` | `glClearBufferfv(GL_COLOR, ...)` |
| `GL_RG16F` | `RG_FLOAT` | `glClearBufferfv(GL_COLOR, ...)`, only when renderable |
| `GL_R32F` | `RED_FLOAT` | `glClearBufferfv(GL_COLOR, ...)`, only when renderable |
| `GL_R32UI` | `RED_UNSIGNED_INT` | `glClearBufferuiv(GL_COLOR, ...)` |
| `GL_DEPTH_COMPONENT24`, `GL_DEPTH_COMPONENT32F`, `GL_DEPTH_COMPONENT32` | `DEPTH_FLOAT` | `glClearBufferfv(GL_DEPTH, ...)` |
| `GL_DEPTH24_STENCIL8` | `DEPTH_STENCIL_UNSIGNED_INT_24_8` | `glClearBufferfi(GL_DEPTH_STENCIL, ...)` |
| `GL_DEPTH32F_STENCIL8` | `DEPTH_STENCIL_FLOAT32_UNSIGNED_INT_24_8` | `glClearBufferfi(GL_DEPTH_STENCIL, ...)`, only when format is supported and attachable |

The table describes candidate GLES clear commands, not a promise that every listed internal format is framebuffer-renderable on every GLES 3.x device. In particular, floating-point color attachment support must be checked against the actual context version/extensions and framebuffer completeness.

## Required GLES backend behavior

A GLES implementation of `clearTextureImage` should:

1. Accept only supported 2D texture handles and valid mip levels.
2. Attach the requested mip level to a backend-owned temporary framebuffer using GLES bind-based framebuffer APIs.
3. Select the correct color, depth, or depth-stencil attachment and the matching typed clear operation.
4. Check framebuffer completeness before clearing; report an explicit unsupported/incomplete-format error rather than silently skipping the clear.
5. Preserve and restore the caller's draw/read framebuffer bindings and every state it changes, including relevant scissor and write-mask state. The implementation must not leave Voxy's or the launcher's GL state modified.
6. Reuse or safely dispose of its private framebuffer within the owning context generation. It must never create, make current, or destroy the launcher's EGL context.

## Why no FBO implementation was added in this stage

The current backend interface does not expose a complete portable framebuffer state-save/restore contract. Adding a partial FBO clear path at the wrapper layer would either leak GL state or introduce direct LWJGL calls outside the backend. The current `GlFramebuffer` wrapper also assumes backend DSA operations and is not a suitable GLES implementation.

The safe next implementation step is to add a backend-owned, state-preserving texture-clear implementation and the minimal GLES framebuffer operations it needs together, then validate each clear type on a real GLES context. Until that exists, the desktop backend remains the only implementation and keeps using `glClearTexImage`.

## Status

- Typed semantic clear contract: present (Stage 61).
- Desktop OpenGL 4.5 clear: present.
- GLES FBO clear: not implemented.
- Android EGL / MobileGlues provider: not implemented.
- Android artifact, Gradle build, automated tests, shader tests, Minecraft launch, and device tests: not run.

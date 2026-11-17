# Stage 60 — Texture Clear Fallback Audit

## Scope

Audit the existing `GlBackend.clearTextureImage` contract and every clear operation exposed by the texture wrapper before choosing an embedded/GLES fallback. This stage is an audit only; it does not add a GLES clear implementation.

## Verified source behavior

- `GlBackend.clearTextureImage(int handle, int level, int format, int type, long dataAddress)` exposes a desktop-style texture-image clear operation.
- `DesktopGlBackend.clearTextureImage` delegates directly to `GL45C.nglClearTexImage`, an OpenGL 4.5 entry point unavailable in GLES 3.x core.
- The wrapper's `GlTexture.zero()` is the concrete caller found in the audited source. It iterates over every allocated mip level and calls the backend with a null/zero data address, intending to clear the complete level to zero.
- `GlTexture.zero()` currently maps internal formats to transfer format/type pairs as follows:

| Internal format | Transfer format | Transfer type |
| --- | --- | --- |
| `GL_R32UI` | `GL_RED_INTEGER` | `GL_UNSIGNED_INT` |
| `GL_RGBA8` | `GL_RGBA` | `GL_INT` |
| `GL_R32F` | `GL_RED` | `GL_FLOAT` |
| `GL_RG16F` | `GL_RG` | `GL_FLOAT` |
| `GL_DEPTH_COMPONENT24` | `GL_DEPTH_COMPONENT` | `GL_FLOAT` |
| `GL_DEPTH_COMPONENT32F` | `GL_DEPTH_COMPONENT` | `GL_FLOAT` |
| `GL_DEPTH_COMPONENT32` | `GL_DEPTH_COMPONENT` | `GL_FLOAT` |
| `GL_DEPTH24_STENCIL8` | `GL_DEPTH_STENCIL` | `GL_UNSIGNED_INT_24_8` |
| `GL_DEPTH32F_STENCIL8` | `GL_DEPTH_STENCIL` | `GL_FLOAT_32_UNSIGNED_INT_24_8_REV` |

## Why a generic FBO clear is not added yet

A GLES-compatible framebuffer clear is a plausible fallback for some 2D texture formats, but it is not a drop-in replacement for the current method:

1. The backend method receives transfer format/type, not the texture's internal format or an explicit attachment class. A correct implementation must distinguish normalized/floating color, unsigned-integer color, depth, and depth-stencil attachments.
2. GLES framebuffer renderability is format- and implementation-dependent. The audited format list includes formats that cannot be assumed renderable on every GLES 3.x implementation.
3. A framebuffer clear must preserve and correctly manage framebuffer bindings, draw/read-buffer state, scissor state, color/depth/stencil write masks, and other relevant GL state. It must also handle incomplete FBOs without corrupting the caller's state.
4. `GL_DEPTH_COMPONENT32` and the packed depth-stencil transfer type require separate extension/version validation; they cannot be treated as universally available GLES 3.0 formats.
5. A future Android backend still needs to prove its actual context/version/extensions and framebuffer format support before selecting this fallback.

Using a zero-filled CPU upload for every mip level is another possible fallback, but it requires format-aware byte sizing, pixel-store alignment handling, native staging memory, and potentially large allocations. It is not silently substituted here.

## Decision

Keep the existing desktop implementation unchanged. Do not claim `clearTextureImage` is GLES-portable, and do not advertise a GLES renderer as implemented. The next implementation step should make the clear operation explicitly typed by internal format/clear class (or expose a capability-gated clear path), then add a GLES implementation with state restoration and tested per-format support.

## Validation status

- Source files inspected: `GlBackend.java`, `DesktopGlBackend.java`, and `GlTexture.java`.
- The repository tree for the working branch was retrieved without truncation.
- The wrapper's `zero()` call and its supported format mapping were inspected directly.
- GitHub code search returned no result for `GlTexture zero()`; code search is default-branch scoped, so this is not treated as proof that there are no additional call sites on the working branch.
- No source code was changed in this stage.
- No Gradle build, automated test, shader compile, Minecraft launch, GitHub Actions run, or Android-device test was performed.

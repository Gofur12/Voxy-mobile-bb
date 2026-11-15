# Stage 64 — State-preserving texture-clear contract

## Changes

The `GlBackend.clearTextureImage` contract now requires implementations to:

- Clear the requested 2D mip level to numeric zero using the supplied semantic clear type.
- Preserve caller-visible graphics state changed by the operation.
- If a temporary framebuffer is used, restore draw/read framebuffer bindings, draw-buffer selection, scissor enable/box, and relevant color/depth/stencil write masks.
- Leave the current program, viewport, and texture bindings unchanged.
- Fail explicitly for unsupported formats or incomplete temporary framebuffers.
- Keep temporary framebuffer resources owned by the active backend/context generation.

The desktop implementation additionally rejects non-positive texture handles and negative mip levels before issuing the desktop clear call.

## Architecture decision

The framebuffer implementation remains encapsulated inside the backend operation. The generic `GlTexture` wrapper does not bind raw framebuffers and does not manipulate global GL state. This avoids leaking GLES- or desktop-specific calls into resource wrappers and gives a future GLES backend room to use bind-based FBO operations safely.

## Status and limitations

- Desktop clear still uses desktop OpenGL 4.5 `glClearTexImage`.
- No GLES FBO clear implementation has been added.
- The contract does not establish that every internal format is renderable on a target GLES device; the GLES backend must verify format support and framebuffer completeness.
- No Android module, EGL integration, MobileGlues provider, Gradle compile, automated test, Minecraft launch, or device test has been completed.

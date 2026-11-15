# Portable 2D Texture Upload — Stage 58

## Source audit

The existing texture wrapper already routes allocation and clear operations through `GlBackend`, but the backend contract had no client-memory sub-image upload. A GLES 3.x adapter can implement a 2D sub-image upload with the bind-based `glTexSubImage2D` API; it should not need desktop OpenGL 4.5 DSA.

The branch tree was checked for Java classes implementing the backend: only the built-in `DesktopGlBackend` currently implements `GlBackend`. Optional future service providers will need to implement the new method.

## Changes

- Added `GlBackend.uploadTexture2D(...)` as a backend-neutral 2D texture sub-image operation.
- Implemented it in `DesktopGlBackend` using bind-based `glTexSubImage2D`, saving and restoring the prior `GL_TEXTURE_2D` binding.
- Added `GlTexture.upload2D(...)` with checks for allocation, context generation, 2D target, mip-level range, non-empty upload size, non-null native address, and mip bounds.
- Native pixel memory remains caller-owned and must stay valid during the call.

## Why this helps Android

The API shape maps to GLES 3.x's ordinary texture upload model and avoids requiring `glTextureSubImage2D` DSA. It is a portable primitive, not an Android backend. Texture allocation, texture clear fallback, format compatibility, pixel-store alignment, and the rest of the renderer still need dedicated GLES implementation and testing.

## Validation limits

The branch tree showed only the desktop backend implementation at this stage, and that implementation was updated. Source files were fetched back after commits. No Gradle compile, automated tests, shader compilation, Minecraft launch, GitHub Actions run, or Android-device test was performed.

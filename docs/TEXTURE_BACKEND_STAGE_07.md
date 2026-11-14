# Stage 07 — Texture operations through backend

## Changes

- Extended `GlBackend` with generic texture creation, texture-view creation, and texture-image clearing.
- Implemented those operations in `DesktopGlBackend` using desktop OpenGL calls.
- Migrated `GlTexture` creation, 2D immutable storage allocation, texture-view creation, image clearing, and deletion through `GlBackendRuntime`.
- Kept texture format metadata, mip-level size accounting, allocation checks, public constructors, and the existing view semantics.

## Important implementation detail

- OpenGL texture views require a fresh unused texture name. The desktop backend obtains that name with `glGenTextures`, calls `glTextureView`, and deletes the name if view creation throws.
- `GlTexture` wraps the returned view handle directly; it does not allocate an extra texture name that could leak.

## Limitations and verification

- This remains a desktop OpenGL adapter; no GLES/MobileGlues/Zink implementation exists yet.
- Other wrappers (framebuffer, vertex array, mapped buffers, shader/program, and the MDIC renderer) still use direct LWJGL calls.
- Gradle compilation and in-game tests have not been run. The changes are source-level only and must be validated by the next compile/test pass.
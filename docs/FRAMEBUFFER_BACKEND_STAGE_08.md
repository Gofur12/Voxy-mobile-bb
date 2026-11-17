# Stage 08 — Framebuffer and renderbuffer backend migration

## Changes

- Extended `GlBackend` with framebuffer texture/renderbuffer attachment, draw-buffer configuration, status retrieval, and renderbuffer lifecycle operations.
- Implemented the new operations in `DesktopGlBackend` using OpenGL 4.5 direct-state-access calls.
- Migrated `GlFramebuffer` creation, attachment, draw-buffer setup, status verification, and deletion.
- Migrated `GlRenderBuffer` creation, storage allocation, and deletion.
- Kept the existing public wrapper methods and framebuffer verification behavior (including the numeric status code in the exception).

## Limitations

- The provider is still desktop OpenGL 4.5 only. The contract now exposes operations that an Android backend will need to implement, but does not itself provide GLES support.
- Framebuffer attachment for texture wrappers remains the existing 2D-style API; other texture targets and layered attachments are not covered.
- `GlDebug` object labels and remaining GL wrappers/render pipeline still use direct LWJGL.

## Verification

- Reviewed edits against the existing wrapper call sites and backend method signatures.
- No Gradle build, unit test, or Minecraft runtime test has been run. This stage is not yet a verified playable build.
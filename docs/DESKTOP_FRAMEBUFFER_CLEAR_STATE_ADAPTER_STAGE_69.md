# Stage 69 — Desktop Framebuffer Clear State Adapter

## Implemented

`DesktopGlBackend` now implements the Stage 68 state contract:

- Captures separate draw and read framebuffer bindings.
- Captures the draw-buffer selection for the currently bound draw framebuffer
  (one selection for the default framebuffer, the reported maximum for an FBO).
- Captures the read-buffer selection.
- Captures scissor enable and rectangle.
- Captures RGBA color write mask, depth write mask, and front/back stencil write masks.
- Restores draw/read framebuffer bindings before restoring their corresponding
  framebuffer-local buffer selections.
- Restores scissor state and the color/depth/stencil write masks.

The implementation uses the existing LWJGL desktop OpenGL adapter and a
`MemoryStack` scope for temporary query buffers. It does not create, own, or
destroy the Minecraft graphics context.

## Integration status

The Stage 68 `GlFramebufferClearStateScope` can now capture and restore state
when opened with a `DesktopGlBackend`. Existing backends that do not implement
the optional methods continue to fail explicitly through the interface defaults.

The temporary-FBO texture-clear fallback is not implemented or connected to
`clearTextureImage` in this stage. The desktop direct clear still uses
`glClearTexImage`.

## Validation limits

The source was committed and fetched back from the target branch. No local
Gradle compilation, automated tests, OpenGL runtime test, Minecraft launch, or
Android-device test was performed. In particular, the draw-buffer restoration
path still needs runtime validation against both the default framebuffer and
user-created FBOs before being used by a production clear fallback.

This is desktop OpenGL 4.5 code only. It is not a GLES 3 implementation and
does not make MobileGlues/Zink/Kopper support available.

# Texture and framebuffer resource audit — Stage 31

## Scope

Audited texture and renderbuffer wrapper lifecycle around backend allocation, validation, accounting, and cleanup. This stage does not replace desktop DSA calls inside the desktop backend and does not implement GLES texture/framebuffer operations.

## Changes

### GlTexture

- Texture storage state is now published only after backend allocation succeeds. A failed allocation leaves the wrapper unallocated and retryable rather than falsely marking it allocated.
- Validates positive width, height, and mip level count, and rejects mip counts beyond the full 2D mip chain.
- Calculates estimated memory with checked arithmetic and preflights the global estimate before allocating GPU storage.
- Frees the texture and updates object/memory counters in a `finally` block, even if backend deletion reports an error.
- Consolidates the estimated-size calculation so allocation and free accounting use the same format/dimension logic.

### GlRenderBuffer

- Validates positive dimensions before creating a GPU object.
- If storage allocation fails after object creation, attempts to delete the renderbuffer and preserves cleanup failures as suppressed exceptions.

## Portability observations

- `GlTexture.zero()` still calls backend `clearTextureImage`, which is not a GLES 3.x core operation and needs a backend fallback (for example, upload/clear through a supported path).
- `GlTexture.createView()` depends on texture-view support; GLES 3.x does not guarantee the desktop texture-view API used by the current desktop backend. The Android renderer must avoid this path or implement a compatible alternative.
- `GlFramebuffer` itself now routes operations through the backend contract, but the desktop implementation still needs its own bind-to-edit/GLES implementation. Framebuffer draw-buffer support and attachment formats must be capability-checked.
- Static LWJGL format constants remain in the wrappers as numeric API constants; removing LWJGL imports entirely is a separate cleanup.

## Validation status

- Source files and this document were committed to the target branch and fetched back.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.

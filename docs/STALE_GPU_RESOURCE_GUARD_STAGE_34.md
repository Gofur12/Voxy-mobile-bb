# Stage 34 — Stale GPU Resource Guards

## Goal

Prevent wrapper-owned GPU handles from being used against a replacement graphics context after the original context is lost or replaced.

## Implementation

- Added `GlBackendRuntime.isContextGenerationCurrent(long)` to distinguish a live resource generation from a lost/replaced context.
- Added `GlBackendRuntime.requireBackend(long, String)` to validate both the active backend/thread and the resource's captured generation before GPU operations.
- Captured the active context generation when constructing:
  - `GlBuffer`
  - `GlTexture`
  - `GlFramebuffer`
  - `GlRenderBuffer`
  - `GlVertexArray`
  - `GlPersistentMappedBuffer`
  - `GlFence`
- Routed wrapper GPU operations through generation-aware backend checks.
- Framebuffer attachment overloads taking `GlTexture` or `GlRenderBuffer` reject attachments from stale contexts.
- Freeing wrappers after context loss marks the Java wrapper freed and updates buffer/texture accounting, but skips GL deletion calls because the old context's handles are no longer valid.
- Persistent mapped-buffer address access is rejected after free or context loss. Freeing a stale mapped buffer does not attempt to unmap or delete its invalid old-context handle.
- Fence polling validates the context even when its signaled state was previously cached.

## Important limits

- `GlBackendRuntime.onContextLost()` still must be called by the platform/context lifecycle owner. This stage does not wire Minecraft, GLFW, EGL, or Android lifecycle callbacks.
- Renderer resources are not automatically recreated after context loss. Higher-level owners must discard old wrappers and rebuild their resources after a new backend initializes.
- Public raw integer handles and raw-address APIs used outside these wrappers cannot be universally protected. Callers that pass raw texture/buffer IDs remain responsible for ensuring those IDs belong to the active context.
- `GlVertexArray.STATIC_VAO` remains on the legacy static LWJGL path and is not generation-tracked.
- This does not add an Android EGL backend, GLES renderer, or GLES shader fallbacks.

## Verification status

- Source files were fetched back from the target branch after the edits.
- No Gradle compilation, automated test suite, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.
- Therefore this stage is a source-level lifecycle guard, not a verified runtime fix.

# Stage 06 — Backend runtime and first buffer migration

## Changes

- Added `GlBackendRuntime` to own backend selection after Minecraft has initialized its graphics renderer.
- Wired initialization into `VoxyClient.initVoxyClient()`. The existing `MixinRenderSystem` calls that method after `RenderSystem.initRenderer` returns.
- Registered the current desktop OpenGL 4.5 provider as the first provider.
- Migrated `GlBuffer` creation, clearing, pixel-store changes, and deletion to the backend contract.
- Preserved the public `GlBuffer` constructors, sparse-buffer flag handling, zero-on-create behavior, tracking counters, and scratch-memory fill behavior.

## Scope and limitations

- This is still a desktop OpenGL provider. No Android GLES, Zink/Kopper, or MobileGlues provider is implemented by this stage.
- Only `GlBuffer` is migrated. Other GL wrappers and renderers still call LWJGL directly.
- Backend selection is process-wide and has no context-recreation or hot-switch support yet. Minecraft continues to own the graphics context.
- The repository baseline remains its configured Minecraft 26.3 / Java 25 line, not the requested Fabric 1.20.1 target.
- The lifecycle point is based on the existing mixin injection after `RenderSystem.initRenderer`; actual in-game behavior has not been tested.

## Verification

- Source edits were checked against the current `GlBackend` interface and `DesktopGlBackend` implementation.
- Gradle compilation, automated tests, and in-game testing have not been run.
- This is not a working Android build or a playable JAR.
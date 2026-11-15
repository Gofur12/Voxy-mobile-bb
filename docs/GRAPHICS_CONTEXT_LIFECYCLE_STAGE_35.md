# Stage 35 — Minecraft Graphics Shutdown Lifecycle

## Goal

Connect Voxy's existing backend shutdown API to Minecraft's normal client shutdown while the OpenGL context is still owned by Minecraft.

## Source findings

- `VoxyClient.initVoxyClient()` selects the backend after `RenderSystem.initRenderer` returns.
- `ClientSessionEvents.sessionEnd()` shuts down the active world/session instance on disconnect, but it is not a graphics-context lifecycle event.
- `GlBackendRuntime.shutdown()` already enforces that backend close occurs on the context initialization thread and clears runtime ownership afterward.
- Before this stage, Minecraft client shutdown had no explicit Voxy graphics-backend shutdown hook.

## Implementation

- Added `VoxyClient.shutdownGraphicsResources()`.
  - Calls `VoxyCommon.shutdownInstance()` first so the active world renderer releases its resources while the context is still current.
  - Then calls `GlBackendRuntime.shutdown()` if a backend is still initialized.
  - Logs shutdown failures instead of silently pretending cleanup succeeded.
- Added a `Minecraft.close` HEAD injection in `MixinMinecraft`.
  - Ends an active Voxy client session first.
  - Runs Voxy graphics-resource shutdown before Minecraft proceeds with window/context teardown.
- Existing world-disconnect behavior remains unchanged.

## Important limits

- This wires normal Minecraft client shutdown, not arbitrary EGL/Android context-loss callbacks.
- Android launchers or platform integrations must still call an explicit lifecycle bridge before destroying/replacing their graphics context; this repository currently has no EGL surface/context owner to hook.
- If the context has already been lost unexpectedly, normal renderer shutdown may not be safe because parts of the renderer still use direct LWJGL calls outside the backend wrappers.
- This stage does not implement context restoration, automatic renderer reconstruction, GLES support, or an Android backend.
- The mixin target and shutdown ordering require a real target-version build/runtime check.

## Verification status

- Updated source files were fetched back from the target branch.
- No Gradle compilation, mixin application test, automated test suite, desktop shutdown test, Android context-loss test, or Minecraft gameplay test was run.

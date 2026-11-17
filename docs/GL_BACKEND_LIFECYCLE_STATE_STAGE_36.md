# Stage 36 — Explicit GL Backend Lifecycle State

## Goal

Make backend lifecycle transitions observable and reject initialization after final shutdown. This is a lifecycle contract and diagnostic improvement; it does not claim that Voxy's renderer can yet recover from Android EGL context loss.

## Changes

`GlBackendRuntime` now exposes:

- `UNINITIALIZED`: no backend has been selected yet.
- `ACTIVE`: a backend is bound to the current context/thread.
- `CONTEXT_LOST`: the previous context was reported lost; the old backend reference is discarded without GL deletion.
- `SHUTDOWN`: the runtime has completed or been told to perform final shutdown.

New API:

- `GlBackendRuntime.getState()` returns the current lifecycle state.
- Successful `initialize(...)` transitions to `ACTIVE`.
- `onContextLost()` transitions to `CONTEXT_LOST`.
- `shutdown()` transitions to `SHUTDOWN`, including when no backend is currently selected.
- Calling `initialize(...)` after `SHUTDOWN` throws instead of silently reviving a runtime whose owning Minecraft client is closing.

If provider selection fails after context loss, the state remains `CONTEXT_LOST`, so callers can distinguish "never initialized" from "lost and not yet recovered".

## State transitions

```text
UNINITIALIZED --successful initialize--> ACTIVE
ACTIVE --onContextLost-----------------> CONTEXT_LOST
CONTEXT_LOST --successful initialize---> ACTIVE
UNINITIALIZED / ACTIVE / CONTEXT_LOST --shutdown--> SHUTDOWN
SHUTDOWN --initialize------------------> rejected
```

A failed provider search does not advance the state.

## Important limits

This stage does **not** recreate GPU resources. In particular:

- `Capabilities.INSTANCE` is a static snapshot created from the original desktop GL context.
- `SharedIndexBuffer.INSTANCE` and its sibling singleton buffers own GPU handles without a context-rebuild path.
- `VoxyCommon` permits its instance factory to be installed only once.
- `VoxyRenderSystem` owns a large graph of renderer, pipeline, stream, geometry, and viewport resources, several of which still issue direct LWJGL calls.
- Old wrappers are generation-guarded, but higher-level owners do not yet reconstruct replacement instances after context loss.

Therefore, reinitializing the backend after `CONTEXT_LOST` alone is not a supported recovery sequence. A later stage must first introduce explicit GPU-resource owner teardown/recreation and resettable capability discovery, then wire actual Android lifecycle callbacks.

## Validation status

- Source was updated through GitHub Contents API.
- No Gradle compilation, automated tests, desktop runtime test, Android EGL/context-loss test, or Minecraft gameplay test was run.

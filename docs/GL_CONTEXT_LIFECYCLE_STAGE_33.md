# GL backend context lifecycle — Stage 33

## Scope

Audited `GlBackendRuntime` ownership and initialization. The previous runtime selected one backend once and had no supported way to release it or announce graphics-context loss. That is unsafe for Android lifecycle/context recreation.

## Changes

- `requireBackend()` now rejects access from a thread other than the thread that initialized the backend. GL context ownership is thread-affine; this turns accidental cross-thread calls into an explicit error instead of issuing calls against an unavailable context.
- Repeated initialization is idempotent only on the original initialization thread.
- Added `onContextLost()` to discard the backend reference without issuing GL deletion calls. This is the required path after the old context is already invalid.
- Added `shutdown()` for graceful backend close while its context is still valid and current. It clears runtime state even if backend close throws.
- Added a monotonically increasing context-generation value for diagnostics and future stale-resource checks.
- Documented the requirement to notify runtime before initializing a backend for a replacement context.

## Important remaining integration work

This is lifecycle infrastructure, not full context-recovery support:

1. Minecraft/platform lifecycle hooks must actually call `onContextLost()` when the GL context is destroyed and call `initialize(...)` after the replacement context is current. Those hooks were not wired in this stage.
2. Existing GPU wrappers do not yet capture/check the context generation. Old wrappers must be discarded after context loss; attempting to free an old handle through a newly initialized backend could target an unrelated object with the same numeric handle. A later stage must add stale-generation protection or central resource invalidation.
3. Renderer-owned GPU resources must be rebuilt after context recreation. This stage does not recreate them.
4. No Android EGL/GLSurfaceView/renderer lifecycle adapter was added.
5. Thread affinity is now explicit. Any existing call site that performs GL work from a worker thread will need to marshal that work to the context thread; this requires runtime verification.

## Validation status

- Updated runtime and this document were committed and fetched back.
- No Gradle compilation, automated tests, desktop runtime test, Android context-loss test, or Minecraft gameplay test was run.

# Resettable DownloadStream lifecycle — Stage 42

## Problem

`DownloadStream.INSTANCE` was an eager static singleton that allocated its download resources at class initialization and could not be recreated for a new graphics context. Its queues contain callback objects, persistent-mapping allocations, fences, and (in CPU fallback mode) native staging allocations.

## Changes

- Replaced eager singleton construction with a nullable, volatile instance.
- Added `initialize()`, `requireInstance()`, and `shutdown()`.
- Captured the creating graphics context generation and guard download, commit, tick, discard, and flush operations against stale contexts.
- Shutdown flushes and delivers pending callbacks while the original context is still valid, then waits for GPU work and releases the persistent readback buffer and fence wrappers.
- If the context has already been lost, shutdown avoids backend calls, drops queued callbacks and Java-side allocations, releases CPU-native staging allocations, and frees generation-aware wrappers without issuing GL deletes against the dead context.
- Cleanup attempts to release every remaining fence and the persistent buffer even if callback delivery or an earlier cleanup operation fails; failures are aggregated.
- Wired initialization after backend/capability setup and before renderer resources are created.
- Wired shutdown after the Voxy world/render instance is stopped and before upload stream, capabilities, and backend teardown.
- Migrated the renderer's two flush call sites to `DownloadStream.requireInstance()`.

## CPU fallback and persistent mapping

- CPU readback remains synchronous at the GPU mapping/copy stage and queues stable native-memory copies for callback delivery on `tick()`.
- Persistent mapping retains its fenced asynchronous readback path.
- Shutdown in a live context uses the existing flush-and-deliver semantics; shutdown after context loss discards callbacks because their GPU data can no longer be safely read.

## Boundaries

This stage only resets the global `DownloadStream`. It does not reset `RawDownloadStream`, implement GLES, or restore the full renderer graph after context loss. The project remains desktop-backend-only at this point.

## Validation

Changed files were fetched back from the target branch and inspected, including the renderer call sites and lifecycle wiring. No Gradle compilation, automated test, Minecraft launch, or Android device test was run.

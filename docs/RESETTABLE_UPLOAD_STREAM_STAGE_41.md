# Resettable UploadStream lifecycle — Stage 41

## Problem

`UploadStream.INSTANCE` was an eager `static final` singleton. Loading the class immediately allocated a 64 MiB staging buffer and, on backends supporting persistent mapping, a GPU buffer and mapped pointer. The owner could not be recreated for a new graphics context, and pending allocations/fences had no explicit shutdown path.

## Changes

- Replaced eager static construction with a nullable, volatile instance.
- Added `initialize()`, which requires an active backend, creates the stream for the current context generation, and safely retires a previous generation before publishing the new instance.
- Added `requireInstance()` so internal call sites fail with a clear lifecycle error instead of dereferencing a null singleton.
- Added `shutdown()`, which detaches the static reference before cleanup.
- Captured the creating context generation and guard all upload, commit, tick-through-commit, and mapped-address operations against stale contexts.
- Shutdown attempts to submit pending uploads while the original context is still valid, waits for submitted GPU work, frees outstanding fence wrappers, releases the persistent mapped buffer or CPU staging allocation, and clears Java-side queues.
- If the context was already lost, shutdown avoids backend calls; generation-aware fence and mapped-buffer wrappers discard old handles without issuing GL deletes.
- Wired initialization after capability setup and before shared index buffers are created.
- Wired shutdown after the Voxy instance and shared index buffers are released, but before capability/backend teardown.
- Migrated known internal call sites in `BufferArena`, `BasicSectionGeometryManager`, `SharedIndexBuffer`, and `VoxyRenderSystem` to `requireInstance()`.

## Important boundaries

- This lifecycle covers the singleton upload stream only. It does not reset `DownloadStream.INSTANCE`, other static GPU owners, or reconstruct the renderer graph.
- CPU staging still uses synchronous backend uploads; persistent-mapping mode still requires a backend with buffer-storage and persistent-mapping support.
- The public low-level `rawUploadAddress()` remains a specialized API; callers must still register destination uploads correctly in CPU staging mode.
- No GLES backend or Android EGL lifecycle integration is added by this stage.

## Validation

The modified source files were fetched back from the target branch and checked for stale `UploadStream.INSTANCE` call sites in the inspected consumers. No Gradle compilation, automated test, Minecraft launch, or Android device test was run.

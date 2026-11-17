# RawDownloadStream context lifecycle — Stage 43

## Audit

`RawDownloadStream` is an instance-owned helper, not a static singleton. It owns either a persistent mapped readback buffer or a conventional CPU-readback `GlBuffer`, plus an allocation arena, pending fragments, frame fences, and callbacks. Its public `free()` method is the owner-facing teardown point.

## Changes

- Capture the graphics context generation when the stream is constructed.
- Reject download allocation, submission, ticking, and buffer-handle access after the stream is freed or its originating context is stale.
- Route backend access through generation-aware `GlBackendRuntime.requireBackend(generation, ...)`.
- Make `free()` idempotent.
- During normal teardown with a live context: submit pending fragments, wait for GPU work, deliver completed callbacks through the existing tick path, then release any remaining frame allocations, fences, and the selected GPU buffer.
- During teardown after context loss: do not call backend methods; discard callbacks whose GPU data is no longer safely readable, release arena allocations, and free generation-aware wrappers without issuing GL deletes against the lost context.
- Aggregate callback and cleanup failures while still attempting to release every remaining resource.

## CPU readback behavior

The CPU fallback still maps the readback buffer, copies each fragment into temporary native memory, invokes the callback, and frees that temporary memory in a `finally` block. It does not retain a separate native staging queue between ticks.

## Boundaries

- No static `RawDownloadStream.INSTANCE` existed, so no global initialize/shutdown wiring was added.
- This stage hardens per-instance ownership; it does not prove every construction owner in the repository invokes `free()` before context destruction.
- It does not implement GLES, an Android EGL lifecycle hook, or full renderer recovery.

## Validation

The modified source was fetched back from the target branch and inspected. No Gradle compilation, automated test, Minecraft launch, or Android device test was run.

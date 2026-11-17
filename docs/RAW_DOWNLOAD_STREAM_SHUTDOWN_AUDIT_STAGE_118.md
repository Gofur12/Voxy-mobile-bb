# RawDownloadStream Shutdown Failure-Path Audit — Stage 118

## Scope
Audited `RawDownloadStream.free()`, `submit()`, and `tick()` in the active backend implementation.

## Finding
The previous `free()` path released frame fences and the backing GL buffer even when the graphics context was lost or backend `finish()` failed. A failed callback delivery is different: after a successful `finish()`, remaining GPU work is quiesced and leftover frame bookkeeping can be retired safely.

## Change
- Track successful GPU quiescence independently from callback/flush success.
- Always drop queued Java callback references and mark the stream closed.
- Only free remaining arena slots, fence wrappers, and the backing GL buffer after a successful backend `finish()`.
- On context loss or failed `finish()`, clear Java-side frame references but deliberately avoid GL deletion of potentially in-flight resources.
- Preserve callback failures while continuing independent cleanup.

## Limits
- Context-loss cleanup intentionally prefers avoiding unsafe GL calls over reclaiming driver resources.
- No injected backend failure, GL-context-loss, Android runtime, or build test was run.

# Raw Upload CPU Staging Audit — Stage 27

## Finding

`UploadStream.rawUpload(int)` allocates and returns a pointer without registering a destination `GlBuffer` copy. That is valid only for the persistent-mapped GPU upload buffer path, where callers can address the mapped GPU buffer directly.

In CPU staging mode there is no GPU upload-buffer object. The old method still returned a pointer into temporary CPU memory, but `commit()` only submitted entries registered through `upload(target, offset, size)`; it then reset the CPU arena. A raw allocation therefore had no defined destination and could be silently discarded.

## Change

- `rawUpload(int)` now throws `UnsupportedOperationException` when persistent GPU mapping is unavailable.
- The exception directs CPU-staging callers to `upload(target, offset, size)`, which records a destination copy and is supported by the CPU fallback.
- Persistent-mapped behavior remains unchanged.

## Call-site review

The inspected geometry manager, MDIC renderer, and shared-index-buffer upload paths use the destination-aware `upload(...)` API. No `rawUpload(...)` call was found in those inspected call sites. This is a targeted audit, not a proof over external integrations or downstream forks.

## Verification and remaining work

- Change committed on `android-port/stage-01-audit`.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.
- `rawUploadAddress(int)` remains public for compatibility; external callers should not use it as a destination-less upload in CPU staging mode. A later API cleanup can make the allocation primitive private once downstream usage is confirmed.
- The GLES backend and renderer remain unimplemented.

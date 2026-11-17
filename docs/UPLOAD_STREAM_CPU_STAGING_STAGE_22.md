# Stage 22 — CPU staging fallback for UploadStream

## What changed

`UploadStream` now selects its staging strategy from the active `GlBackend` capability snapshot:

- When both buffer storage and persistent mapping are available, it retains the existing GPU persistent-mapped upload buffer and fence-protected allocation reuse.
- Otherwise, it allocates a CPU-native staging arena. `upload(...)` and `rawUpload(...)` return pointers into that arena. `commit()` sends each queued range directly to its destination with `GlBackend.uploadBuffer(...)`.

The CPU path does not create a persistent-mapped GPU buffer, call named-buffer flush/copy functions, or create GPU fences. Since the backend upload call consumes the client-memory source before returning, the CPU arena is reset after all queued uploads have been submitted.

The stream's base alignment is now 16 bytes. Its allocations are copied into target buffers and are not used as indexed SSBO binding offsets, so querying desktop-only `Capabilities.INSTANCE.ssboBindingAlignment` was unnecessary and prevented this class from being loaded without the desktop capability probe.

## Call sites checked

The following in-repository callers use the `upload(...)` pointer-and-commit flow:

- `BasicSectionGeometryManager` uploads section metadata.
- `BufferArena` uploads built geometry.
- `MDICSectionRenderer` uploads its uniform data.

No in-repository call site was found for `getBaseAddress()` or `getRawBufferId()`. Those two methods explicitly throw in CPU staging mode because a CPU allocation has no corresponding GPU buffer object. The public `rawUploadAddress()` remains an offset into the active staging arena and is valid only until the next CPU-mode `commit()`.

## Limitations

- This is a staging strategy fallback, not an Android implementation. There is still no GLES backend/provider or Android context integration in this branch.
- `GlBackend.uploadBuffer` must be implemented by a future GLES backend with correct client-memory upload semantics.
- Other renderer components still use desktop-only OpenGL APIs and capabilities; this change does not make the MDIC renderer GLES-compatible.
- The 64 MiB singleton staging allocation remains native memory for the lifetime of the singleton, matching the long-lived nature of the previous upload stream.
- No Gradle compilation, automated test, desktop runtime test, Android runtime test, or Minecraft gameplay test was run for this stage.

## Commits

- `86f6b26` — CPU staging fallback.
- `6ad5a6a` — preserve arena capacity after commit/reset.

# Stage 23 — CPU readback fallback for DownloadStream

## What changed

`DownloadStream` now selects its readback strategy from the active backend capability snapshot:

- If buffer storage and persistent mapping are both available, it retains the existing persistent-mapped download buffer, GPU copy commands, and fence-based callback delivery.
- Otherwise, it maps the requested source-buffer range with `GlBackend.mapBufferRange(..., GL_MAP_READ_BIT)`, copies the bytes into native CPU memory, unmaps the GPU buffer, and queues the CPU copy for callback delivery on `tick()`.

The fallback preserves deferred callback delivery, but the actual GPU readback is synchronous: mapping a range for reading may wait for prior GPU work. This is deliberate; it does not pretend to provide asynchronous behavior without a PBO/fence implementation.

The CPU path frees each native staging allocation after its callback returns. `waitDiscard()` releases queued CPU copies without invoking callbacks. Range validation now avoids overflow-prone `offset + size` arithmetic.

## API and implementation notes

- The fallback reuses the backend's existing map/unmap contract; no new GL-specific API was added.
- The persistent-mapped desktop path remains in place.
- The CPU fallback requires a future GLES backend to implement readable `mapBufferRange` and `unmapBuffer` correctly for the selected buffer target.
- The callback pointer is valid only for the duration of the callback, matching the existing transient download-buffer usage model.

## Limitations

- No GLES backend/provider or Android context integration exists yet.
- GLES mapping behavior and synchronization have not been runtime-verified on MobileGlues, Zink/Kopper, or Adreno.
- The fallback may stall the render thread while waiting for GPU completion; a later Android-specific asynchronous readback strategy may improve this.
- `RawDownloadStream` remains persistent-mapping-only and is not migrated by this stage.
- Other renderer code still contains desktop-only OpenGL calls; this does not make the renderer Android-compatible.
- No Gradle compilation, automated test, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.

## Commits

- `a7ac1e5` — CPU readback fallback.
- `9168ef7` — cleanup hardening for failed map/copy/unmap operations.

# Stage 24 — RawDownloadStream CPU readback fallback

## What changed

`RawDownloadStream` now selects its storage strategy from the active backend capabilities:

- When both buffer storage and persistent mapping are supported, it keeps the original persistently mapped download buffer.
- Otherwise it allocates an ordinary mutable `GlBuffer`. Callers continue writing into the buffer returned by `getBufferId()`, using the integer offset returned by `download()`.
- On `tick()`, the fallback waits for the frame fence, maps each completed allocation for read, copies it into temporary native memory, unmaps the GPU buffer, invokes the callback, and frees the temporary memory immediately after the callback returns.
- Allocation offsets are returned to the arena after the frame callbacks have been processed.
- `free()` finishes outstanding GPU work, drains submitted frames, then releases the selected buffer.

## API compatibility

The public constructor, `download(int, IDownloadCompletedCallback)`, `submit()`, `tick()`, and `getBufferId()` remain. The integer returned from `download()` remains a byte offset into the buffer ID returned by `getBufferId()`.

The callback pointer is valid only during the callback, matching the class contract.

## Limitations / validation status

- This is a synchronous map-and-copy fallback at callback delivery time; it is not persistent asynchronous mapping.
- A future GLES backend must implement readable `mapBufferRange`, `unmapBuffer`, fences, and `finish()` correctly.
- The fallback assumes the producer writes into the returned buffer range before `submit()` fences that work.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test has been run for this stage.
- This stage does not add a GLES backend or make the full renderer Android-compatible.

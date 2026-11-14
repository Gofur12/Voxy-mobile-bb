# Stage 21 — Portable Buffer Zero/Fill Fallback

## Changes

- Migrated `GlBuffer.zero()` and `zeroRange()` away from backend
  `clearBuffer` / `clearBufferRange` operations.
- Migrated `GlBuffer.fill(int)` away from desktop buffer-clear APIs.
- All three methods now use `GlBackend.uploadBuffer`, which Stage 20 changed
  to a bind-to-edit `glBufferSubData` implementation in the desktop adapter.
- Staging is bounded to a 64 KiB native-memory block. Large ranges are uploaded
  in chunks rather than allocating a second buffer-sized CPU copy.
- Native staging memory is released in a `finally` block.
- Range validation prevents negative offsets, overflow-prone end calculations,
  and writes beyond the buffer allocation.
- The 32-bit fill path validates 4-byte alignment, matching its former
  `GL_R32UI` clear semantics.

## Portability significance

The conventional zero/fill path no longer requires
`glClearNamedBufferData` or `glClearNamedBufferSubData`. A future GLES 3.x
backend can implement the existing `uploadBuffer` contract with
`glBindBuffer` + `glBufferSubData` and support these operations without
requiring GLES 3.2 clear-buffer functionality.

## Remaining blockers

- This repository still has no GLES backend/provider or Android context
  bootstrap.
- Persistent mapping and `UploadStream` still depend on desktop-only
  operations (mapped persistent pointers, named-buffer flush/copy, and the
  existing synchronization path).
- Texture, framebuffer, VAO, shader, and render pipeline APIs remain partly
  desktop-specific.
- The branch's build remains Minecraft 26.3 / Java 25, not a 1.20.1 / Java 17
  Android target.

## Verification

Source files were committed and fetched back from the target branch. No Gradle
compile, automated test, desktop runtime test, Android runtime test, or
Minecraft gameplay test was run. Do not treat this stage as a verified build.

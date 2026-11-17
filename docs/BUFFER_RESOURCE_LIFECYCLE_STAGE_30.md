# Buffer resource lifecycle audit — Stage 30

## Scope

Audited `GlPersistentMappedBuffer` and `GlBuffer` for capability assumptions and failure cleanup. This stage does not implement a new GLES backend.

## Findings and changes

### Persistent mapped buffers

- The wrapper previously requested persistent storage without checking the selected backend's advertised capabilities.
- If mapping failed, buffer deletion was attempted, but a deletion failure could mask the original mapping failure.
- `free()` ignored a false return from unmap and would skip buffer deletion if unmap threw.

Changes:
- Reject construction unless the backend advertises both buffer storage and persistent mapping.
- Validate positive allocation size.
- Preserve the original mapping error and attach cleanup failures as suppressed exceptions.
- Always attempt buffer deletion after unmap; report invalid unmap contents or deletion errors.

### Ordinary buffers

- A failure while zero-initializing a newly created buffer left the GPU allocation behind because construction had not completed and callers could not free it.
- Buffer accounting was updated only after successful initialization, but deletion failure during normal free could leave the static counters stale.

Changes:
- Validate positive allocation size.
- If optional zero initialization fails, attempt to delete the just-created buffer and preserve cleanup errors as suppressed exceptions.
- Update resource counters in a `finally` block when freeing, so accounting reflects the wrapper's terminal free operation even if backend deletion reports an error.

## Android/GLES implications

- `GlPersistentMappedBuffer` is now explicitly unavailable when the backend does not advertise persistent mapping. Callers must use the already introduced ordinary-buffer staging/readback paths.
- `GlBuffer` still uses backend `createBuffer`, `uploadBuffer`, and `deleteBuffer`; an actual GLES backend must implement those methods.
- Sparse-buffer flags remain desktop-specific and must not be passed by a GLES path.
- This change does not migrate remaining LWJGL constants/imports out of the wrappers and does not provide Android context creation or GLES resource operations.

## Validation status

- Both source files were committed and fetched back from the target branch.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.

# RawDownloadStream lifecycle hardening — Stage 28

## Scope

This stage audits callback delivery, CPU readback staging memory, frame cleanup, and stream shutdown in `RawDownloadStream`. It does not add a GLES backend or claim Android runtime compatibility.

## Findings and changes

- A callback exception previously stopped iteration of the current frame. Remaining callbacks were skipped even though their arena allocations were released.
- The previous CPU readback cleanup could leak its destination allocation if `unmapBuffer` itself threw while executing a `finally` block.
- A callback exception during `free()` could prevent the underlying GPU buffer from being released, even after the GPU had been synchronously finished.

The implementation now:
- Attempts every callback in a signaled frame, recording the first failure and attaching later failures as suppressed exceptions.
- Frees every fragment allocation and the frame fence even if a callback fails.
- Frees CPU staging memory if mapping, copying, or unmapping fails; callback-owned access remains limited to the callback duration.
- Attempts stream-buffer cleanup after `finish()` and callback draining. If frames unexpectedly remain, it reports the invariant failure and deliberately does not free a buffer that may still be referenced by pending work.

## Preserved behavior

- Persistent-mapped mode still passes a pointer into the persistent GPU mapping.
- CPU fallback still maps the readback buffer, copies into native staging memory, unmaps, and invokes the callback with the temporary CPU pointer.
- `submit()` still fences the writes issued by the caller; `tick()` only delivers frames whose fences have signaled.

## Validation status

- Source was committed and fetched back from the target branch.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.
- This is lifecycle hardening only. It does not implement GLES operations or prove that the current buffer/fence backend can run on Android.

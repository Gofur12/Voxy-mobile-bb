# DownloadStream lifecycle hardening — Stage 29

## Scope

This stage audits callback delivery, CPU readback staging cleanup, and discard semantics in `DownloadStream`. It does not add a GLES backend or establish Android runtime compatibility.

## Findings and changes

- In CPU fallback mode, a callback exception stopped the tick loop, leaving later queued callbacks and their native allocations pending.
- In persistent-mapped mode, a callback exception stopped processing the frame before its arena allocations and fence were released.
- The CPU readback `finally` block could leak the destination allocation if `unmapBuffer` threw.
- Persistent-mode `waitDiscard()` did not account for allocations and callback records accumulated since the last frame fence. It also created an extra fence after a GPU finish even though the finish already establishes completion.

The implementation now:
- Attempts every queued CPU callback and frees each staging allocation, then rethrows the first callback failure with later failures suppressed.
- Attempts every callback in each signaled persistent frame and always attempts allocation/fence cleanup.
- Frees CPU staging memory when mapping, copying, or unmapping fails.
- Implements persistent-mode `waitDiscard()` as a true discard: waits for prior GPU work to finish, drops queued callbacks that have not been delivered, frees all unfenced and fenced arena allocations, and deletes frame fences.
- Resets the current allocation cursor after discard so subsequent downloads start from a clean arena state.

## Preserved behavior

- CPU fallback still performs a synchronous map/copy/unmap and defers callback delivery until `tick()`.
- Persistent mode still copies requested source ranges into the persistent download buffer and delivers callbacks only after the associated frame fence signals.
- `flushWaitClear()` retains its existing deliver-and-wait behavior; this stage does not change its public semantics.

## Validation status

- Source was committed to the target branch.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.
- The changes harden stream lifecycle only; they do not supply GLES implementations for mapping, buffer copies, or fences.

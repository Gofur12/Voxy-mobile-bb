# RawDownloadStream Submit Transaction Audit — Stage 119

## Finding
`submit()` previously cleared `frameFragments` before constructing its `GlFence`. If fence creation failed, the allocation records and callbacks were detached from the stream, so later shutdown could not reclaim those arena slots or release the callback references.

## Change
- Snapshot fragments without clearing the owning list.
- Construct the fence and enqueue the frame first.
- Clear `frameFragments` only after successful frame enqueue.
- If enqueue fails after fence creation, attempt to free the newly created fence and attach any cleanup failure to the original exception.

## Limits
- This closes the Java-side ownership gap for fence/frame construction failures.
- No injected allocation failure, backend failure, build, or Android runtime test was run.

# Upload/Download Backend Completion — Stage 26

## Scope

This stage removes remaining direct desktop OpenGL calls from the upload/download stream coordination paths that can be expressed by the current backend contract.

## Changes

- Added `GlBackend.flushMappedBufferRange(handle, offset, length)` for explicitly flushing non-coherent persistent mappings.
- Implemented the flush operation in `DesktopGlBackend` with OpenGL 4.5 DSA and range validation.
- Routed `UploadStream` mapped-range flushes through the backend.
- Routed `UploadStream` GPU buffer copies through `GlBackend.copyBufferSubData`.
- Routed `UploadStream` and `DownloadStream` explicit GPU waits through `GlBackend.finish()`.
- Removed direct desktop flush/copy/wait invocations from those stream classes.

## Compatibility notes

This is backend-boundary cleanup, not an Android renderer implementation. The desktop adapter still uses OpenGL 4.5 DSA. A GLES backend must implement mapped-range flushing only when it supports non-coherent persistent mapping; otherwise the capability selection must choose the CPU staging path and never call this operation.

## Known remaining work

- `RawDownloadStream` still relies on the backend map/unmap contract for its CPU readback fallback and needs lifecycle/error-path testing.
- The CPU `UploadStream.rawUpload()` API returns a native staging pointer but does not itself enqueue a destination-buffer copy. Its actual call sites and semantics need a dedicated audit before claiming complete CPU fallback coverage.
- Other renderer classes still contain direct LWJGL calls, including the MDIC/Hi-Z paths. No GLES renderer exists yet.

## Verification

- Source changes were committed on `android-port/stage-01-audit`.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run for this stage.

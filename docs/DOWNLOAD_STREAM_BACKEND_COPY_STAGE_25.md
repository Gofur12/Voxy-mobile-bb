# Stage 25 — DownloadStream backend copy and barrier operations

## Changes

- Added `GlBackend.copyBufferSubData(source, destination, sourceOffset, destinationOffset, size)`.
- Implemented the operation in `DesktopGlBackend` with OpenGL 4.5 named-buffer copy.
- Routed the persistent-mapped `DownloadStream.commit()` path through the active backend for:
  - buffer-update memory barriers,
  - buffer-to-download-buffer copies,
  - client-mapped-buffer visibility barriers.
- Replaced direct `glFinish()` calls in `DownloadStream` with `GlBackend.finish()`.
- Removed direct OpenGL 4.2/4.4/4.5 imports from `DownloadStream`. The barrier bit values remain API constants, while the actual operations are backend-owned.

## Compatibility notes

The desktop implementation preserves the existing named-buffer-copy behavior. A GLES backend must implement buffer copies using a supported bind-to-edit copy path (or another correct fallback) and provide the required barrier semantics. The persistent-mapping branch still only activates when the backend advertises both buffer storage and persistent mapping.

## Validation status

- Source changes were committed and fetched back from the target branch.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test has been run.
- This stage does not implement the GLES backend and does not yet make Voxy's renderer Android-compatible.

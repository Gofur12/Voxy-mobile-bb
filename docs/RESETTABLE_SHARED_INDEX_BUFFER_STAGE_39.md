# Resettable shared index-buffer owner — Stage 39

## Problem

`SharedIndexBuffer` previously allocated three GPU buffers in static field initializers:

- `INSTANCE` — 16-bit quad indices plus cube indices
- `INSTANCE_BYTE` — byte quad indices plus cube indices
- `INSTANCE_BB_BYTE` — byte cube indices

That meant merely initializing the Java class attempted GPU allocation, and the GPU handles lived for the process lifetime. They could not be cleanly released and recreated for a replacement graphics context.

## Changes

- Replaced eager `static final` GPU instances with nullable, volatile references.
- Added synchronized `initialize()`, which requires an active backend, creates all three variants, and publishes them only after all constructors succeed.
- Initialization is idempotent for the current context generation.
- Added synchronized `shutdown()`, which clears published references before releasing each unique buffer and aggregates cleanup failures.
- Each instance records its context generation; `id()` rejects access from a different or unavailable context.
- Constructor failures now release temporary CPU index data and the partially allocated GPU buffer.
- The byte-index constructor now commits its upload stream, matching the other constructors and ensuring CPU-staging uploads are submitted.
- Wired initialization into `VoxyClient.initVoxyClient()` and release into `VoxyClient.shutdownGraphicsResources()`, after the Voxy world instance shuts down and before the backend closes.

## Lifecycle order

Startup:
1. Initialize the graphics backend.
2. Capture the context capability snapshot.
3. Initialize the shared index buffers.
4. Register the Voxy instance factory and allow renderer construction.

Shutdown:
1. Shut down the active Voxy instance.
2. Release shared index buffers.
3. Clear the capability snapshot.
4. Shut down the graphics backend.

## Remaining work

- Other static GPU owners remain, including `UploadStream.INSTANCE`, `DownloadStream.INSTANCE`, `SharedIndexBuffer` consumers outside the currently inspected renderer path, and static vertex-array state.
- Renderer graph reconstruction after context loss is not implemented.
- Android EGL lifecycle callbacks and a GLES backend are not implemented.
- This change does not make the current MDIC renderer GLES-compatible.

## Validation

Changed source files were fetched back from the target branch and inspected. No Gradle compilation, automated test, Minecraft launch, or Android device test was run.

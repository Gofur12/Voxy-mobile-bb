# Stage 44 — DownloadStream singleton call-site completion

## Scope

After Stage 42 changed `DownloadStream.INSTANCE` from an eagerly-created static final singleton to a resettable lifecycle-managed instance, this stage audited the renderer and arena call sites that still referenced the old field.

## Changes

Two missed call sites were migrated to the lifecycle API:

- `AbstractRenderPipeline.innerPrimaryWork()`: stream ticking now uses `DownloadStream.requireInstance().tick()`.
- `BufferArena.downloadRemove()`: queued readback now uses `DownloadStream.requireInstance().download(...)`.

This prevents those paths from dereferencing the removed/null legacy singleton after Stage 42's lifecycle conversion. It also makes use-before-initialize or use-after-shutdown fail through the explicit lifecycle guard rather than an accidental null dereference.

## Additional audit findings

- `RawDownloadStream` is an instance-owned helper, not a singleton. Its constructor captures the current context generation and its `free()` method handles both live-context cleanup and context-loss abandonment.
- This audit did not establish all construction/owner call sites for `RawDownloadStream`; do not assume every instance is automatically freed. A complete owner-lifecycle proof remains open.
- `AbstractRenderPipeline` still owns static GL/native resources: `DEPTH_SAMPLER` is generated at class initialization and `SCRATCH` is allocated with `MemoryUtil.nmemAlloc`. They are not tied to `GlBackendRuntime` context generations and have no matching class-level cleanup. These are concrete cross-context/lifetime hazards for a future context-recovery design.
- The pipeline and renderer still contain many direct LWJGL calls. This stage does not make the render graph GLES-compatible.

## Verification

- Both changed files were fetched from branch `android-port/stage-01-audit` before editing and updated using their current blob SHAs.
- The two call sites were changed to `DownloadStream.requireInstance()`.
- No Gradle build, automated test, Minecraft launch, or Android-device test was run.

## Remaining limitations

No Android EGL integration, GLES backend, context restoration, or renderer graph reconstruction is delivered by this stage. The static pipeline resources and RawDownloadStream ownership audit remain follow-up work.

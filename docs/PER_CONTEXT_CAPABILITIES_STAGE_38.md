# Per-context graphics capability snapshot — Stage 38

## Purpose

The old `Capabilities.INSTANCE` was a `static final` object constructed when the class was first initialized. Its constructor immediately queried LWJGL's current OpenGL context, so whichever context happened to trigger class initialization became the process-lifetime snapshot. That design cannot represent a later graphics context.

## Changes

- Replaced eager `static final` initialization with a nullable, volatile active snapshot.
- `Capabilities.init()` now requires an initialized `GlBackendRuntime`, clears any prior snapshot, probes the current context, and publishes a fresh snapshot only after construction succeeds.
- Each snapshot records `GlBackendRuntime.getContextGeneration()`.
- `Capabilities.require()` fails clearly when no snapshot exists and rejects a snapshot belonging to an older context generation.
- GPU-memory query method checks the snapshot generation before issuing GL queries.
- Migrated the known runtime consumers in `VoxyClient`, `BasicSectionGeometryData`, `BufferArena`, and `MDICSectionRenderer` from direct `Capabilities.INSTANCE` access to `Capabilities.require()`.
- Client shutdown clears the snapshot after the Voxy instance teardown and before backend shutdown.

## Ordering

1. Initialize the graphics backend while the context is current.
2. Call `Capabilities.init()` to capture that context's capability snapshot.
3. Construct Voxy GPU resources and renderer objects.
4. During orderly shutdown, tear down Voxy resources, clear the snapshot, then shut down the backend.

## Boundaries

This stage removes the eager process-lifetime capability snapshot and makes migrated reads generation-aware. It does **not** make the renderer recoverable after context loss: static GPU owners, renderer graph reconstruction, and platform EGL lifecycle integration remain outstanding. Some capability probes still compile desktop GLSL and use desktop-only GL calls; this class is not yet an Android/GLES capability probe.

## Validation status

Source edits were fetched back from the target branch for inspection. No Gradle compilation, unit tests, Minecraft launch, or Android renderer test was run in this stage.

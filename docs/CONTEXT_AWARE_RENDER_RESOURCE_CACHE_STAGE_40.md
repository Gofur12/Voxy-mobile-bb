# Context-aware render resource cache — Stage 40

## Problem

`RenderResourceReuse` held model texture atlases and geometry buffers in process-wide static lists. Cached wrappers could therefore outlive the graphics context that created their GL handles. The class also still read `Capabilities.INSTANCE` directly after the capability API had changed to an explicit context-bound snapshot in Stage 38.

## Changes

- Added a cache context-generation marker tied to the active `GlBackendRuntime`.
- Every cache acquisition and return now requires an active backend and synchronizes the cache generation.
- When a new context generation is observed, cached wrappers from the previous generation are removed and released. Their generation-aware wrappers avoid issuing GL deletes against the replacement context.
- Cache return methods reject null, freed, or stale resources; texture returns also require allocated storage.
- Cache operations are synchronized so list mutation and generation transitions are serialized.
- `clearResources()` now detaches cached references first, attempts to free every cached texture and buffer, and aggregates cleanup failures instead of stopping at the first error.
- Migrated all capability reads in this cache to one local `Capabilities.require()` snapshot. This closes a missed Stage 38 call-site that would otherwise dereference the now-nullable legacy `INSTANCE`.
- Added `GlBuffer.assertContextCurrent()` to match the existing texture wrapper guard.
- Texture-atlas allocation now frees its wrapper if storage allocation or naming fails.

## Scope

This stage protects only resources held in `RenderResourceReuse`. It does not reconstruct active renderer resources after context loss, make the render pipeline GLES-compatible, or replace other static GPU owners such as upload/download streams and static vertex-array state.

The geometry allocation policy still uses desktop-oriented sparse-buffer and driver-query logic. This cache lifecycle change is not an Android backend implementation.

## Validation

The changed source files were fetched back from the target branch and inspected. No Gradle compilation, automated test, Minecraft launch, or Android device test was run.

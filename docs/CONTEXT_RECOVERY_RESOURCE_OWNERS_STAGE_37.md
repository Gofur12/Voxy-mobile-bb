# Stage 37 — Context Recovery Resource Owner Audit

## Purpose

Map the concrete owners that prevent safe context recreation. This is an implementation-oriented dependency audit, not a claim that Android context recovery is already supported.

## Findings from the current branch

### 1. Capability discovery is process-global and eager

File: `src/main/java/me/cortex/voxy/client/core/gl/Capabilities.java`

- `Capabilities.INSTANCE` is constructed as a static final field.
- Construction calls `GL.getCapabilities()`, reads desktop GL limits/extensions, and may compile test shaders and allocate GL objects.
- `init()` is empty; it only causes class initialization when called.
- The snapshot cannot be replaced after a context is lost or a new context becomes current.
- It contains desktop-only probes (for example GL 4.5 named-object calls, compute shaders, SSBO limits and NVX memory information).

**Consequence:** backend generation changes do not refresh capability data. This class must become an explicitly-created per-context snapshot before recovery can be attempted.

### 2. Shared index buffers are static GPU owners without disposal

File: `src/main/java/me/cortex/voxy/client/core/rendering/util/SharedIndexBuffer.java`

- Three static final instances exist: `INSTANCE`, `INSTANCE_BYTE`, and `INSTANCE_BB_BYTE`.
- Each constructor allocates a `GlBuffer` and uploads generated index data.
- The public `id()` returns the underlying handle.
- There is no `free()`, context-generation check at the singleton owner level, or recreation method.
- Referencing `SharedIndexBuffer.INSTANCE` initializes all three static fields, not just the first one.

**Consequence:** the first class initialization binds these shared buffers to one context. After context loss, their integer handles cannot be reused and the static final instances cannot be replaced.

### 3. Render-resource reuse cache can retain stale wrappers

File: `src/main/java/me/cortex/voxy/client/core/RenderResourceReuse.java`

- Static lists cache `GlTexture` model atlases and `GlBuffer` geometry buffers.
- `clearResources()` frees cached wrappers and clears both lists.
- Normal cache clearing is currently reached from `VoxyClientInstance.shutdown()`, after `super.shutdown()`.
- A context-loss path cannot safely call normal GL deletion on wrappers from the lost context; the cache has no discard-without-GL-delete operation and no generation partitioning.

**Consequence:** cache ownership needs explicit context-loss invalidation distinct from graceful resource freeing.

### 4. Client instance factory is one-shot

File: `src/main/java/me/cortex/voxy/commonImpl/VoxyCommon.java`

- `setInstanceFactory()` rejects a second registration.
- `shutdownInstance()` clears and shuts down the current instance, but does not reset the factory.
- `createInstance()` can create a later instance from the existing factory, but this does not solve GPU resource reconstruction inside renderer owners.

**Consequence:** do not call `setInstanceFactory()` again on recovery. A recovery design should preserve the factory and recreate only the world instance after all GPU owners have been reset.

### 5. Renderer graph owns many context-bound resources

File: `src/main/java/me/cortex/voxy/client/core/VoxyRenderSystem.java`

The render system creates and coordinates geometry data, node/traversal managers, render pipeline, section renderer, viewport selector, bound renderers and render-distance tracking. Its normal `shutdown()` flushes streams and frees those owners, but multiple shutdown steps still issue GL operations directly or through wrappers.

**Consequence:** calling the existing normal shutdown after the context is already lost is not safe. Recovery needs two distinct paths:
- graceful teardown while the old context is still current;
- context-loss invalidation that discards Java-side ownership without issuing GL calls.

A new context then needs a fresh renderer graph; existing objects must not be revived by simply swapping the backend.

### 6. Streams and arenas are context-bound owners too

Files: `UploadStream.java`, `DownloadStream.java`, `RawDownloadStream.java`, `BufferArena.java`.

The streams own mapped or ordinary GPU buffers, fences, queued transfers and CPU staging memory. Arenas own GPU buffers. Their existing `free()`/flush behavior is designed for orderly operation with a live backend; no common context-loss invalidation contract is present.

**Consequence:** these must be included in the owner inventory and shutdown ordering. Pending callbacks and CPU allocations require deterministic disposal even when GPU completion can no longer be queried.

## Required recovery architecture (dependency order)

1. **Per-context capability snapshot:** replace eager `Capabilities.INSTANCE` with an explicit immutable snapshot created only after a backend/context is active. Pass it to consumers or expose a generation-checked accessor.
2. **GPU owner lifecycle contract:** define separate graceful `close()` and context-loss `invalidate()` semantics. Invalidation must never call GL.
3. **Static resource owners:** convert `SharedIndexBuffer` and global streams from static-final GPU instances to resettable owners/factories; record the context generation for every created resource.
4. **Cache invalidation:** make `RenderResourceReuse` clear Java references on context loss without trying to delete old-context handles; recreate its cached resources lazily on the new context.
5. **Renderer graph rebuild:** ensure the old world renderer is detached and its CPU-side workers stop, then construct a wholly new graph using the new context's capabilities and backend.
6. **Platform lifecycle integration:** only after the above exists, connect Android EGL/context callbacks to loss and restore transitions. A successful backend initialization alone must not resume rendering.

## Explicitly rejected shortcut

Do not implement `onContextRestored()` as merely `GlBackendRuntime.initialize(...)`. That would reactivate a backend while static capability snapshots, shared index buffers, stream buffers, cache entries and renderer objects still point to the old context generation.

## Validation status

- Inspected the current branch sources listed above and recorded concrete ownership and lifecycle constraints.
- This stage adds an audit/implementation plan only; no runtime ownership semantics were changed.
- No Gradle compilation, automated tests, Android EGL test, or Minecraft gameplay test was run.

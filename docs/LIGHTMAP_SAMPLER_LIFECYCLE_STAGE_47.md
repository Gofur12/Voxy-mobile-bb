# Lightmap Sampler Lifecycle — Stage 47

## Implemented

Files:
- `src/main/java/me/cortex/voxy/client/core/rendering/util/LightMapHelper.java`
- `src/main/java/me/cortex/voxy/client/VoxyClient.java`

Changes:
- Removed class-load-time creation of the static lightmap sampler.
- Sampler creation is now lazy and occurs on first bind, after requiring the active backend/context thread.
- The sampler records its context generation.
- A stale sampler handle is discarded without issuing deletion against a replacement context.
- Sampler parameter setup is exception-safe: a partially configured sampler is deleted when the originating context is still current.
- Added idempotent `LightMapHelper.shutdown()`.
- Wired sampler shutdown into `VoxyClient.shutdownGraphicsResources()` before capability/backend shutdown. The method also safely discards the Java handle after context loss without trying to delete an invalid GL object.
- Added validation that the requested lighting texture unit is non-negative.

## Preserved behavior

The sampler still uses linear minification/magnification and clamp-to-edge wrapping, and `bind(int)` still binds Minecraft's current lightmap texture to the requested texture unit.

## Remaining limitations

- This is lifecycle hardening on the desktop LWJGL path, not an Android GLES sampler backend.
- The lightmap texture is still obtained through the RenderPearl/Minecraft texture wrapper.
- GL sampler and texture binding calls remain direct LWJGL calls.
- `GlVertexArray.STATIC_VAO` remains a separate class-static GL resource requiring its own call-site/lifetime audit.

## Verification

Source-level review only. No Gradle build, automated tests, Minecraft launch, or Android device test was run.

# Fabric 1.20.1 mixin boundary — Stage 88

Baseline: Stage 87 inventory (CI job `build-1-20-1`, `:compileJava` failed).

## Why this stage exists

javac stops at 100 errors by default, so the Stage 87 log only showed the first slice of the failures. Stage 88 raises `-Xmaxerrs`/`-Xmaxwarns` (1.20.1 target only) so the next CI log lists every remaining error, and fixes the first blocker group: mixins that target 26.3-only classes.

## Changes (1.20.1 target only; the default 26.3 build is untouched)

Replaced by real 1.20.1 sources in `src/1.20.1/java` (main versions excluded in `build.gradle`):
- `MixinClientChunkCache` + new `MixinClientChunkCacheStorage` / `ICheekyClientChunkStorage`: `drop(int, int)`, public `ChunkPos.x/z`, and `ClientChunkCache$Storage` accessed through a shadow-based interface instead of non-public members.
- `MixinClientLevel`: `setBlocksDirty` ingest hook kept; constructor hook dropped, `Level#getMinSection` used, `SectionPos#getX/getY/getZ`, 1.20.1 `ChunkStatus` package.
- `MixinRenderSystem`: `initRenderer(int, boolean)`; no RenderPearl `GpuDevice`.
- `MixinMinecraft`: world-close and shutdown hooks kept; RenderPearl backend wrapper removed; `disconnect(Screen)` signature.
- `MixinLevelRendererLifecycle` replaces `MixinLevelExtractor` (`LevelRenderer.setLevel` / `allChanged`).

Disabled on 1.20.1 because the target class does not exist there (removed from `client.voxy.1.20.1.mixins.json`):
- `MixinLevelExtractor` (superseded above), `MixinFogRenderer` (`client.renderer.fog`), `MixinClientCommonPacketListenerImpl` (class added after 1.20.1).

## Unverified assumptions (check on the next CI run)

Written without a Minecraft 1.20.1 classpath. Compile-time names are from memory of the 1.20.1 Mojang mappings; the mixin injection targets (`drop`, `setBlocksDirty`, `initRenderer`, `disconnect(Screen)`, `setLevel`, `allChanged`, `Storage.getChunk/getIndex`) are only validated at mixin-apply time, i.e. runtime.

## Still open

`ModelFactory`, `RenderDataFactory`, `SoftwareModelTextureBakery`, `ReuseVertexConsumer`, `LightMapHelper`, `TextureUtils` (Sodium `ColorSRGB`), `DepthTransformerAccessor` (Iris), `Logger`, `ClientImportManager`, `Taskbar`, `NormalRenderPipeline`, `DHImporter`, `WorldImporter`, plus whatever the uncapped log reveals. No 1.20.1 runtime support is claimed.

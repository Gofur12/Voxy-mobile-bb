# Fabric 1.20.1 compatibility failure inventory — Stage 87

Branch: `android-port/stage-01-audit`
Baseline commit: `98093e5037b093975d3fa1402e94712e325ad4e4`
Evidence: GitHub Actions run [43695275781](https://github.com/Gofur12/Voxy-mobile-bb/actions/runs/43695275781), job `build-1-20-1` (job ID `132754299142`).

## Result

The Stage 86 JSpecify dependency change did not make the 1.20.1 compile pass. The default `check-does-build` workflow passed, but `check-fabric-1-20-1` failed during `:compileJava`. The current source set is still predominantly the 26.3 implementation; the 1.20.1 source set contains only a limited number of replacement classes.

## Confirmed blocker groups from the failing compiler log

1. **Modern Minecraft rendering API is referenced from shared main sources.**
   - RenderPearl packages: `com.mojang.renderpearl.api`, `com.mojang.renderpearl.backend.opengl`, and `com.mojang.renderpearl.backend.vulkan`.
   - Modern Minecraft packages absent on 1.20.1: `client.renderer.block.dispatch`, `client.renderer.extract`, `client.renderer.fog`, `client.resources.model.geometry`, and `world.level.chunk.status`.
   - Affected files include `SoftwareModelTextureBakery`, `LightMapHelper`, `MixinRenderSystem`, `MixinMinecraft`, `MixinClientLevel`, `MixinLevelExtractor`, `MixinFogRenderer`, `ReuseVertexConsumer`, `WorldImporter`, and `MixinClientCommonPacketListenerImpl`.

2. **Minecraft chunk internals differ.**
   - `MixinClientChunkCache` shadows `ClientChunkCache.Storage`, which is private in the 1.20.1 target.
   - `WorldConversionFactory` directly reads `PalettedContainer.data`, and its nested `Data.palette` / `Data.storage` members are inaccessible on this target.

3. **Modern optional-mod APIs are compiled unconditionally.**
   - Sodium `ColorSRGB` is absent from the 1.20.1 Sodium API used by this profile.
   - Iris `DepthTransformerAccessor` has a target-signature mismatch.
   - These need version-specific integration sources or a compatibility layer, not dummy API classes.

4. **Additional Minecraft/library API drift.**
   - Compiler failures also occur in `Mapper`, `VoxelIngestService`, `SaveLoadSystem3`, `Logger`, `MemoryStorageBackend`, `RocksDBStorageBackend`, `ModelFactory`, `VoxyCommands`, and `DHImporter`.
   - The CI log contains multiple unresolved symbols in these files; each must be triaged against the actual 1.20.1 mappings and dependency versions before changing behavior.

## Migration order

1. Establish a clean source boundary: keep 26.3-only rendering, mixin, and integration classes out of the 1.20.1 compile source set. Do not solve missing classes by adding fake Minecraft or RenderPearl stubs.
2. Add real 1.20.1 implementations/replacements for the chunk cache and paletted-container access, using public 1.20.1 APIs or narrowly scoped mixin accessors with verified targets.
3. Port model baking and render hooks to the 1.20.1 rendering pipeline, preserving the renderer's required behavior. Keep unsupported backend integrations disabled only where the target genuinely lacks them.
4. Port world ingestion, mapper, persistence, commands, and logging API changes.
5. Re-enable optional Sodium/Iris/Distant Horizons integrations individually with target-specific signatures.
6. Require all three checks—default build, 1.20.1 build, and PR build—before calling the corresponding stage complete. A successful Java compile is not Android runtime validation.

## Stage 87 exit criteria

This inventory is complete, but Stage 87 is an audit/triage stage only. No 1.20.1 runtime support is claimed. The next implementation stage must take one blocker group at a time and verify each change with CI.

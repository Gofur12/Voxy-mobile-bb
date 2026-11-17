# Fabric 1.20.1 model baking / render hook port — Stage 89

Baseline: CI job `build-1-20-1` (run `118397605700`), `:compileJava` failed with 117 unique errors in 11 files
(the log prints each error three times, 351 lines in total). All of them were 26.x APIs referenced from `src/main/java`.

## What changed (1.20.1 target only; `src/main` and the default 26.3 build are untouched)

Real 1.20.1 sources were added under `src/1.20.1/java` (the main copies are excluded automatically because the path matches):

| File | Errors | 1.20.1 treatment |
|---|---|---|
| `core/model/ModelFactory` | 57 | private `ChunkSectionLayer {SOLID, CUTOUT, TRANSLUCENT}` (the bake result only ever uses these three); tint via `BlockColors#getColor` probed per tint index with a small `BlockAndTintGetter` (`TintProbeGetter`) instead of `BlockTintSource`; `registryOrThrow` / `getOptional` + `ResourceLocation`; `emissiveRendering(BlockGetter, BlockPos)`; `Math.clamp` removed (Java 17) |
| `core/model/bakery/SoftwareModelTextureBakery` | 37 | atlas read through the plain GL texture id + `glGetTextureLevelParameteri` (no RenderPearl); block quads from `BlockRenderDispatcher#getBlockModel` / `BakedModel#getQuads`, layer from `ItemBlockRenderTypes#getChunkRenderType`; fluids through `BlockRenderDispatcher#renderLiquid`, layer from `ItemBlockRenderTypes#getRenderLayer`; `PoseStack#mulPose(Quaternionf)` / `mulPoseMatrix(Matrix4f)`; `Direction#getStepX/Y/Z` |
| `core/VoxyRenderSystem` | 4 | `Gui#getChat().addMessage`, `getMinSection/getMaxSection`, `RenderSystem.getProjectionMatrix()` |
| `core/rendering/util/LightMapHelper` | 3 | lightmap id from `LightTexture#lightTexture` (new line in `voxy-1.20.1.accesswidener`) |
| `core/rendering/section/backend/AbstractSectionRenderer` | 1 | `ClientLevel#getShade` instead of `CardinalLighting` |
| `ClientImportManager` | 3 | boss bar driven through `BossHealthOverlay#update(ClientboundBossEventPacket)`, chat through `Gui#getChat()` |
| `client/iris/VoxySamplers` | 4 | `addDynamicSampler(IntSupplier, String...)` (Iris 1.7.x has no `Supplier<GlSampler>` overloads / `MIPPED_NEAREST_NEAREST`) |
| `client/iris/IrisVoxyRenderPipelineData` | 4 | anonymous `SamplerHolder` implements the `GlSampler` based overloads |
| `client/iris/VoxyUniforms` | 1 | matrix suppliers are `Supplier<Matrix4f>` on Iris 1.7.x |
| `client/iris/IrisShaderPatch` | 1 | `setLenient()` instead of Gson `Strictness`; reverse-Z patching disabled |

`DepthTransformerAccessor` (Iris `DepthTransformer` does not exist in Iris 1.7.x) is excluded from the 1.20.1 compile set in
`build.gradle` and removed from `client.voxy.1.20.1.mixins.json`. Its only user was the reverse-Z shader patch, which must not run on
1.20.1 anyway (`RenderProperties#isReverseZ` is false there).

## Unverified assumptions (check on the next CI run)

Like Stage 88, this was written without a Minecraft 1.20.1 / Iris 1.7.6 classpath, from the Mojang mappings and the compiler log.

- Compile-time: `BlockAndTintGetter` (`net.minecraft.world.level`) abstract set (`getShade`, `getLightEngine`, `getBlockTint`,
  `getBlockEntity`, `getBlockState`, `getFluidState`, `getHeight`, `getMinBuildHeight`); `BossEvent` anonymous subclass;
  the Iris `SamplerHolder` overload set (every plausible overload is implemented, without `@Override`).
- Access widener: `LightTexture.lightTexture` (`DynamicTexture`) — `:validateAccessWidener` will reject it if the name is wrong.
- Not reached by javac yet: `validateAccessWidener`, `remapJar`, mixin application (runtime).

## Behaviour that intentionally differs from 26.3

- Block quads use the block's chunk layer (1.20.1 quads do not carry one); sprites are never "dark cutout".
- Fluids bake into one consumer chosen from the fluid's layer; Fabric `FluidRenderHandler` colours of modded fluids are not probed for tinting.
- `computeProjectionMat`: the raw projection is the active `RenderSystem` projection, so the "extra" (view bobbing) term is identity.
- Iris depth sampler keeps the depth texture's own sampling state; no reverse-Z shader patch.

No 1.20.1 / Android runtime support is claimed. A successful compile is not runtime validation.

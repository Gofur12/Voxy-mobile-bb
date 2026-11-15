# Renderer Path Selection — Stage 52

## Source audit

The existing render pipeline is not backend-neutral end-to-end:

- `AbstractRenderPipeline` directly imports and calls LWJGL functions for framebuffer state, depth/stencil setup, texture binding, barriers, and uniform updates.
- `NormalRenderPipeline` directly uses OpenGL 4.5 direct-state-access texture operations.
- `HiZBuffer` uses desktop direct-state-access and texture-barrier operations.
- `HiZBuffer2` adds compute dispatch, image binding, and compute shader dependencies.
- `MDICSectionRenderer` uses compute dispatch, SSBO bindings, memory barriers, indirect draw buffers, and `glMultiDrawElementsIndirectCountARB`.

Therefore, creating a GLES backend alone cannot make the current render pipelines run on Android. The pipeline and shader paths need their own GLES-compatible implementations.

## Changes

Files:
- `src/main/java/me/cortex/voxy/client/core/gl/backend/GlCapabilities.java`
- `src/main/java/me/cortex/voxy/client/core/gl/backend/GlRendererPath.java`

Added:
- `supportsGlesComputeLodPath()`: checks for an embedded context with compute shaders, SSBOs, and image load/store.
- `GlRendererPath.select(GlCapabilities)`: selects the highest capability tier:
  1. `DESKTOP_MDIC`
  2. `GLES_COMPUTE_LOD_CANDIDATE`
  3. `GLES_BASIC_RASTER_CANDIDATE`
  4. `UNSUPPORTED`

The GLES enum names deliberately say CANDIDATE. This selector is a capability decision helper only; it does not construct or dispatch to a GLES renderer.

## Implementation status

| Tier | Capability gate | Renderer implementation |
|---|---|---|
| Desktop MDIC | Existing desktop feature gate | Existing desktop renderer |
| GLES compute LOD | GLES compute + SSBO + image load/store | Not implemented |
| GLES basic raster | GLES 3.x context | Not implemented |
| Unsupported | No supported tier | No renderer |

The existing `NormalRenderPipeline` is not the GLES basic raster implementation; it still contains desktop OpenGL calls.

## Next engineering work

1. Add a GLES backend provider that can inspect an already-current Android GL context.
2. Implement a GLES raster pipeline with GLES-compatible framebuffer, texture, vertex input, and shader code.
3. Add CPU/vertex-based LOD preparation or a separately designed GLES 3.1 compute path.
4. Keep MDIC and desktop-only code unreachable from embedded contexts.
5. Add version-specific Minecraft/Fabric build targets; this repository baseline remains on Minecraft 26.3 / Java 25 and is not yet a 1.20.1 build.

## Verification

Changed source files were fetched back from the working branch. This stage did not run Gradle, compile Java, execute GPU shader tests, launch Minecraft, or test on an Android device.

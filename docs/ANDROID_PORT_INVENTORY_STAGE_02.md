# Android Port Inventory — Stage 02

Date: 2026-11-14
Baseline: `main` at `1eb84b84f25b5d7923deb832a5a186be356bb6a3`
Working branch: `android-port/stage-01-audit`

## Result

This stage inventories the renderer's concrete API dependencies. It is still an audit/documentation stage: no rendering code is changed yet. The current renderer is not GLES-ready merely by swapping LWJGL imports.

## GL dependency map

| Area | Existing implementation evidence | Android implication |
|---|---|---|
| Direct State Access (DSA) | `GlBuffer` uses `glCreateBuffers`, `glNamedBufferStorage`, `glClearNamedBufferData`; `GlTexture` uses `glCreateTextures`, `glTextureStorage2D`, `glTextureView`, `glClearTexImage`; framebuffer and VAO wrappers use named-object calls | GLES backend needs bind-to-edit equivalents or extension-gated paths. Texture views and immutable storage need explicit capability checks. |
| Persistent mapped buffers | `GlPersistentMappedBuffer` calls `glNamedBufferStorage` and `glMapNamedBufferRange` with persistent/coherent flags | Not a baseline GLES 3.x facility. Replace with staged upload/download buffers or an extension-backed implementation, with explicit ownership and fences. |
| Compute shaders / SSBO | `ShaderType.COMPUTE`; compute dispatches in `MDICSectionRenderer`; compute shaders use GLSL 4.50/4.60, std430 storage blocks, image load/store and subgroup operations | GLES 3.1 compute is not source-compatible with GLSL 4.5/4.6 desktop shaders. Shader variants and algorithmic fallbacks are required. |
| Indirect draw | `MDICSectionRenderer` calls `glMultiDrawElementsIndirectCountARB`, `glDrawElementsIndirect`, and indirect dispatch | Multi-draw indirect-count is an extension path, not a GLES baseline. A mobile backend needs CPU-generated draw batches or a verified supported indirect path. |
| GPU barriers / synchronization | `glMemoryBarrier`, sync objects, and `glGetSynciv` are used in renderer and `GlFence` | Map to supported GLES barrier/sync APIs; avoid assuming desktop entry points. Test synchronization semantics on actual drivers. |
| Texture / framebuffer | Texture storage, DSA framebuffer attachment and status checks, depth-stencil and image formats | Implement GLES binding-based FBO/texture operations and validate exact internal-format support. |
| Optional vendor features | Representative fragment test, mesh/task shaders, sparse buffers, subgroup ops, GPU memory query | These must remain optional optimizations. No backend may require them for baseline rendering. |

## Shader source findings

The inspected shader sources contain hard desktop GLSL requirements:

- `lod/gl46/prep.comp`: `#version 450`.
- `lod/gl46/cmdgen.comp`: `#version 450` and optional `GL_ARB_gpu_shader_int64`.
- `lod/hierarchical/traversal_dev.comp`: `#version 460 core`, compute workgroups and SSBOs.
- `hiz/hiz.comp`: `#version 460 core`, KHR subgroup arithmetic/basic/clustered extensions, image load/store and shared memory.
- `util/memcpy.comp`: `#version 460 core` and SSBOs.
- `lod/gl46/bindings.glsl`: multiple std430 SSBO bindings.

A GLES implementation cannot simply rewrite the `#version` line. The subgroup operations, SSBO layout, image formats, integer atomics, dispatch strategy, and memory barriers need to be reviewed as a set. The hierarchical traversal and command-generation stages are particularly central to Voxy's high-distance LOD performance.

## Java / native boundary findings

- GL wrappers are directly coupled to LWJGL static imports; a backend boundary must sit below the Voxy algorithms and above GL calls.
- `GPUSelectorWindows2.java` uses Windows GDI/Kernel JNI APIs and must be excluded from Android class loading.
- `build.gradle` currently includes LWJGL desktop natives and native-oriented LMDB/Zstd artifacts.
- `build.gradle` bundles RocksDB JNI and SQLite JDBC. These are not interchangeable with Android-safe storage automatically; their ABI support and packaging need independent verification.
- `RocksDBStorageBackend.java` is a concrete native storage integration and should be isolated behind the existing storage backend abstraction before Android support is attempted.

## Proposed backend tiers

1. **GLES 3.1 minimum candidate:** resource management, buffer uploads, FBOs, compute/SSBO only where device support is verified. This is a capability target, not a promise that current shaders already run.
2. **GLES 3.2 / extension tier:** optional enhancements after per-extension detection and shader variants exist.
3. **Zink/Kopper tier:** treat as an OpenGL translation implementation. Query the exposed context version/extensions at runtime; do not infer support from the renderer label.
4. **Desktop GL tier:** preserve current implementation and behavior; Android work must not regress it.

## Next engineering steps

- Create a backend-neutral GL capability record and resource interfaces.
- Extract current GL 4.5 wrapper calls into a desktop implementation without changing behavior.
- Define a GLES implementation with bind-based buffer/texture/FBO operations.
- Add shader feature profiles and compile-time variants. Start with a small capability probe shader before attempting LOD traversal.
- Keep the current MDIC backend desktop-only until a mobile draw backend exists.

## Acceptance criteria

- [x] Core GL wrapper classes inspected.
- [x] MDIC backend and shader requirements inspected.
- [x] Desktop/native dependencies mapped to Android risk.
- [x] A concrete backend split and implementation order documented.
- [ ] No code port or Android runtime test claimed in this stage.

# Static VAO Migration — Stage 49

## Change

Files:
- `src/main/java/me/cortex/voxy/client/core/gl/GlVertexArray.java`
- `src/main/java/me/cortex/voxy/client/core/rendering/section/backend/mdic/MDICSectionRenderer.java`

The eager class-static `GlVertexArray.STATIC_VAO = glGenVertexArrays()` field has been removed.

The identified renderer consumer was `MDICSectionRenderer`, with three bind sites:
- opaque terrain rendering,
- translucent terrain rendering,
- occlusion/culling draw setup.

That renderer now owns a `GlVertexArray` instance created after its shader construction succeeds. All three sites call the wrapper's generation-aware `bind()`; renderer `free()` releases the VAO with the other owned GPU resources.

## Lifetime behavior

- VAO creation now requires an active `GlBackendRuntime` context.
- The wrapper records the originating context generation.
- Binding through a stale context is rejected.
- The wrapper's `free()` skips GL deletion if the originating context has already been lost.
- No static integer VAO handle remains in `GlVertexArray`.

## Scope and caveats

The branch-specific audit found the three known `STATIC_VAO` uses in `MDICSectionRenderer`. GitHub's code-search API only searched the default branch during this task, so it could not independently certify every source file on this working branch. The inspected call sites were migrated, but a full branch-wide textual grep remains a desirable final check before treating the public field removal as globally verified.

This does not make MDIC Android-compatible. MDIC still depends on desktop compute shaders, SSBOs, indirect draw/count, and direct LWJGL calls. The change only removes the unsafe static VAO lifetime.

## Verification

Fetched the updated files back from the working branch and checked:
- no `STATIC_VAO` reference remains in `GlVertexArray` or `MDICSectionRenderer`;
- the renderer contains three instance-owned VAO bind calls;
- renderer cleanup frees its VAO.

No Gradle compilation, automated tests, Minecraft launch, or Android device test was run.

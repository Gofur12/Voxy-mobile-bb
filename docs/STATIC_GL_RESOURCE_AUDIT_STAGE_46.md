# Static GL Resource Audit — Stage 46

## Scope

This stage inspected renderer-side static GL/native resource declarations after the context-lifecycle work in Stages 33–45. It focused on class-load-time GL object creation and references to resettable shared owners.

## Implemented: FullscreenBlit VAO ownership

File: `src/main/java/me/cortex/voxy/client/core/rendering/post/FullscreenBlit.java`

- Removed the class-load-time `EMPTY_VAO` allocation.
- Each `FullscreenBlit` instance now creates and owns its own empty VAO after confirming an active backend/context.
- Captures the GL context generation and rejects bind/blit calls for a deleted or stale instance.
- Deletes the VAO only while its originating context generation is still active.
- Makes deletion idempotent and attempts shader cleanup even if VAO cleanup fails.
- Checks that `SharedIndexBuffer.INSTANCE_BYTE` has been initialized before issuing the blit draw.

This prevents loading the class before a GL context exists from immediately issuing `glCreateVertexArrays`, and avoids sharing one class-global VAO across independent context lifetimes.

## Remaining static GL resource findings

### LightMapHelper sampler

`LightMapHelper` still creates `LM_SAMPLER` in a static initializer using direct LWJGL calls. This remains unsafe if the class is first loaded without the intended context and has no explicit release/recreation lifecycle. It is recorded for a dedicated lifecycle migration rather than changing it without confirming Minecraft lightmap call timing and shutdown ownership.

### GlVertexArray.STATIC_VAO

`GlVertexArray.STATIC_VAO` remains a class-static VAO created through legacy `glGenVertexArrays()`. Its callers and initialization ordering need a separate call-site/lifetime audit before changing its API.

### Other static fields inspected

- `GlDebug` static fields are configuration/helpers, not owned GL object handles.
- `PrintfDebugUtil` static fields hold Java-side queues/processors, not GL object handles.
- `VoxyRenderSystem` uses static methods and external timing singleton references; the inspected declarations did not reveal another class-load-time GL object allocation.

## Verification and limitations

- Source-level review only; no Gradle compilation, automated tests, Minecraft launch, or Android device test was run.
- The new VAO remains a desktop LWJGL call; this is lifecycle hardening, not a GLES implementation.
- The renderer still has direct desktop OpenGL dependencies and no Android EGL/GLES backend.
- Static sampler and legacy static VAO work remain outstanding.

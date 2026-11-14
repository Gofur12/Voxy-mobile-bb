# Desktop Backend Adapter — Stage 04

Date: 2026-11-14
Branch: `android-port/stage-01-audit`

## Added

- `backend/desktop/DesktopGlBackend.java`
- `backend/desktop/DesktopGlBackendProvider.java`

The desktop adapter implements the Stage 03 backend contract using the existing desktop LWJGL OpenGL 4.5 context. It never creates, destroys, or detaches the Minecraft-owned context.

## Operations currently mapped

| Contract operation | Desktop implementation |
|---|---|
| Buffer create/delete/upload/bind | OpenGL 4.5 DSA + buffer binding |
| 2D texture create/storage/delete | OpenGL 4.5 DSA |
| Framebuffer create/attach/check/delete | OpenGL 4.5 DSA |
| VAO create/bind/delete | OpenGL 4.5 DSA |
| Memory barrier / finish | OpenGL 4.2 / OpenGL 1.1 |

The provider returns null if no current context exists or the context is below OpenGL 4.5.

## Important limitations

- This adapter is not yet selected by `VoxyRenderSystem`.
- Existing `GlBuffer`, `GlTexture`, `GlFramebuffer`, `GlVertexArray`, and `GlFence` wrappers still call LWJGL directly.
- The MDIC renderer remains hardwired in `VoxyRenderSystem.getRenderBackendFactory()`.
- Persistent mapped buffers, renderbuffers, shader/program lifecycle, indirect dispatch/draw, texture views, and debug labels are not yet represented completely by the backend contract.
- No Gradle build or in-game test was run in this environment. Treat compilation and runtime compatibility as unverified.

## Design correction

The first interface is intentionally a narrow initial seam, not a claim that all Voxy GPU calls are abstracted. The next migration must expand the contract around real call sites and move one resource wrapper at a time, with desktop behavior preserved before adding GLES.

## Acceptance

- [x] Desktop OpenGL provider checks for a current OpenGL 4.5 context.
- [x] Desktop adapter implements every method currently declared by `GlBackend`.
- [ ] Adapter selected by Voxy initialization.
- [ ] Resource wrappers delegate through backend.
- [ ] Build and runtime tests pass.

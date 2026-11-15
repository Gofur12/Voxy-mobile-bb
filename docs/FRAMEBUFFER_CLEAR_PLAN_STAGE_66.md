# Stage 66 — API-neutral framebuffer clear plan

## Change

Added `GlFramebufferClearPlan`, a pure-Java decision table that maps each
`GlTextureClearType` to the framebuffer attachment class and clear operation
category required by a future framebuffer-based fallback.

Mappings:

- RED_FLOAT, RG_FLOAT, RGBA_FLOAT → COLOR / COLOR_FLOAT
- RED_UNSIGNED_INT → COLOR / COLOR_UNSIGNED_INTEGER
- DEPTH_FLOAT → DEPTH / DEPTH_FLOAT
- Both depth-stencil formats → DEPTH_STENCIL / DEPTH_STENCIL_FLOAT_UNSIGNED_INTEGER

## Deliberate boundary

This is not an FBO implementation and does not issue GL calls. It does not
claim that a texture format is renderable on GLES. A concrete backend must still
check format renderability and framebuffer completeness, preserve and restore
all modified state, and issue the matching API-specific clear operation.

The current repository tree has only DesktopGlBackend as a GlBackend
implementation. There is no Android GLES backend in this branch, so adding
LWJGL desktop FBO calls and labelling them GLES would be incorrect.

## Validation status

Source was committed and fetched back from the working branch. No Gradle
compilation, automated tests, GL-driver test, Minecraft launch, or Android
device test was run.

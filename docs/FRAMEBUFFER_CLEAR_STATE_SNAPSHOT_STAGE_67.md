# Stage 67 — framebuffer clear state snapshot

## Change

Added the immutable, API-neutral `GlFramebufferClearState` value object for
state a temporary framebuffer clear may modify:

- draw and read framebuffer bindings;
- full draw-buffer selection and read-buffer selection;
- scissor enable state and rectangle;
- RGBA color write mask;
- depth write mask;
- front and back stencil write masks.

The draw-buffer array is defensively copied on input and output. Negative
scissor dimensions and an empty draw-buffer selection are rejected.

## Backend obligations

A concrete backend must capture native state before changing it, then restore
every changed value in a `finally` path. This snapshot is only the data model:
it does not query GL, bind an FBO, clear a texture, or prove that a format is
renderable. The backend must still handle API-specific state semantics and
validate framebuffer completeness.

## Current limitation

There is still no GLES backend in this repository branch. No desktop calls are
being presented as GLES support.

## Validation

The source and this document were committed and fetched back from
`android-port/stage-01-audit`. No Gradle compile, unit test, graphics-driver
test, Minecraft launch, or Android device test was run.

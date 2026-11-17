# Stage 63 — Semantic texture-clear attachment metadata

## Change

Extended `GlTextureClearType` with backend-neutral metadata:

- `Attachment.COLOR`, `Attachment.DEPTH`, or `Attachment.DEPTH_STENCIL`
- `ValueType.FLOAT`, `ValueType.UNSIGNED_INTEGER`, or `ValueType.NONE`
- `attachment()` and `valueType()` accessors

The metadata is derived from the existing semantic clear categories and contains no LWJGL or GLES constants.

## Why this boundary

A future GLES backend can select the appropriate framebuffer attachment and typed clear entry point without duplicating a fragile switch over enum names:

- Color + FLOAT: floating-point color clear operation.
- Color + UNSIGNED_INTEGER: unsigned-integer color clear operation.
- DEPTH: depth clear operation.
- DEPTH_STENCIL: combined depth/stencil clear operation.

The existing desktop backend continues to map the enum to `glClearTexImage`; this change does not alter its behavior.

## Limitations

This is metadata only. It does not create a GLES backend, framebuffer, EGL context, Android module, or MobileGlues integration. It does not prove that every internal texture format is renderable as a GLES framebuffer attachment; a GLES backend must still check actual context support and framebuffer completeness.

## Validation

Source and documentation are fetched back from the working branch after commit. No Gradle compile, automated test, GL-driver test, Minecraft launch, or Android-device test has been run.

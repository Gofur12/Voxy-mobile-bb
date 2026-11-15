# Stage 61 — Typed Texture Clear Contract

## Changes

Introduced `GlTextureClearType`, a backend-neutral semantic enum for zero-clearing supported texture formats:

- `RED_FLOAT`
- `RG_FLOAT`
- `RGBA_FLOAT`
- `RED_UNSIGNED_INT`
- `DEPTH_FLOAT`
- `DEPTH_STENCIL_UNSIGNED_INT_24_8`
- `DEPTH_STENCIL_FLOAT32_UNSIGNED_INT_24_8`

Changed `GlBackend.clearTextureImage` to accept a semantic clear type rather than a caller-supplied GL transfer format, type, and raw data pointer. The desktop backend maps each semantic type to its OpenGL transfer representation and continues to use `glClearTexImage` with a zero value.

Updated `GlTexture.zero()` to select a semantic clear type from the texture's internal format, then clear each mip level through the active backend. This moves format-to-transfer mapping out of the wrapper and gives a future GLES backend a clear, typed contract to implement with its own supported operations.

Also corrected the wrapper's existing `GL_DEPTH32F_STENCIL8` bookkeeping: it now reports the depth-stencil transfer format and accounts for eight bytes per texel in size estimation. Previously the zero path named this format, but allocation-size estimation and transfer-format lookup did not support it.

## Compatibility notes

- This changes the `GlBackend` interface. Any additional backend provider must implement the new method signature.
- The semantic enum contains no LWJGL or platform types.
- Desktop behavior remains routed through OpenGL 4.5 `glClearTexImage`.
- This stage does **not** implement a GLES framebuffer-clear or CPU-upload fallback.
- GLES format support, renderability, and state restoration remain to be implemented and validated per device/context.

## Validation status

- Updated source files and the new enum were committed to `android-port/stage-01-audit`.
- The source changes were fetched back from the branch for verification.
- No Gradle compilation, automated test, GL driver test, Minecraft launch, GitHub Actions run, or Android-device test was performed.
- Because no build was run, Java compilation and exact LWJGL constant availability remain unverified.

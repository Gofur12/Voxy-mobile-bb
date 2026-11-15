# Bind-Based Texture Allocation — Stage 59

## Change

Updated `DesktopGlBackend.allocateTexture2D(...)` to allocate immutable 2D storage with the bind-based `glTexStorage2D` API rather than desktop OpenGL 4.5's direct-state-access `glTextureStorage2D`.

The adapter now:
1. validates positive mip count and dimensions;
2. saves the current `GL_TEXTURE_BINDING_2D`;
3. binds the target texture;
4. allocates immutable storage;
5. restores the previous binding in a `finally` block.

The call is made through the OpenGL 4.2 API entry point in the desktop implementation. The same operation has a GLES 3.0 counterpart, so a future GLES backend can implement this contract without DSA.

## Format audit notes

The existing `GlTexture` wrapper recognizes these internal formats: `GL_RGBA8`, `GL_RG16F`, `GL_R32UI`, `GL_R32F`, `GL_DEPTH_COMPONENT24`, `GL_DEPTH_COMPONENT32F`, `GL_DEPTH_COMPONENT32`, and `GL_DEPTH24_STENCIL8`. This stage does not certify every format/type combination on GLES devices; those need explicit GLES capability/extension checks and device-driver tests before being enabled.

## Scope and limitations

This removes one OpenGL 4.5 DSA dependency from the texture allocation path. It does not add a GLES backend, Android module, EGL integration, MobileGlues adapter, or a complete renderer. Texture views and texture clears still need separate fallback work.

## Validation

The modified source was fetched back from `android-port/stage-01-audit`. No Gradle compilation, unit tests, shader compilation, Minecraft launch, GitHub Actions run, or Android-device test was performed.

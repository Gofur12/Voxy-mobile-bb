# Uniform Vec2 Backend Stage 14

## Scope

This stage adds the two-component floating-point uniform operation required by the Hi-Z compute mip pass and migrates the remaining direct uniform calls in `HiZBuffer2`.

## Changes

- Added `GlBackend.setUniform2f(int location, float x, float y)`.
- Implemented the operation in `DesktopGlBackend` using `GL20C.glUniform2f`.
- Migrated the sampler integer uniform in `HiZBuffer2` to `GlBackendRuntime.requireBackend().setUniform1i(0, 0)`.
- Migrated the inverse texture-size vector uniform in `HiZBuffer2` to `GlBackendRuntime.requireBackend().setUniform2f(0, 1f / width, 1f / height)`.

## Compatibility notes

- Both setters preserve the original current-program semantics. The target shader must already be bound.
- Uniform locations remain the existing explicit location `0`; this stage does not change shader declarations or add uniform-location lookup.
- The scope is the two uniform call sites in `HiZBuffer2`. It does not claim that every uniform call across Minecraft, Iris, or all Voxy renderers has been migrated.
- The desktop backend remains OpenGL 4.5-only. No GLES implementation is included.

## Verification status

- Changes were committed to `android-port/stage-01-audit`.
- Source-level verification confirms both `HiZBuffer2` uniform calls now route through the backend.
- No Gradle compilation, automated test, desktop runtime test, or Android/Minecraft runtime test has been run.

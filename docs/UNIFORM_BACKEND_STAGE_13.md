# Uniform Backend Stage 13

## Scope

This stage moves the existing integer-uniform operation used by the Hi-Z mip-chain pass behind the backend-neutral graphics contract.

## Changes

- Added `GlBackend.setUniform1i(int location, int value)`.
- Implemented the operation in `DesktopGlBackend` with `GL20C.glUniform1i`.
- Migrated `HiZBuffer.buildMipChain` from a direct LWJGL `glUniform1i(0, 0)` call to `GlBackendRuntime.requireBackend().setUniform1i(0, 0)`.

## Compatibility notes

- The operation intentionally targets the currently bound program, matching the original `glUniform1i` semantics. Callers must bind the intended shader before setting the value.
- The location remains the existing explicit location `0`; this stage does not add uniform-location lookup or change shader source declarations.
- Only the integer setter needed by the inspected Hi-Z call site is added. Float, vector, matrix, and program-direct uniform APIs are not claimed as migrated.
- The desktop implementation still requires the existing OpenGL 4.5 backend. No Android/GLES backend is provided by this stage.

## Verification status

- Source changes were committed to `android-port/stage-01-audit`.
- No Gradle compilation, automated tests, desktop runtime test, or Android/Minecraft runtime test has been run.
- The Hi-Z rendering path is therefore not yet verified end-to-end.

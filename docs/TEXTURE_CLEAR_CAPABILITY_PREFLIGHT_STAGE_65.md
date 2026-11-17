# Stage 65 — Texture Clear Capability Preflight

## Goal

Make texture clearing fail before modifying any mip level when the active backend cannot support the required semantic clear operation.

## Changes

- Added `GlBackend.supportsTextureClear(GlTextureClearType)`.
- The interface default returns `false`, so new or external providers opt in explicitly rather than inheriting an unsafe assumption.
- `DesktopGlBackend` reports support for the semantic clear categories handled by its existing OpenGL 4.5 `glClearTexImage` implementation.
- `GlTexture.zero()` now checks support once before entering its mip-level loop and throws an explanatory `UnsupportedOperationException` when unsupported.

## Why this is useful for GLES

A future GLES backend can implement this capability check based on its supported attachment classes, format renderability, and clear path. It can reject an unsupported category before partially clearing a multi-level texture.

This stage does not implement the GLES framebuffer clear itself. The repository still has no GLES backend or Android EGL integration, and the current desktop framebuffer methods use OpenGL 4.5 DSA calls. A real GLES implementation must provide its own bind-based FBO/state-preserving path and report only formats it can actually clear.

## Validation status

- Source changes committed to `android-port/stage-01-audit`.
- Source must still be compiled and tested; no Gradle build, automated tests, GL-driver test, Minecraft launch, GitHub Actions run, or Android-device test was performed for this stage.

# Renderer Feature-Level Gate — Stage 57

## Purpose

Keep backend capability detection separate from renderer availability. A GLES context may expose GLES 3.x features, but that does not make Voxy's desktop renderer portable to GLES.

## Changes

- `GlRendererPath.isImplemented()` currently returns true only for `DESKTOP_MDIC`.
- `GlRendererPath.selectImplemented(GlCapabilities)` first evaluates the capability candidate and then rejects tiers whose renderer implementation is not present.
- `VoxyClient.initVoxyClient()` performs this gate immediately after backend discovery and before `Capabilities.init()` or any Voxy GPU resource initialization.
- The existing desktop MDIC capability gate and initialization sequence remain unchanged.

## Current support status

| Backend tier | Capability candidate | Implemented | Startup |
| --- | --- | --- | --- |
| Desktop OpenGL MDIC | Yes, when required features exist | Yes | May continue through the existing desktop checks |
| GLES compute LOD | Candidate only | No | Rejected |
| GLES basic raster | Candidate only | No | Rejected |
| Other | Unsupported | No | Rejected |

This is intentionally conservative. The repository still has no complete GLES renderer, EGL integration, Android module, or MobileGlues adapter. This stage does not enable Android rendering.

## Why the gate is before legacy capabilities

The legacy `Capabilities` constructor has desktop LWJGL probes in its non-embedded path and only a partial embedded snapshot path. Rejecting unimplemented GLES tiers before constructing it avoids treating an API-level candidate as a usable renderer and avoids entering desktop-oriented initialization for a GLES-only context.

## Validation limits

Source edits were committed to `android-port/stage-01-audit`. No Gradle compilation, unit tests, shader compilation, Minecraft launch, GitHub Actions run, or Android-device test was performed. This stage is a source-level startup guard, not a verified build.

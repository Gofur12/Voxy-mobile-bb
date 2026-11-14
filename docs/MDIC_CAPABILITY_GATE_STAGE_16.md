# MDIC Capability Gate Tightening — Stage 16

## Change

Tightened `GlCapabilities.supportsDesktopMdicPath()` to reuse
`supportsComputeLodPath()`. The desktop MDIC path now requires all of:

- A non-embedded desktop GL profile
- Compute shaders
- Shader storage buffer objects (SSBO)
- Image load/store
- Multi-draw indirect
- Indirect draw parameters

Previously, the predicate checked compute shaders, SSBO, multi-draw indirect,
and indirect parameters, but omitted image load/store even though the
capability model already identifies it as a required part of the compute LOD
path.

## Why this matters

A capability predicate must fail closed. Reporting the MDIC path as supported
when image load/store is unavailable could let a renderer attempt to construct
a pipeline that cannot execute its required GPU work.

## Scope and limitations

- This changes the capability predicate only.
- The predicate is not yet wired into `VoxyRenderSystem` renderer selection.
- No GLES renderer, CPU fallback, or Android provider is implemented here.
- The current MDIC renderer still contains direct desktop OpenGL calls and
  compute/indirect-draw dependencies.
- This is not evidence that Voxy now runs on Android.

## Verification

- Reviewed `GlCapabilities`, `DesktopGlBackend`,
  `MDICSectionRenderer`, and `VoxyRenderSystem` on
  `android-port/stage-01-audit`.
- Confirmed the updated predicate delegates the compute/SSBO/image-load-store
  requirements to `supportsComputeLodPath()`.
- Gradle compilation, automated tests, and Minecraft runtime tests were not run.

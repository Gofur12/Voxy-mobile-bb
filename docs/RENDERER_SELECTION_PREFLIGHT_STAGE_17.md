# Renderer Selection Preflight — Stage 17

## Change

`VoxyRenderSystem.getRenderBackendFactory()` no longer returns the MDIC
factory unconditionally.

It now:

1. Requires a current LWJGL OpenGL context.
2. Requires desktop OpenGL 4.5 because the current MDIC path depends on
   desktop DSA operations.
3. Builds a `GlCapabilities` snapshot from the current LWJGL context flags.
4. Uses `supportsDesktopMdicPath()` to check compute shaders, SSBO,
   image load/store, multi-draw indirect, and indirect parameters.
5. Fails with an explicit unsupported-renderer message if the requirements
   are not met.

The selected factory is now resolved at the start of the
`VoxyRenderSystem` constructor, before SSBO state queries and GPU resource
construction.

## Why fail instead of silently falling back?

There is no GLES-compatible section renderer in this branch yet. Returning
MDIC on an unsupported context would be misleading and likely fail later with
a lower-level GL error. This preflight makes the limitation explicit and
prevents the known MDIC path from being selected on GLES.

## Limitations

- This does not implement an Android/GLES renderer or CPU fallback.
- The current code still depends on LWJGL and Minecraft's desktop GL setup.
- Other parts of Voxy may perform graphics initialization before this
  constructor is reached.
- Capability flags are checked, but no shader compile or feature runtime probe
  is performed here.
- No Gradle build, automated test, or Minecraft runtime test was run.

## Verification

- Reviewed the constructor order and confirmed factory selection occurs before
  the SSBO binding queries.
- Confirmed the factory gate delegates the feature requirements to
  `GlCapabilities.supportsDesktopMdicPath()`.

# Conventional Geometry Allocation Mode — Stage 19

## Change

`BasicSectionGeometryData` now has an explicit allocation policy through
constructor overloads:

- Existing `BasicSectionGeometryData(maxSections, capacity)` preserves the
  legacy sparse-buffer-enabled behavior.
- New `BasicSectionGeometryData(maxSections, capacity, false)` requests one
  conventional geometry-buffer allocation and does not probe desktop GPU
  vendor/memory capabilities or attempt ARB sparse commitment.
- The external-buffer constructor also has an explicit `allowSparseBuffers`
  overload. Its original signature preserves legacy sparse-buffer handling;
  callers selecting a GLES path must explicitly pass `false`.

The conventional mode still uses the existing `GlBuffer` abstraction for
metadata and geometry storage. Its backend must therefore implement buffer
creation/upload in a GLES-compatible way before this mode can run on Android.

## Safety checks

- Reject non-positive section counts.
- Reject non-positive geometry capacity.
- Require geometry capacity to remain 8-byte aligned (the existing packed
  quad format).
- In conventional mode, sparse commitment and desktop GPU-memory telemetry
  are bypassed.
- Existing constructor behavior is retained for the current desktop-owned
  geometry allocation path.

## Not yet complete

- `VoxyRenderSystem` still constructs the legacy external geometry buffer
  and MDIC renderer; it does not select the new conventional mode.
- `GlBuffer`'s current desktop backend uses desktop DSA allocation; a GLES
  implementation is still required.
- The renderer's geometry upload and draw path still requires migration.
- No Gradle compilation, unit tests, or Minecraft runtime tests were run.

This stage creates a real allocation-policy seam, not an Android-ready
renderer claim.

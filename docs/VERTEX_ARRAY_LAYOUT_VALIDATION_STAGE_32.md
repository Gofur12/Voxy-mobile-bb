# Vertex-array layout validation — Stage 32

## Scope

Reviewed `GlVertexArray` against the backend-neutral vertex-array contract introduced earlier. This stage hardens wrapper input validation and documents the remaining desktop-only initialization path. It does not claim that a GLES backend exists.

## Changes

- `setStride` rejects zero and negative strides.
- `bindBuffer` now fails clearly if a positive stride has not been configured.
- Attribute indices must be nonnegative.
- Attribute component counts are restricted to the OpenGL-supported range of 1–4.
- Attribute byte offsets must be nonnegative.
- Attribute indices are recorded only after the backend successfully enables and configures the attribute, avoiding stale wrapper state after a backend failure.
- Existing backend methods remain responsible for translating the vertex-array operations to the active graphics API.

## Remaining portability blockers

- `STATIC_VAO` is still created by a static LWJGL `glGenVertexArrays()` call. It can initialize before `GlBackendRuntime` has a backend/current context, so it cannot safely be switched to runtime backend creation without first auditing every caller and the client renderer initialization order.
- The desktop backend implementation uses OpenGL 4.5 DSA for vertex-array setup. A GLES 3.x backend must implement the contract using bind-to-edit calls (bind VAO, bind array buffer, configure attribute pointer/divisor as needed, bind element buffer), while preserving VAO state semantics.
- This wrapper does not yet track per-attribute component type sizes, so it cannot verify that each attribute's offset plus byte width fits inside the configured stride. That requires a type-size contract shared with backend implementations.
- No GLES backend, CPU geometry renderer, or Android renderer was added in this stage.

## Validation status

- Updated source and this document were committed and fetched back from the target branch.
- No Gradle compilation, automated tests, desktop runtime test, Android runtime test, or Minecraft gameplay test was run.

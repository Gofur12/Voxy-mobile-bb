# Stage 09 — Vertex array backend migration

## Changes

- Expanded `GlBackend` with vertex-buffer binding, element-buffer binding, attribute enablement, integer attribute format, and floating-point attribute format operations.
- Implemented the operations in `DesktopGlBackend` using OpenGL 4.5 DSA.
- Migrated `GlVertexArray` instance creation, bind, buffer setup, attribute setup, and deletion through `GlBackendRuntime`.
- Preserved the wrapper's existing attribute-index tracking and stride behavior.

## Remaining legacy path

- `STATIC_VAO` remains initialized through the original static `glGenVertexArrays()` call. It is a class-initialization-time global handle and cannot safely be routed through `GlBackendRuntime` without first resolving backend initialization order and its ownership/lifetime. It is intentionally not silently replaced with a different handle.
- The desktop provider is still the only registered backend; this is not yet Android GLES support.

## Verification

- Source edits were reviewed against the current `GlVertexArray` methods.
- No Gradle compilation, automated test, or Minecraft runtime test has been run.
# Stage 68 — Framebuffer Clear State Scope

## Change

Added `GlBackend.captureFramebufferClearState()` and
`GlBackend.restoreFramebufferClearState(GlFramebufferClearState)` as optional
backend operations. Their default implementations throw
`UnsupportedOperationException`, so an existing backend cannot accidentally
claim that it supports a state-preserving framebuffer fallback.

Added `GlFramebufferClearStateScope`, an `AutoCloseable` helper intended for
Java try-with-resources. Opening the scope captures the backend snapshot;
closing it restores that snapshot once. Java guarantees that try-with-resources
invokes `close()` when the scoped operation exits exceptionally. If both the
operation and restoration fail, Java records the restoration failure as a
suppressed exception.

## Intended use

A future backend implementation can wrap the temporary framebuffer sequence:

```java
try (GlFramebufferClearStateScope ignored =
             GlFramebufferClearStateScope.open(backend)) {
    // Bind temporary FBO, attach target mip, validate completeness, clear.
}
```

The scope itself does not bind an FBO, attach a texture, validate completeness,
or issue a clear command.

## Current limitations

- `DesktopGlBackend` does not yet implement capture/restore; the interface
  defaults reject scope opening. This stage does not introduce unverified
  LWJGL state-query code.
- No GLES/MobileGlues backend exists yet.
- `clearTextureImage` is not wired to this scope.
- No Gradle compilation, unit tests, Minecraft launch, renderer test, or
  Android-device test was run for this stage.

## Next

Implement and verify a concrete state capture/restore adapter for a specific
graphics profile, then integrate it with a temporary-FBO clear path only after
framebuffer formats, draw-buffer behavior, and state restoration are validated.

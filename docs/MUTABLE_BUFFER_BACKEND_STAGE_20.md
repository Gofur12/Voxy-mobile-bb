# Stage 20 — Mutable Buffer Allocation and Upload

## Scope

This stage separates ordinary buffer allocation/upload from desktop-only
OpenGL 4.5 direct-state-access (DSA) calls. It is a portability step for the
conventional geometry allocation path introduced in Stage 19, not a complete
GLES backend.

## Changes

- `GlBackend.createBuffer` now documents that `storageFlags == 0` means
  ordinary mutable storage, suitable for a GLES 3.x implementation using
  `glBufferData`.
- `DesktopGlBackend.createBuffer` uses the legacy bind-to-edit allocation
  path (`glGenBuffers`, `glBindBuffer`, `glBufferData`) when flags are zero.
- Non-zero storage flags retain immutable DSA allocation. This preserves the
  current desktop sparse/persistent-mapping path rather than silently changing
  its semantics.
- `DesktopGlBackend.uploadBuffer` now uses bind-to-edit
  `glBufferSubData`, preserving and restoring the prior array-buffer binding.
  This operation is available in GLES 3.x.
- Buffer binding state is restored in `finally`, including when allocation or
  upload throws.

## Important remaining blockers

This does **not** make Voxy runnable on GLES yet:

1. `GlBuffer.zero()` and `fill()` still call `clearBuffer`, whose desktop
   implementation uses OpenGL 4.5 named-buffer clear. GLES 3.0 does not provide
   that operation as a core feature.
2. `GlPersistentMappedBuffer` still depends on persistent mapping and raw
   mapped pointers. The existing `UploadStream` uses named-buffer flush/copy,
   fences, and memory barriers; it needs a separate staging/upload fallback.
3. The repository currently has no GLES provider/implementation or Android
   context bootstrap. Its Gradle configuration is still Minecraft 26.3,
   Java 25, and the desktop Fabric/LWJGL build.
4. Shader, texture, framebuffer, VAO, and renderer paths still contain
   desktop-only calls and feature requirements.

## Verification

Source-level changes were committed. No Gradle compilation, automated test,
desktop runtime test, Android runtime test, or Minecraft gameplay test was run
for this stage. Treat the changes as uncompiled until a real build is executed.

## Next work

Implement a portable buffer-clear/zero-fill strategy, then introduce a GLES
backend/provider only after confirming the Android launcher's actual GL
binding and context integration. The 1.20.1/Java 17 target also needs its own
version-specific build line; this branch currently targets 26.3/Java 25.

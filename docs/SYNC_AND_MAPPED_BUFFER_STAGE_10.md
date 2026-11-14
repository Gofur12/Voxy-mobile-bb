# Stage 10 — Fence and persistent mapped buffer backend migration

## Changes

- Added backend operations for mapping/unmapping buffer ranges and creating, polling, and deleting GPU fences.
- Implemented those operations in `DesktopGlBackend` using named-buffer mapping and OpenGL sync objects.
- Migrated `GlFence` away from a native scratch allocation plus `glGetSynciv`; polling now uses a zero-timeout client wait and preserves non-blocking behavior.
- Migrated `GlPersistentMappedBuffer` storage creation, mapping, unmapping, and deletion through the backend.
- Added a failed-map guard that deletes the just-created buffer before throwing.

## Android implications

- This is a contract boundary, not an Android implementation. GLES backends may need a different policy for persistent mapping: `GL_EXT_buffer_storage` is optional and ordinary map/unmap or staging-buffer strategies may be required.
- The current wrapper still exposes a raw mapped address, so any backend that cannot provide a stable mapped pointer cannot satisfy this exact API without a higher-level upload/ring-buffer redesign.
- Fence handles are opaque `long` values; the Android backend must implement compatible sync semantics or a deliberate fallback.

## Verification

- Source edits were reviewed against the existing wrapper API.
- No Gradle build, automated test, or Minecraft runtime test has been run.
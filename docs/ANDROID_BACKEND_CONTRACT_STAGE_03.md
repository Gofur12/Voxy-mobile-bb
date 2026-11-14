# Renderer Backend Contract — Stage 03

Date: 2026-11-14
Branch: `android-port/stage-01-audit`

## Files added

- `GlBackend.java`: backend-neutral resource and synchronization contract.
- `GlCapabilities.java`: immutable capability snapshot with feature predicates.
- `GlBackendProvider.java`: provider contract for an already-current GL context.
- `GlBufferUsage.java`: backend-neutral usage hints.

## Design constraints

- No LWJGL imports or platform-specific types appear in these contracts.
- The capability snapshot is Java 17-compatible so it can be shared with the Minecraft 1.20.1 target line.
- Capability flags represent verified context support, not guesses based on a renderer name.
- Desktop MDIC eligibility is explicitly separated from embedded-profile contexts.
- Providers do not create or own the Minecraft renderer context.

## Current limitations

This is the first contract scaffold, not a completed renderer migration. Existing `GlBuffer`, `GlTexture`, `GlFramebuffer`, `GlVertexArray`, `GlFence`, and `MDICSectionRenderer` still call LWJGL directly. No provider is registered, no backend is instantiated, and no gameplay rendering path uses these interfaces yet.

The contract also does not yet expose shader compilation, draw submission, query objects, texture upload formats, or mapped-buffer lifetime. These must be added after tracing the concrete call sites rather than guessed now.

## Acceptance

- [x] Backend contracts added in a separate package.
- [x] Contracts contain no LWJGL references.
- [x] Capability predicates distinguish embedded and desktop paths.
- [ ] Existing desktop wrappers migrated behind the contract.
- [ ] GLES provider implemented and tested.
- [ ] Build and in-game verification completed.

## Next stage

Stage 04 will map each existing wrapper operation onto the contract and extend the contract only for concrete missing operations. Desktop behavior must remain unchanged while the boundary is introduced.

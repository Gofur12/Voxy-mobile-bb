# Buffer Contract Expansion — Stage 05

Date: 2026-11-14
Branch: `android-port/stage-01-audit`

## Changes

Expanded `GlBackend` with the exact operations required by the existing `GlBuffer` implementation:

- Create immutable buffer storage with the original initial-data pointer and storage flags.
- Clear a whole buffer with the original internal format, format, type, and data pointer.
- Clear a buffer subrange with the original offset and size.
- Set pixel-store state used by the existing fill path.

Updated `DesktopGlBackend` to implement those operations using the corresponding OpenGL 4.5 DSA calls.

## Why the first buffer API was corrected

The original Stage 03/04 `createBuffer(size, usageFlags)` proposal was not equivalent to Voxy's actual constructor. Voxy passes raw storage flags, may supply initial data, and uses sparse-storage flags. Mapping that to a generic usage hint would silently change allocation semantics and could break large geometry/LOD buffers. This stage replaces that incomplete abstraction with an operation matching the real call site.

## Deliberately not changed

`GlBuffer.java` still calls LWJGL directly. Switching it to the new interface before a backend lifecycle/installation point exists would cause initialization-order failures and could route allocations to the wrong context. The desktop adapter is not yet wired into Voxy startup.

## Acceptance

- [x] Backend contract models original buffer storage arguments.
- [x] Whole-buffer and range clear operations are represented.
- [x] Pixel-store operation is represented.
- [x] Desktop adapter maps the new methods to GL 4.5 DSA.
- [ ] GlBuffer delegates through a selected backend.
- [ ] GLES buffer implementation and sparse-buffer fallback exist.
- [ ] Build and runtime tests pass.

## Next

Stage 06: add a backend lifecycle/selection point that is initialized only after Minecraft's GL context is current, then migrate `GlBuffer` without changing its public constructors or accounting behavior.

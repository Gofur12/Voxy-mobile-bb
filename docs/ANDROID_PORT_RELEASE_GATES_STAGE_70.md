# Stage 70 — Android Port Release Gates and Critical Path

## Why this audit exists

The repository has accumulated backend abstractions, but those changes are not
an Android renderer. This stage records the concrete blockers found in the
current branch so progress is measured by runnable milestones rather than
commit or stage count.

## Verified blockers in the current source

### 1. The repository has a separate Minecraft 1.20.1 build target

The default `gradle.properties` profile selects Minecraft `26.3`, Fabric
Loader `0.19.5`, Fabric API `0.160.5+26.3`, and Java compile release `25`.
The Gradle build now provides an isolated `-PvoxyTarget=1.20.1` profile with
Minecraft `1.20.1`, Fabric Loader `0.19.2`, Fabric API `0.92.8+1.20.1`,
Sodium `0.5.13`, Iris `1.7.6`, and Java compile release `17`.

The `check-fabric-1-20-1.yml` workflow builds this target separately. CI
success confirms compilation and artifact packaging only; it does not confirm
that the mod loads, enters a world, or renders on Android.

### 2. The active renderer remains desktop-only

`GlRendererPath.isImplemented()` returns true only for
`DESKTOP_MDIC`. GLES paths are candidates, not implementations.
`VoxyClient.initVoxyClient()` rejects candidates without an implemented
renderer before legacy renderer initialization.

### 3. Desktop API calls remain in the resource wrappers and backend

`GlTexture.zero()` delegates to `clearTextureImage`, whose desktop
implementation uses `glClearTexImage`. `GlFramebuffer` delegates to methods
whose current desktop implementation uses OpenGL 4.5 direct-state-access
operations for framebuffer creation, attachment, draw buffers, and status.
Other backend methods also retain desktop-only calls (persistent mapping,
texture views, buffer clears, VAO DSA, barriers, compute/indirect operations).

### 4. The renderer's feature requirements are beyond GLES 3.0

The existing Voxy path relies on compute, SSBO, image load/store, indirect
draw/parameter operations, and desktop shader profiles. GLES 3.0 cannot
directly provide all of these. A GLES 3.0-compatible path therefore needs
algorithm and shader changes, not just alternate GL function names.

### 5. Stage 69 is not yet wired into texture clearing

Desktop state capture/restore has been added, but no temporary-FBO fallback
uses it. The desktop backend still uses direct texture clear. No GLES FBO clear
implementation is present.

### 6. Default storage still depends on desktop/native libraries

`StorageConfigUtil.createDefaultSerializer()` selects RocksDB plus the LWJGL
Zstandard compressor. The repository also exposes LMDB and a native LZ4
compressor. The current build packages desktop-oriented native artifacts and
does not provide a verified Android/Bionic implementation for these storage
paths. Switching to `MemoryStorageBackend` would avoid some native calls but
would discard persisted LOD data between sessions, so it is not an acceptable
silent fallback.

An Android release needs a tested persistent Android-compatible storage
backend and a compatible compressor, or verified Android ABI builds of every
native dependency used by the selected configuration.

### 7. LWJGL host-version compatibility is unresolved

The build declares LWJGL `3.4.1`, while the Android launcher environment
previously reported LWJGL `3.3.3`. Bundled LWJGL extension modules and native
libraries must be aligned with the host-provided core binding. A successful
desktop CI build does not validate this Android class/native compatibility.

## Release milestones

These are completion gates, not promises of fixed calendar dates:

1. **Build separation:** preserve the current desktop target and maintain the
   independent Fabric 1.20.1 profile. CI compilation is established; in-game
   compatibility and Android runtime validation remain open.
2. **Portable resource layer:** implement and compile the GLES-safe subset for
   buffers, textures, framebuffer/renderbuffer operations, shaders, and VAOs.
   Remove or gate desktop-only calls from every code path reachable by the
   GLES renderer.
3. **Android context integration:** add a tested provider for the launcher-owned
   current EGL/GLES context. It must not create, destroy, or switch the
   launcher's context.
4. **First visible LOD renderer:** implement a basic raster LOD path with
   terrain upload, camera transforms, depth handling, culling, and correct
   resource lifetime. Verify it on a real Android device.
5. **Voxy-quality features:** add distance/quality controls, streaming,
   background generation/upload, stable chunk transitions, memory budgets,
   and performance tuning. Only then compare settings and visual quality with
   desktop Voxy.
6. **Version expansion:** port and validate each additional Minecraft/Fabric
   version independently; do not claim universal compatibility from one build.

## Definition of done

A release is not complete until the intended Minecraft/Fabric target builds,
the mod loads in an Android launcher with the selected renderer, enters a
world, renders distant LODs beyond vanilla render distance, survives world
reload and shutdown, exposes working distance/quality settings, and passes
repeatable runtime checks on the target GPU.

## Current status

- Backend contracts and desktop lifecycle groundwork: in progress.
- Concrete GLES backend: not implemented.
- Android EGL provider: not implemented.
- Android LOD renderer: not implemented.
- Fabric 1.20.1 build target: established; CI build has passed on recent commits.
- Android-only runtime guards: Windows GPU selection mixin and desktop CPU
  topology probes are skipped on Android/non-Windows as appropriate.
- 1.20.1 mixin compatibility: BlockableEventLoop mixin omitted from the 1.20.1
  config; login-session hook uses the stable `handleLogin` tail.
- Upload stream: pending copies are committed before allocation-pressure
  waiting; integer alignment overflow is rejected.
- Optional graphics providers: runtime linkage failures are handled as an
  unavailable provider rather than crashing provider selection.
- Android package/runtime validation: not performed.

No completion date can responsibly be estimated until milestones 1–4 have
working builds and a first visible LOD result. The next engineering priority is
to establish the separate 1.20.1 build target and the GLES-safe resource
surface, rather than continuing to add abstraction-only stages.

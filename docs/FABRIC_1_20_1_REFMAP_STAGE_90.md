# Fabric 1.20.1 mixin refmap — Stage 90

Baseline: Stage 89 sources; first runtime log of the 1.20.1 jar (Voxy `0.2.20-beta`, Fabric Loader 0.19.5,
Zalith Launcher 2.8, MobileGlues renderer, Android 13).

## Observed crash (runtime, not compile)

`MixinMinecraft` (`client.voxy.1.20.1.mixins.json`) fails to apply and Minecraft exits during startup:

```
Critical injection failure: @Inject annotation on voxy$injectWorldClose could not find any targets
matching 'disconnect(Lnet/minecraft/client/gui/screens/Screen;)V' in net/minecraft/class_310. No refMap loaded.
```

- The game runs in Fabric **intermediary** (`class_310`); the mixin targets are written in **Mojang** names.
- `No refMap loaded` means Mixin saw no `refmap` for that config, so nothing translated
  `disconnect(Lnet/minecraft/client/gui/screens/Screen;)V` to its intermediary form.
- Every Mojang-named `@Inject`/`@Shadow` target in `client.voxy.1.20.1.mixins.json` and
  `common.voxy.1.20.1.mixins.json` (`MixinWorld`) has the same problem. `MixinMinecraft` is just the first one the
  game loads.

Also seen in the same log (harmless, but wrong for this target):

```
@Mixin target net/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobQueue was not found
client.voxy.1.20.1.mixins.json:sodium.MixinChunkJobQueue
```

`ChunkJobQueue` only exists in the newer Sodium; the pinned Sodium for this target is `0.5.13`.

## Changes (1.20.1 target only; the default 26.3 build is untouched)

- `build.gradle`: for `-PvoxyTarget=1.20.1`, set `loom.mixin.useLegacyMixinAp = true` and
  `loom.mixin.defaultRefmapName = "voxy-refmap.json"`. Both are guarded with `hasProperty`, so a Loom release that
  drops one of them logs a warning instead of failing configuration.
- `client.voxy.1.20.1.mixins.json`, `common.voxy.1.20.1.mixins.json`: explicit `"refmap": "voxy-refmap.json"`.
- `client.voxy.1.20.1.mixins.json`: removed `sodium.MixinChunkJobQueue` (target absent in Sodium 0.5.13). The source file
  stays in the tree; it is simply not listed. Consequence: on this target Sodium's chunk builder threads are not merged
  into Voxy's thread pool.
- `.github/workflows/check-fabric-1-20-1.yml`: new step **Verify mixin refmap in built jar**, run before the upload. It
  fails the job unless every mixin config referenced by `fabric.mod.json` has a `refmap` key, the refmap file is in the
  jar, it contains intermediary entries, and it has a mapping for `MixinMinecraft` `disconnect(...)`.

## Verification status

- Nothing was compiled or run. The environment that produced this change had no network, so there was no Gradle, Loom
  or Minecraft classpath. The CI verification script itself was exercised against synthetic jars (one with a refmap,
  one without) and behaves as described.
- It is **not established** why the earlier jar had no refmap. The leading hypothesis is that the Mixin annotation
  processor did not run for the `fabric-loom-remap` build, so no refmap was generated or injected. This change forces the
  processor on and makes the result checkable. If the new CI step still fails with "no 'refmap' key" or "refmap not in
  the jar", the annotation processor is still not running and the next thing to inspect is the `compileJava` log for
  `-AoutRefMapFile` / `-AinMapFileNamedIntermediary` arguments.
- Not yet checked: that `disconnect(Screen)` and the other injection targets exist with the assumed signatures on
  1.20.1 (Stage 88 already lists them as unverified).

## Known remaining risks for the Android runtime (not addressed here)

- `lwjglVersion` is `3.4.1` for every target, so the jar bundles `lwjgl-lmdb`/`lwjgl-zstd` 3.4.1 while the launcher
  provides LWJGL 3.3.3. Expect a binding/version conflict once the mixins load.
- MobileGlues reported OpenGL 4.0 and no `GL_EXT_multi_draw_indirect`; the renderer feature gates (Stages 16, 52, 57)
  decide whether Voxy can start at all.

## Next gate

Run the `check-fabric-1-20-1` workflow. Install the new jar only if **Verify mixin refmap in built jar** passes, and
attach the next startup log.

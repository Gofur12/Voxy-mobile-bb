# Fabric 1.20.1 Gradle Target Profile — Stage 76

## Change

The build now accepts an explicit target selector:

- Default: `./gradlew build` (keeps the existing Minecraft 26.3 properties).
- Legacy target: `./gradlew -PvoxyTarget=1.20.1 build`.

The 1.20.1 profile is applied inside `build.gradle`; it does not rewrite `gradle.properties` and does not change the default target. Unsupported target strings fail early with a clear Gradle exception.

## Pinned 1.20.1 dependency profile

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | 0.19.2 |
| Fabric API | 0.92.8+1.20.1 |
| Sodium | mc1.20.1-0.5.13-fabric |
| Iris | 1.7.6+1.20.1 |
| Lithium | mc1.20.1-0.11.3-fabric |
| Mod Menu | 7.2.2 |
| Java bytecode release | 17 |

These are version coordinates for the legacy target, not a claim that the current Voxy source tree is already compatible with Minecraft 1.20.1. The Sodium/Iris pair is aligned with Iris 1.7.6's published 1.20.1 compatibility information.

## Verification status

- The normal 26.3 build remains the default path.
- The 1.20.1 profile has been wired into dependency selection and Java release selection.
- A successful 1.20.1 compile has **not** yet been established.
- Source-level Minecraft/Fabric API differences, LWJGL assumptions, and Android GLES work remain separate porting tasks.
- No Android launcher or device test is claimed.

## Next gate

Run the default CI build to ensure the profile refactor has not regressed 26.3. Then run the explicit 1.20.1 Gradle build and address concrete source/API compile errors before treating 1.20.1 as a supported target.

# Fabric 1.20.1 Source Compatibility Boundary — Stage 77

## Purpose

Turn the first real Fabric 1.20.1 compile result into a file-level remediation plan. This stage does not claim that the legacy target builds or runs on Android.

## CI evidence

The target command is:

```sh
./gradlew -PvoxyTarget=1.20.1 build --info --stacktrace
```

On commit `c7ded322ebc4f849d64d4d1c29518332be3c5195`, GitHub Actions run `43689142531` completed with failure in `compileJava`. The default 26.3 build and PR workflow both passed on the same commit (runs `43689142573` and `43689145948`).

## Confirmed compile blocker groups

The failure is not one missing import. It is a source/API generation mismatch across the current 26.3-oriented source tree and the 1.20.1 dependency set.

| Source file | Observed blocker | Required treatment |
|---|---|---|
| `client/config/SodiumConfigBuilder.java` | Sodium config API packages/types absent from Sodium 0.5.13 | Separate the modern Sodium config implementation from the legacy config entry point; do not fabricate Sodium classes |
| `client/config/VoxyConfigMenu.java` | Modern Sodium ConfigEntryPoint, ConfigBuilder, Identifier APIs absent | Provide a 1.20.1-compatible config integration or disable this optional entry point on the legacy target |
| `client/config/ModMenuIntegration.java` | Modern Sodium client config classes absent | Version-specific Mod Menu integration |
| `client/DebugEntries.java` | Modern Minecraft debug HUD package/types absent | Version-specific debug entry implementation or target-gated exclusion |
| `client/VoxyClientInstance.java` | Modern Sodium renderer package absent | Adapter against the actual Sodium 0.5.13 API |
| `client/core/rendering/Viewport.java` | Modern Sodium utility and fog API absent | Remove unnecessary Sodium internal type coupling or provide a version adapter |
| `client/core/RenderProperties.java` | RenderPearl pipeline API absent | Identify whether this is optional integration; isolate behind a target-specific adapter |
| `client/core/model/ModelFactory.java` | Modern Minecraft block tint, model layer and identifier types absent | Port model baking against 1.20.1 APIs with a separate implementation |
| `client/core/model/bakery/SoftwareModelTextureBakery.java` | RenderPearl and modern block/fluid renderer APIs absent | Separate version-specific model/texture bakery |
| `client/core/model/ModelStore.java` | Modern Identifier API absent | Use the target's native resource identifier in a version-specific source implementation |
| `commonImpl/WorldIdentifier.java` | Modern Identifier API absent | Use a target-specific identifier adapter |
| `client/VoxyCommands.java` | Minecraft command API symbol mismatch | Port command registration/signatures to 1.20.1 |

## Remediation order

1. **Source-set boundary:** add an explicit legacy source directory and target-based source selection in Gradle. Keep the default 26.3 source implementation unchanged.
2. **Optional integrations first:** isolate Sodium config UI, debug HUD, and RenderPearl integration so the core 1.20.1 compile is not held hostage by optional UI APIs.
3. **Minecraft API adapters:** port identifiers, commands, debug display and model-baking against actual 1.20.1 official mappings.
4. **Sodium 0.5.13 adapter:** inspect the resolved 0.5.13 jar/API and adapt renderer hooks; do not assume modern Sodium APIs exist.
5. **Compile gate:** require both default 26.3 and explicit 1.20.1 CI to pass after each compatibility slice.

## Guardrails

- Do not add fake classes under Sodium or Minecraft packages to silence compilation.
- Do not silently disable core Voxy rendering to make the build green.
- Do not claim Android, GLES, MobileGlues, Zink, Kopper, or distant-LOD runtime support based on compilation alone.
- Keep the 26.3 build green while implementing legacy compatibility.

## Stage 77 exit criteria

This stage is complete when the target-specific source boundary is documented and implemented, the first compatibility slice is selected without changing the default build, and CI verifies no regression on 26.3. A successful 1.20.1 build remains a later gate until all listed API groups are addressed.

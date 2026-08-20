# Persona–AssetChannel extension agent guide

Standalone Persona extension exposing AssetChannel sounds, soundtracks, stages, HUDs, conditions, and placeholders to ordered scripts. Java 25; build Persona first.

## Find things

- Persona registration and parsing: `src/main/java/nu/miguel/persona/assetchannel/AssetChannelExpansion.java`
- Testable bridge contract: `BridgeAdapter.java`; reflective implementation: `ReflectiveAssetChannelAdapter.java`
- Per-player soundtrack/HUD slot ownership: `SessionSlots.java`
- Extension manifest: `src/main/resources/persona-extension.yml`
- Authoring samples: `examples/`; behavior/parsing/reflection/slot tests: `src/test/java/`
- Supported commands and integration semantics: `README.md`.

## Boundary

AssetChannel is discovered through Paper services and invoked reflectively. Do not import, bundle, or directly reference `nu.miguel.assetChannel` classes: cross-plugin classloader linkage is intentionally avoided. Fail closed with useful Persona failures when AssetChannel is missing, incompatible, or not ready. Keep slot replacement, reconnect retention, origin validation, and async command ordering intact.

## Test-driven development

- For behavior-changing code, write the smallest meaningful automated test before changing production code.
- Run that exact test first and confirm it fails for the expected reason. Only then implement the change and rerun the test until it passes; do not weaken or rewrite the test merely to make the implementation pass.
- Add tests only where they protect meaningful behavior, regressions, contracts, or edge cases. Documentation, formatting, mechanical refactors, and other changes with no behavior to verify do not need tests.
- During development, run only the narrowest relevant test target. Before finishing, run the smallest affected test set needed to catch integration regressions; do not run unrelated full suites by default.

## Verify

```sh
cd ../Persona && ./gradlew build
cd ../PersonaAssetChannelExtension && ./gradlew build
```

`build` runs tests and `verifyNoAssetChannelClasses`. For one test: `./gradlew test --tests 'fully.qualified.TestName'`. Deploy `build/libs/PersonaAssetChannel-2.0.0.jar` to `plugins/Persona/extensions/` and restart; the extension is unshaded.

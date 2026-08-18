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

## Verify

```sh
cd ../Persona && ./gradlew build
cd ../PersonaAssetChannelExtension && ./gradlew build
```

`build` runs tests and `verifyNoAssetChannelClasses`. For one test: `./gradlew test --tests 'fully.qualified.TestName'`. Deploy `build/libs/PersonaAssetChannel-2.0.0.jar` to `plugins/Persona/extensions/` and restart; the extension is unshaded.


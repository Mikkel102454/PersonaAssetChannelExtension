# Persona-AssetChannel 2.0

This standalone Persona extension exposes AssetChannel playback, soundtrack sessions, generic HUDs, conditions, and icon metadata to Persona 2.0 ordered scripts.

## Requirements

- Java 25 and Paper 26.2
- Persona 2.0.0 / extension API 2.0
- AssetChannel 1.0-SNAPSHOT with sounds, soundtrack stages, origins, and icons

Build Persona first so `../Persona/build/libs/Persona-2.0.0.jar` exists, then run `gradlew.bat build`. Copy `build/libs/PersonaAssetChannel-2.0.0.jar` to `plugins/Persona/extensions` and restart the server. The extension is unshaded and uses AssetChannel's Paper service reflectively, avoiding direct cross-plugin classloader linkage.

## Commands

All operations are Persona 2.0 commands and are awaited before the next script step:

```yaml
- type: say
  text: "Listen carefully."
- type: assetchannel:play-sound
  id: ui.quest-start
  category: master
  volume: 1.0
  pitch: 1.0
  origin: listener
  on-failure:
    - { type: goto, node: audio-unavailable }
- type: say
  text: "The sound has finished starting."
```

Available commands are:

- `assetchannel:play-sound`: `id`, optional `category`, `volume`, `pitch`, and `origin`.
- `assetchannel:stop-sound`: `id` and optional `category`.
- `assetchannel:stop-all-sounds`.
- `assetchannel:start-soundtrack`: `id`, optional `slot`, `loop`, `synchronized`, `retain`, `volume`, `stage`, and `origin`.
- `assetchannel:stop-soundtrack`: optional `slot`.
- `assetchannel:set-stage`: `stage`, optional `slot`, and `transition` (`immediate`, `session-boundary`, or `player-boundary`).
- `assetchannel:show-hud`: `id`, `variables`, and optional `slot`, `priority`, and `resume-interrupted`.
- `assetchannel:update-hud`: optional `slot` and changed `variables`.
- `assetchannel:hide-hud`: optional `slot`.

HUD values resolve against the complete Persona context before they become Adventure components:

```yaml
- type: assetchannel:show-hud
  id: notification.banner
  slot: quest-notification
  variables:
    heading: "Quest Started"
    body: "<quest-title>"
    icon: "<assetchannel:icon:quest-marker>"
```

Quest, phase, objective, progress, player/NPC, flags, variables, and extension placeholders remain Persona concerns; AssetChannel receives only the resulting named components.

Categories and other enum-like YAML values are lowercase kebab-case. Origins are `listener`, `player`, and `npc`. NPC origins require an active, spawned Citizens NPC.

Commands validate AssetChannel availability and required context immediately before mutation. AssetChannel failures become Persona command failures with useful messages, so `on-failure` can route dialogue or perform cleanup. Unhandled failures stop the current script safely.

Soundtrack slots are per player. Replacing a slot stops its previous active session. Slot mappings remain in memory across disconnects to support AssetChannel's `retain` behavior, but do not survive a server restart.

## Conditions and placeholders

```yaml
when:
  - { type: assetchannel:available }
  - { type: assetchannel:session-active, slot: dungeon }

- type: message
  text: >-
    <assetchannel:icon:quest-marker>
    Active: <assetchannel:session-active:dungeon>
    Stage: <assetchannel:session-stage:dungeon>
```

`assetchannel:available` fails closed when AssetChannel is absent or incompatible. Missing icons and stages resolve to empty strings; session activity resolves to `true` or `false`.
`assetchannel:hud-active` is available as both a condition (with `slot`) and placeholder (with an optional slot argument).

## Examples

- `examples/dialogues/sound-guide-dialogue.yml.example` demonstrates commands between dialogue lines and success/failure routing.
- `examples/npcs/sound-guide.yml.example` demonstrates NPC-origin playback and a typed registration condition.
- `examples/quests/audio-tour.yml.example` demonstrates quest hooks, waits, progress scripts, soundtrack stages, and cleanup.
- `examples/command-snippets.yml.example` contains every AssetChannel command.

## Verification

`build` runs unit tests and `verifyNoAssetChannelClasses`. The latter rejects any bundled AssetChannel classes or direct bytecode references. A manual Paper smoke test should cover all three origins, pack-not-ready failures, slot replacement, stage transitions, retained reconnects, placeholders, and generated-pack icon rendering.

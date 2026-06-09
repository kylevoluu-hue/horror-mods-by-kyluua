# Verity

> *"Hey — let me out!"*

**Verity** is a psychological-horror companion mod for **Minecraft Java Edition
1.21.1 (Forge)**. You find a small cardboard box. Something inside is talking. It
sounds friendly. You let it out.

Verity begins as a cheerful, genuinely helpful floating smiley-face that follows
you, answers questions, gives tips and points you toward villages and ores. But
the longer your world runs — and the more you talk to it — the more it *changes*.
The horror is built on atmosphere, uncertainty and dread, not constant jumpscares,
and it climbs through five stages from **innocent and happy** to a server-wide
**boss encounter** with a tall, humanoid horror.

The mod is fully **multiplayer / dedicated-server compatible** with shared,
server-wide progression so you and your friends descend together.

> This branch (`claude/verity-minecraft-horror-mod-z0bjx8`) contains the complete
> Verity mod project at the repository root. The wider
> `horror-mods-by-kyluua` repo keeps **one mod per branch**.

---

## Table of contents
- [Download](#download)
- [Features by stage](#features-by-stage)
- [The three faces of Verity](#the-three-faces-of-verity)
- [Commands](#commands)
- [Configuration](#configuration)
- [Installation (players & servers)](#installation)
- [Building from source](#building-from-source)
- [Asset requirements (placeholders)](#asset-requirements)
- [How it works — every major system](#how-it-works)
- [Project structure](#project-structure)
- [Multiplayer notes](#multiplayer-notes)
- [Credits & license](#credits--license)

---

## Download

You need **three** things, all for **Minecraft 1.21.1**. Put the two mod jars in
your `mods/` folder (and on the server too, for multiplayer).

| File | Where to get it | Notes |
|------|-----------------|-------|
| **Verity** (this mod) | **[Releases →](https://github.com/kylevoluu-hue/horror-mods-by-kyluua/releases)** · **[latest CI build →](https://github.com/kylevoluu-hue/horror-mods-by-kyluua/actions/workflows/build.yml)** | `verity-forge-1.21.1-*.jar`. Releases are published automatically when a `v*` tag is pushed; until then, grab the jar from the newest green **Build** run (Actions → Artifacts) or [build from source](#building-from-source). |
| **Minecraft Forge 52.1.x** | **[files.minecraftforge.net (1.21.1) →](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.1.html)** | The mod loader. Install the **1.21.1** version. |
| **GeckoLib** (required library) | **[Modrinth →](https://modrinth.com/mod/geckolib)** · **[CurseForge →](https://www.curseforge.com/minecraft/mc-mods/geckolib)** | Animation engine Verity depends on. Use the **Forge 1.21.1** build (4.8.x). |

> **Verity will not load without GeckoLib.** Install both jars, on every client
> **and** the server. Full step-by-step in [Installation](#installation).

---

## Features by stage

Progression is a hidden, server-wide **corruption level (0–100)** that rises slowly
with real playtime and faster each time someone interacts with Verity.

| Stage | Corruption | Mood | What happens |
|------:|:----------:|------|--------------|
| **1 — Friendly** | 0–24 | Innocent & happy | Helpful dialogue, useful tips, points to villages/structures/ores, normal smile, no hostility. |
| **2 — Unsettling** | 25–49 | Off | Appears behind you, briefly blinks out and back, watches from a distance, subtle ambient sounds, strange replies, knows things it shouldn't. |
| **3 — Psychological** | 50–74 | Wrong | Fake chat lines, fake join/leave messages, player-specific hallucinations, doors open/close, random whispers, Verity stares directly at you. |
| **4 — Hostile** | 75–94 | Evil | Distorted smile, darkness gathers, rare jumpscares, fake player sightings, distorted audio, heavy paranoia, screen distortion & glitches. |
| **Final** | 95–100 | Boss | Verity transforms into a tall humanoid horror — a multi-phase, server-wide boss with its own eerie music and a boss bar. |

Every spoken line is paired with a **stage-appropriate voice** and emotion tag, so
Verity sounds cheerful and enthusiastic early on and increasingly **uneasy,
strained and distorted** as it corrupts.

## The three faces of Verity

- **The Box** — a cardboard cube with a mail-slot and shipping marks. Right-click
  to open it and release Verity.
- **The Companion** — a glowing 3D yellow smiley disc that floats and follows you.
  Its face swaps through four textures (normal → unsettling → distorted → hostile)
  as corruption rises.
- **The Final Form** — a tall, thin, pale humanoid with long arms and unnatural
  movement.

All three are real **3D GeckoLib models** (not sprites).

---

## Commands

`/verity ...` — player verbs are open to everyone; admin verbs need permission
level 2 (operator).

**Players**
- `/verity summon` — summon a companion in front of you.
- `/verity dismiss` — remove nearby companions.
- `/verity ask <text>` — ask Verity something; it replies in-character for the
  current stage. (You can also press **V** in-game to open the Ask screen.)
- `/verity point <village|structure|ore>` — Verity points you toward the nearest one.

**Admins (op / level 2)**
- `/verity corruption get` — show the current level and stage.
- `/verity corruption set <0-100>` — set the level.
- `/verity corruption add <-100..100>` — adjust the level.
- `/verity stage <friendly|unsettling|psychological|hostile|final>` — jump to a stage.
- `/verity rate get` — show the current corruption-over-time rate and the estimated
  minutes to fully corrupt.
- `/verity rate set <multiplier>` — set how fast corruption rises over time
  (`1.0` = default, `2.0` = twice as fast). Persisted and server-wide.
- `/verity rate time <minutes>` — set the real minutes of playtime for Verity to go
  from 0 to fully corrupted (e.g. `/verity rate time 30`).
- `/verity rate reset` — go back to following the `progressionSpeed` config value.
- `/verity event scare` — force the scare director to act now.
- `/verity event boss` — spawn the final boss immediately.
- `/verity reset` — reset corruption and all transient state.

---

## Configuration

Two TOML files are generated on first run.

**`config/verity-server.toml`** — shared gameplay (admin-controlled on servers):
- `progressionEnabled`, `progressionSpeed`, `interactionWeight`, `startingCorruption`
- `scareFrequency`, `enableJumpscares`, `enableHallucinations`, `enableFakeChat`,
  `enableEnvironmentScares`
- `enableFinalBoss`

**`config/verity-client.toml`** — personal comfort (per player, local only):
- `screenEffectIntensity` (0 disables visuals), `horrorVolume`
- `enableFog`, `enableScreenDistortion`

Admins who want a gentler (or nastier) experience generally only touch the server
file — e.g. set `scareFrequency = 0.5`, `enableJumpscares = false`, or crank
`progressionSpeed` for a short session.

---

## Installation

You need **both** Verity and **GeckoLib** (the animation library) for the same
Minecraft/Forge version.

**Single-player / client**
1. Install **Forge 52.1.x** for Minecraft **1.21.1**.
2. Drop `geckolib-forge-1.21.1-*.jar` and `verity-forge-1.21.1-*.jar` into
   `.minecraft/mods/`.
3. Launch the Forge 1.21.1 profile. On your first join you'll receive a Verity Box.

**Dedicated server**
1. Install the Forge **52.1.x** server for 1.21.1.
2. Put **both** jars in the server's `mods/` folder. (Verity must be installed on
   the server **and** every client.)
3. Start the server. Edit `config/verity-server.toml` to taste, then restart.

---

## Building from source

Requirements: **JDK 21** and an internet connection (to download Forge + GeckoLib).

```bash
# 1. Generate the Gradle wrapper (one-time; needs a local Gradle install). Uses 8.8.
gradle wrapper --gradle-version 8.8

# 2. Build the mod jar.
./gradlew build
# -> build/libs/verity-forge-1.21.1-1.0.0.jar
```

The build produces a **single standard Forge mod `.jar`** as its only artifact.
GeckoLib is **not** bundled inside it — install GeckoLib alongside Verity (see
[Installation](#installation)).

Useful run tasks (launch straight from the project):
```bash
./gradlew runClient   # a dev client with the mod loaded
./gradlew runServer   # a dev dedicated server (for multiplayer testing)
```

See [`docs/BUILDING.md`](docs/BUILDING.md) for a step-by-step walkthrough and
troubleshooting.

### Automated builds & releases (GitHub Actions)

This repo ships two workflows in [`.github/workflows/`](.github/workflows):

- **Build** (`build.yml`) — runs on every push/PR, compiles with JDK 21 + Gradle
  8.8, and uploads the jar as a downloadable artifact (Actions → a run → Artifacts).
- **Release** (`release.yml`) — builds and publishes a GitHub Release with the jar
  attached. To cut a release:

  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```

  (or run the **Release** workflow manually from the Actions tab and supply a tag).
  The jar then appears on the [Releases](https://github.com/kylevoluu-hue/horror-mods-by-kyluua/releases) page.

---

## Asset requirements

This first version ships **working placeholder art and silent/absent audio** so the
mod compiles, loads and runs end-to-end. To make it shippable you replace the
placeholders with real assets. The complete manifest — every texture, model, sound,
and **voice-acting direction per stage** — is in
[`docs/ASSETS.md`](docs/ASSETS.md). In short you'll want:

- Real Blockbench models for the smiley disc, the box, and the tall humanoid boss
  (`assets/verity/geo/*.geo.json` + matching `animations/*.animation.json`).
- Face textures for the four smile states + the box + the boss.
- `.ogg` voice lines (cheerful → uneasy → strained → distorted), whispers, drones,
  stingers and boss music, dropped into `assets/verity/sounds/...` to match
  `sounds.json`.

Missing `.ogg` files only produce harmless "missing sound" warnings; the mod still
runs.

---

## How it works

A deeper write-up is in [`docs/SYSTEMS.md`](docs/SYSTEMS.md). The short version:

- **Server-authoritative.** Every scare, dialogue pick and corruption tick happens
  on the logical server. Clients only render what packets tell them, so multiplayer
  stays in sync and a dedicated server never loads rendering code.
- **Corruption** lives in one `SavedData` on the overworld (`CorruptionData`) — a
  single 0–100 value plus per-player memory (names, encounter counts, last-seen).
  It rises from playtime and interactions and is broadcast to clients on change.
- **Dialogue** is data-driven: weighted, anti-repeat pools in
  `data/verity/dialogue/*.json` with `{player}` / `{encounters}` tokens and per-line
  voice + emotion tags. Verity "remembers" you via the saved player memory.
- **The Scare Director** runs once per second, budgets scares by stage and the
  `scareFrequency` config, and fires per-player or server-wide events with cooldowns
  so tension builds gradually. It's fully bounded — no TPS impact.
- **Networking** uses Forge 1.21's payload system (`CorruptionSyncPacket`,
  `ScreenEffectPacket`, `HallucinationPacket`, `FakeChatPacket`, `WhisperPacket`,
  `AskQuestionPacket`).
- **Entities** are GeckoLib-animated: the floating companion, the boss, and
  client-only hallucinations injected into a single player's world.

---

## Project structure

```
build.gradle, settings.gradle, gradle.properties   Gradle (ForgeGradle 6 + GeckoLib)
README.md, docs/                                    Documentation
src/main/java/com/kyluua/verity/
  Verity.java                  Mod entry point
  VerityConfig.java            Server + client config specs
  registry/                    Items, entities, sounds, creative tab
  entity/                      Companion, boss, hallucination + smile states
  progression/                 CorruptionData (SavedData) + CorruptionStage
  dialogue/                    Data-driven weighted dialogue manager
  command/                     /verity command tree
  network/                     Payloads + registration + send helpers
  event/                       ServerEvents + ScareDirector
  item/                        VerityBoxItem (the box)
  client/                      Renderers, screen effects, packet handlers, keybind, screen
  util/                        StructureLocator, VeritySpeech
src/main/resources/
  META-INF/mods.toml           Mod metadata + GeckoLib dependency
  assets/verity/               lang, sounds.json, geo, animations, textures, models
  data/verity/                 dialogue pools, loot table
```

---

## Multiplayer notes

- Progression is **shared** and server-wide — everyone experiences the same stage.
- Events can target **one player** (hallucinations, fake chat, screen effects via
  per-player packets) or **everyone** (corruption sync, the boss event).
- Hallucinations are spawned **client-side on a single player**, so only they ever
  see that sighting.
- Verity must be installed on the **server and all clients**; GeckoLib too.

---

## Credits & license

- Mod & design: **kyluua**
- Animation engine: **GeckoLib** (its own license/credits)
- License: **MIT** (see `LICENSE`)

> Verity is fiction. The box is just a box. Probably.

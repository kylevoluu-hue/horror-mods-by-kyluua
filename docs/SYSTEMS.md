# Verity — Systems Explained

This document explains every major system and how they fit together. The guiding
principle throughout is **server authority**: the logical server owns all state and
randomness; clients only render what they're told. That keeps multiplayer in sync
and means a dedicated server never loads client/rendering classes.

---

## 1. Entry point — `Verity.java`
Forge instantiates this `@Mod` class. It only wires things up:
- attaches the `DeferredRegister`s (items, entities, sounds, creative tab) to the
  mod event bus;
- registers the config specs and the networking payloads;
- subscribes the server-side handlers (`ServerEvents`, `VerityCommand`,
  `DialogueManager`) to the Forge game bus;
- on the **client only**, binds client setup, renderers and the keybind.

The dist guard (`FMLEnvironment.dist == Dist.CLIENT`) ensures none of the client
code is referenced on a dedicated server.

---

## 2. Progression — `progression/`
**`CorruptionData`** is a vanilla `SavedData` stored once on the overworld, so it
persists and is shared by everyone (server-wide progression). It holds:
- `level` (0–100) plus a fractional `accumulator` so slow growth still accrues;
- `bossTriggered` so the final event fires once;
- a map of **`PlayerMemory`** (name, encounter count, last-seen tick) keyed by UUID.

Growth: `tickPlaytime()` runs once per second from real playtime (scaled by
`progressionSpeed`); `addInteraction()` adds a burst whenever someone talks to or
opens Verity (scaled by `interactionWeight`). `enableFinalBoss = false` caps the
level just below transformation.

**`CorruptionStage`** maps the level to one of five stages with weighted thresholds
(the friendly stages last longest) and provides `progressWithin()` for smooth
effect ramps. Stage is always derived from the level — never stored separately.

---

## 3. Entities — `entity/`
All three are **GeckoLib `GeoEntity`s**.

- **`VerityCompanionEntity`** — a flying, no-gravity `PathfinderMob`. A custom
  `HoverFollowGoal` keeps it hovering near the nearest player; a `LookAtPlayerGoal`
  makes it watch you. It syncs three values to clients via `SynchedEntityData`: the
  `VeritySmileState` (which texture/face), a corruption mirror (for client glow),
  and a "staring" flag (special animation). Each second it reads `CorruptionData`
  and updates its face. The scare director calls its hooks: `teleportBehind`,
  `blink`/`unblink`, `setStaring`.
- **`VerityBossEntity`** — a tall `Monster` with a `ServerBossEvent` bar. Health
  drives three phases (100/66/33%); each phase speeds it up and switches its
  animation controller (`idle`/`walk`/`lunge`/`rage`) for that unnatural feel.
- **`HallucinationEntity`** — a no-collision, invulnerable, no-gravity ghost with a
  short lifespan that always faces the nearest player and **deletes itself the
  moment they get close**. It's normally spawned **client-side only** (see
  networking), so only one player ever sees a given sighting.

**`VeritySmileState`** enumerates the four faces and maps a stage → texture, so
Verity visibly degrades from innocent to evil.

---

## 4. Dialogue — `dialogue/`
**`DialogueManager`** is a datapack reload listener that loads every
`data/verity/dialogue/<pool>.json` into weighted pools. Selection is:
1. exclude lines used in the last few picks (per-pool ring buffer → anti-repeat);
2. weighted-random among the rest;
3. token substitution (`{player}`, `{encounters}`) from `CorruptionData` memory.

Each `DialogueEntry` carries a `voice` (a `verity:` sound id) and an `emotion` tag,
so the chosen line is spoken with stage-appropriate audio. Pools are named after
stages (`friendly`…`final`) plus `tips`.

Writers can add or rebalance lines with **no code changes** — just edit JSON and
`/reload`.

---

## 5. The Scare Director — `event/ScareDirector.java`
The horror "brain", ticked once per second per level that has players. Each tick:
1. run any due scheduled tasks (un-blink, stop staring);
2. bail out if the stage is still *Friendly*;
3. compute a cooldown from the stage and `scareFrequency`, and a random gate, so
   scares never feel metronomic and build gradually;
4. pick a random target player and fire a **stage-appropriate** scare from a
   weighted switch.

Scares include: relocate the companion behind you, make it stare, positioned
whispers, fake chat / join-leave, client-only hallucinations, door open/close,
vignette/distortion/glitch pulses, and (stage 4, if enabled) a jumpscare. Each
respects its config toggle. All work is bounded (small radii, no scans when no
players) → **no measurable TPS impact**.

---

## 6. Server glue — `event/ServerEvents.java`
On the Forge game bus:
- **Level tick (1/sec):** ticks playtime corruption on the overworld, broadcasts a
  `CorruptionSyncPacket` when the level changes, checks the boss trigger, and ticks
  the scare director wherever players are.
- **Player login:** registers the player in memory, syncs current corruption to
  them, and gives a **Verity Box** on their first ever join (tracked in the
  player's persisted NBT).
- **Boss trigger:** at level 100 it clears nearby companions, spawns the boss
  facing a player, plays the spawn sound, and broadcasts boss music + an
  announcement to everyone.

---

## 7. Networking — `network/`
Forge 1.21's payload system. Registration happens in `VerityNetwork` on
`RegisterPayloadHandlersEvent`; direction is enforced (`playToClient` vs
`playToServer`). Server→client handlers defer to client-only code through
`DistExecutor` so the server never classloads rendering.

| Packet | Dir | Purpose |
|--------|-----|---------|
| `CorruptionSyncPacket` | S→C | level + stage → fog/effects intensity |
| `ScreenEffectPacket` | S→C | vignette / distortion / glitch / jumpscare pulse |
| `HallucinationPacket` | S→C | spawn a client-only sighting for one player |
| `FakeChatPacket` | S→C | fake chat / fake join / fake leave |
| `WhisperPacket` | S→C | positioned whisper/ambient sound |
| `AskQuestionPacket` | C→S | the player's typed question → a dialogue reply |

Helpers `VerityNetwork.toPlayer(...)` and `toAll(...)` wrap `PacketDistributor`.

---

## 8. Client effects — `client/`
- **`ClientPacketHandlers`** applies each S→C packet: caches corruption, triggers
  screen effects, injects hallucination entities under unique negative ids (so they
  never clash with real entities), prints fake chat, and plays whispers locally.
- **`ScreenEffectHandler`** draws the ambient corruption vignette + creeping fog
  (scaled by the cached corruption) and the timed scare overlays (vignette stab,
  glitch bars, jumpscare flash), all gated by the client comfort config.
- **`VerityChatScreen` + `VerityKeybinds`** give a press-**V** "Ask Verity" box that
  sends an `AskQuestionPacket`.
- **Renderers** (`client/renderer/`) are thin `GeoEntityRenderer`s; the companion's
  texture is chosen from its current smile state.

---

## 9. Items & commands
- **`VerityBoxItem`** — right-click opens the box: spawns the companion, plays the
  iconic "Hey, let me out!", greets the opener, seeds corruption, and consumes the
  box.
- **`VerityCommand`** — the `/verity` tree (player verbs open to all; admin verbs at
  permission level 2) for summoning, asking, pointing, and full admin control of
  corruption / stages / events.

---

## Data flow at a glance
```
playtime + interactions ──► CorruptionData (server, SavedData)
                                   │  on change
                                   ├─► CorruptionSyncPacket ──► ClientCorruptionState ──► fog/vignette
                                   │
ScareDirector (1/sec) ── reads stage ──► per-player packets ──► ClientPacketHandlers ──► scares
                                   │
DialogueManager ── weighted/anti-repeat ──► VeritySpeech ──► chat + voice
                                   │
level == 100 ──► ServerEvents.spawnBoss ──► boss + music broadcast (everyone)
```

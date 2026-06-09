# Verity — Asset Requirements

This first version ships **valid placeholders** for everything so the mod compiles,
loads and runs. This document lists the real assets to create for a shippable
release, with exact paths and specs. Paths are under `src/main/resources/`.

The reference look:
- **Companion** — a glowing round yellow smiley disc (black oval eyes, wide grin).
- **Box** — a cardboard cube with a dark mail-slot and shipping marks (▲ + fragile).
- **Final form** — a tall, thin, pale-grey bald humanoid with long arms.

---

## 1. Models (GeckoLib `.geo.json`)
Author in [Blockbench](https://www.blockbench.net/) (Bedrock/GeckoLib model), keep
the bone names so the existing animations bind.

| File | Replace with | Notes |
|------|--------------|-------|
| `assets/verity/geo/verity_companion.geo.json` | Round disc / coin smiley | Keep a bone named `body`. Make it read as a 3D coin, not flat. |
| `assets/verity/geo/verity_boss.geo.json` | Tall humanoid | Keep bones `root, body, head, armLeft, armRight, legLeft, legRight`. Exaggerate height + arm length. |

(The box is an **item**, not a GeckoLib entity — see Models §3.)

## 2. Animations (`.animation.json`)
| File | Animations expected by code |
|------|------------------------------|
| `assets/verity/animations/verity_companion.animation.json` | `idle`, `float`, `stare`, `distort` |
| `assets/verity/animations/verity_boss.animation.json` | `idle`, `walk`, `lunge`, `rage`, `stare` |

Replace the placeholder keyframes with proper, unnatural motion (jitter, head
tilts, long-arm sway). Keep the animation names — they're referenced in the entity
animation controllers.

## 3. Textures (`.png`)
| Path | Size | Replace with |
|------|------|--------------|
| `assets/verity/textures/entity/verity_smile_normal.png` | 32×32 | Bright cheerful yellow smiley |
| `assets/verity/textures/entity/verity_smile_unsettling.png` | 32×32 | Slightly dimmer, crooked grin / eye twitch |
| `assets/verity/textures/entity/verity_smile_distorted.png` | 32×32 | Colder hue, melting/cracked grin |
| `assets/verity/textures/entity/verity_smile_hostile.png` | 32×32 | Jagged, red-shifted, wrong |
| `assets/verity/textures/entity/verity_boss.png` | 64×64 | Pale-grey humanoid skin (UV-mapped to the geo) |
| `assets/verity/textures/item/verity_box.png` | 16×16 | Cardboard with mail-slot + ▲/fragile marks |
| `verity_logo.png` (resources root) | 128×128 | Mod logo for the mods list |

Tip: give the smiley an **emissive** glow layer so it self-lights in the dark
(GeckoLib auto-glow uses a `*_glowmask` companion texture).

The **box item model** (`assets/verity/models/item/verity_box.json`) currently uses
one texture on all faces. For the real look, convert it to a per-face model so the
mail-slot only shows on the front.

## 4. Sounds (`.ogg`, Ogg Vorbis)
Drop files to match `assets/verity/sounds.json`. Folders already exist under
`assets/verity/sounds/`.

| Sound id | File path | Direction |
|----------|-----------|-----------|
| `voice_letmeout` | `sounds/voice/letmeout.ogg` | **Excited, muffled-in-box, genuinely friendly:** "Hey — let me out!" |
| `voice_friendly_1/2` | `sounds/voice/friendly_1.ogg`, `friendly_2.ogg` | Warm, cheerful, helpful. A friend. |
| `voice_unsettling_1/2` | `sounds/voice/unsettling_1.ogg`, `unsettling_2.ogg` | Still friendly **but a beat too long, too knowing.** Slight unease. |
| `voice_psych_1/2` | `sounds/voice/psych_1.ogg`, `psych_2.ogg` | Strained, hesitant, half-whispered, voice cracking. |
| `voice_hostile_1/2` | `sounds/voice/hostile_1.ogg`, `hostile_2.ogg` | Distorted, layered/doubled, pitch-bent, threatening. |
| `whisper` | `sounds/ambient/whisper.ogg` | Indistinct close whisper. |
| `ambient_drone` | `sounds/ambient/drone.ogg` | Low uneasy drone (loopable). |
| `ambient_distorted` | `sounds/ambient/distorted.ogg` | Glitchy distorted ambience. |
| `heartbeat` | `sounds/ambient/heartbeat.ogg` | Slow heartbeat (paranoia). |
| `jumpscare_stinger` | `sounds/scare/stinger.ogg` | Sharp loud stab. |
| `glitch` | `sounds/scare/glitch.ogg` | Digital glitch burst. |
| `boss_spawn` | `sounds/boss/spawn.ogg` | Reality-tearing arrival. |
| `boss_music` | `sounds/boss/music.ogg` | Eerie boss loop (marked `stream: true`). |
| `boss_roar` | `sounds/boss/roar.ogg` | Distorted scream. |

**Voice arc summary:** cheerful & enthusiastic (stage 1) → friendly-but-off (stage
2) → strained & whispery (stage 3) → distorted & wrong (stage 4) → fully monstrous
(boss). The `emotion` tag on each dialogue line in
`data/verity/dialogue/*.json` should match the take you record.

Missing `.ogg` files don't crash the game — they only log "missing sound"
warnings — so you can ship art first and audio later.

## 5. Optional polish
- Custom particle textures for the "dark gathering" at high corruption.
- A `verity:textures/gui/` overlay for the jumpscare flash (a face PNG drawn over
  the screen) instead of the plain black stab.
- A bossbar background under `assets/verity/textures/gui/` if you want a custom bar.

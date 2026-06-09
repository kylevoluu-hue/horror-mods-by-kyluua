package com.kyluua.verity.event;

import com.kyluua.verity.VerityConfig;
import com.kyluua.verity.entity.VerityCompanionEntity;
import com.kyluua.verity.network.FakeChatPacket;
import com.kyluua.verity.network.HallucinationPacket;
import com.kyluua.verity.network.ScreenEffectPacket;
import com.kyluua.verity.network.VerityNetwork;
import com.kyluua.verity.network.WhisperPacket;
import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.progression.CorruptionStage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The scare director - Verity's "horror brain".
 *
 * <p>Ticked once per second from {@link ServerEvents}. On each tick it decides,
 * per stage and gated by a cooldown + the {@code scareFrequency} config, whether to
 * fire a scare, then picks a stage-appropriate one from a weighted table and runs
 * it. Everything happens server-side and reaches players through packets (or small,
 * reversible world changes), so the horror is authoritative and stays in sync in
 * multiplayer. A built-in cooldown means tension builds gradually instead of
 * spamming.</p>
 *
 * <p>All logic is bounded and cheap (no scans larger than a small radius, no work
 * when no players are online), so it has no meaningful TPS impact.</p>
 */
public final class ScareDirector {

    private ScareDirector() {}

    private static final Random RANDOM = new Random();

    /** Game-time tick of the last scare, to enforce a cooldown. */
    private static long lastScareTick = 0;

    /** Fake player names used for fake chat / join-leave scares. */
    private static final String[] FAKE_NAMES = {
            "Player", "Steve", "Notch", "Herobrine", "null", "Verity", "you"
    };
    private static final String[] FAKE_LINES = {
            "is anyone else seeing this?", "where did you go?", "don't turn around.",
            "i can see you.", "why is it so quiet?", "help", "behind you"
    };

    /** Tiny scheduler so scares can restore state (un-blink, stop staring) a moment later. */
    private record ScheduledTask(long runAt, Runnable action) {}
    private static final List<ScheduledTask> SCHEDULE = new ArrayList<>();

    /**
     * Main entry point, called every second per loaded level that has players.
     */
    public static void tick(ServerLevel level, CorruptionData data) {
        long now = level.getGameTime();
        runDueTasks(now);

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        CorruptionStage stage = data.getStage();
        if (stage == CorruptionStage.FRIENDLY) return; // no scares while genuinely friendly

        // Cooldown shrinks as corruption rises and scareFrequency increases.
        double freq = VerityConfig.SERVER.scareFrequency.get();
        if (freq <= 0) return;
        int baseCooldown = switch (stage) {
            case UNSETTLING -> 600;       // ~30s
            case PSYCHOLOGICAL -> 400;    // ~20s
            case HOSTILE -> 240;          // ~12s
            default -> 200;
        };
        long cooldown = (long) (baseCooldown / freq);
        if (now - lastScareTick < cooldown) return;

        // Roll to actually fire (adds randomness so it never feels metronomic).
        if (RANDOM.nextFloat() > 0.6f) return;

        ServerPlayer target = players.get(RANDOM.nextInt(players.size()));
        fireScare(level, data, stage, target);
        lastScareTick = now;
    }

    /** Picks and runs a scare appropriate to the stage. */
    private static void fireScare(ServerLevel level, CorruptionData data, CorruptionStage stage, ServerPlayer target) {
        switch (stage) {
            case UNSETTLING -> {
                switch (RANDOM.nextInt(3)) {
                    case 0 -> behindYou(level, target);
                    case 1 -> whisper(level, target, 0);
                    default -> stare(level, target);
                }
            }
            case PSYCHOLOGICAL -> {
                switch (RANDOM.nextInt(6)) {
                    case 0 -> fakeChat(target);
                    case 1 -> hallucination(target);
                    case 2 -> whisper(level, target, RANDOM.nextInt(2));
                    case 3 -> stare(level, target);
                    case 4 -> doorScare(level, target);
                    default -> screenPulse(target, ScreenEffectPacket.VIGNETTE, 0.5f, 30);
                }
            }
            case HOSTILE -> {
                switch (RANDOM.nextInt(7)) {
                    case 0 -> hallucination(target);
                    case 1 -> screenPulse(target, ScreenEffectPacket.DISTORTION, 0.8f, 60);
                    case 2 -> screenPulse(target, ScreenEffectPacket.GLITCH, 0.9f, 25);
                    case 3 -> whisper(level, target, 2);
                    case 4 -> behindYou(level, target);
                    case 5 -> fakeChat(target);
                    default -> jumpscare(level, target);
                }
            }
            default -> { /* FINAL handled by the boss event */ }
        }
    }

    // =========================================================================
    //  Individual scares
    // =========================================================================

    /** Silently relocate the companion to just behind the player. */
    private static void behindYou(ServerLevel level, ServerPlayer player) {
        VerityCompanionEntity verity = nearestCompanion(level, player);
        if (verity != null) verity.teleportBehind(player);
    }

    /** Make the companion stare at the player for a few seconds. */
    private static void stare(ServerLevel level, ServerPlayer player) {
        VerityCompanionEntity verity = nearestCompanion(level, player);
        if (verity == null) return;
        verity.setStaring(true);
        schedule(level.getGameTime() + 80, () -> verity.setStaring(false));
    }

    /** Play a positioned whisper near the player. */
    private static void whisper(ServerLevel level, ServerPlayer player, int soundId) {
        Vec3 around = player.position().add(offset(2.0, 4.0));
        VerityNetwork.toPlayer(player, new WhisperPacket(
                around.x, around.y + 1, around.z, soundId, 0.7f, 0.9f + RANDOM.nextFloat() * 0.2f));
    }

    /** Inject a fake chat line (or join/leave) only this player sees. */
    private static void fakeChat(ServerPlayer player) {
        if (!VerityConfig.SERVER.enableFakeChat.get()) return;
        int roll = RANDOM.nextInt(5);
        if (roll == 0) {
            VerityNetwork.toPlayer(player, new FakeChatPacket(randomName(), FakeChatPacket.JOIN));
        } else if (roll == 1) {
            VerityNetwork.toPlayer(player, new FakeChatPacket(randomName(), FakeChatPacket.LEAVE));
        } else {
            String line = "<" + randomName() + "> " + FAKE_LINES[RANDOM.nextInt(FAKE_LINES.length)];
            VerityNetwork.toPlayer(player, new FakeChatPacket(line, FakeChatPacket.CHAT));
        }
    }

    /** Spawn a client-only hallucination at the edge of the player's vision. */
    private static void hallucination(ServerPlayer player) {
        if (!VerityConfig.SERVER.enableHallucinations.get()) return;
        Vec3 pos = player.position().add(offset(6.0, 12.0));
        VerityNetwork.toPlayer(player, new HallucinationPacket(pos.x, pos.y, pos.z, 60 + RANDOM.nextInt(60), 0));
    }

    /** Open or close the nearest door to the player. */
    private static void doorScare(ServerLevel level, ServerPlayer player) {
        if (!VerityConfig.SERVER.enableEnvironmentScares.get()) return;
        BlockPos origin = player.blockPosition();
        int r = 6;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -3, -r), origin.offset(r, 3, r))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock && state.hasProperty(BlockStateProperties.OPEN)) {
                boolean open = state.getValue(BlockStateProperties.OPEN);
                level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !open), 10);
                level.levelEvent(null, open ? 1012 : 1006, pos, 0); // vanilla door sound
                return;
            }
        }
    }

    /** Direct jumpscare: yank the companion behind the player + flash + stinger. */
    private static void jumpscare(ServerLevel level, ServerPlayer player) {
        if (!VerityConfig.SERVER.enableJumpscares.get()) {
            screenPulse(player, ScreenEffectPacket.GLITCH, 0.7f, 20); // soft fallback
            return;
        }
        behindYou(level, player);
        screenPulse(player, ScreenEffectPacket.JUMPSCARE, 1.0f, 15);
    }

    private static void screenPulse(ServerPlayer player, int effect, float intensity, int duration) {
        VerityNetwork.toPlayer(player, new ScreenEffectPacket(effect, intensity, duration));
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private static VerityCompanionEntity nearestCompanion(ServerLevel level, ServerPlayer player) {
        List<VerityCompanionEntity> list = level.getEntitiesOfClass(
                VerityCompanionEntity.class, player.getBoundingBox().inflate(64.0D));
        return list.isEmpty() ? null : list.get(0);
    }

    private static Vec3 offset(double min, double max) {
        double dist = min + RANDOM.nextDouble() * (max - min);
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        return new Vec3(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
    }

    private static String randomName() {
        return FAKE_NAMES[RANDOM.nextInt(FAKE_NAMES.length)];
    }

    private static void schedule(long runAt, Runnable action) {
        SCHEDULE.add(new ScheduledTask(runAt, action));
    }

    private static void runDueTasks(long now) {
        SCHEDULE.removeIf(task -> {
            if (now >= task.runAt()) {
                try { task.action().run(); } catch (Exception ignored) {}
                return true;
            }
            return false;
        });
    }

    /** Clears transient state (used by /verity reset). */
    public static void reset() {
        SCHEDULE.clear();
        lastScareTick = 0;
    }
}

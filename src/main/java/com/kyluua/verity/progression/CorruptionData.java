package com.kyluua.verity.progression;

import com.kyluua.verity.VerityConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The single, server-wide source of truth for Verity's progression.
 *
 * <p>Stored once on the overworld via Minecraft's {@link SavedData} system, so it
 * persists across restarts and is shared by everyone on the server (the spec's
 * "server-wide progression affects all players"). It tracks:</p>
 * <ul>
 *   <li>the hidden 0-100 {@code level} (and a fractional accumulator for smooth growth);</li>
 *   <li>per-player {@link PlayerMemory} - names seen, how many encounters, when last seen -
 *       which the dialogue system uses to feel like Verity "remembers" people.</li>
 * </ul>
 */
public class CorruptionData extends SavedData {

    /** Storage key under the overworld's data folder. */
    public static final String NAME = "verity_corruption";

    /** Hidden corruption, clamped 0-100. */
    private int level;

    /** Fractional progress carried between ticks so slow growth still accrues. */
    private double accumulator;

    /** Per-player memory keyed by UUID. */
    private final Map<UUID, PlayerMemory> memory = new HashMap<>();

    /** True once the final boss has been spawned, so it only triggers once. */
    private boolean bossTriggered;

    /**
     * Runtime override for the progression speed multiplier, settable by an OP via
     * {@code /verity rate ...} and persisted. {@code -1} is the sentinel for "unset"
     * (follow the {@code progressionSpeed} config value instead).
     */
    private double rateOverride = -1.0D;

    /** Base corruption gained per second at speed multiplier 1.0. */
    public static final double BASE_RATE_PER_SECOND = 0.0025D;

    public CorruptionData() {}

    /** Fetches (or creates) the shared instance. Always stored on the overworld. */
    public static CorruptionData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CorruptionData::create, CorruptionData::load, null), NAME);
    }

    private static CorruptionData create() {
        CorruptionData data = new CorruptionData();
        data.level = VerityConfig.SERVER.startingCorruption.get();
        return data;
    }

    // =========================================================================
    //  Progression
    // =========================================================================

    /**
     * Advances corruption from real playtime. Call once per second from the server
     * tick. Growth rate is tuned so the friendly stages last a good while and is
     * scaled by the {@code progressionSpeed} config.
     */
    public void tickPlaytime() {
        if (!VerityConfig.SERVER.progressionEnabled.get()) return;
        // BASE_RATE_PER_SECOND at speed 1.0 => ~100 corruption in roughly 11 hours of
        // playtime. The effective speed is the OP's runtime override if set, otherwise
        // the progressionSpeed config (see /verity rate ...).
        accumulator += BASE_RATE_PER_SECOND * getEffectiveSpeed();
        flushAccumulator();
    }

    /** Adds corruption from a direct interaction (talking to Verity, opening the box, ...). */
    public void addInteraction(double multiplier) {
        if (!VerityConfig.SERVER.progressionEnabled.get()) return;
        accumulator += 0.05D * VerityConfig.SERVER.interactionWeight.get() * multiplier;
        flushAccumulator();
    }

    /** Moves whole points out of the accumulator into the clamped level. */
    private void flushAccumulator() {
        int cap = VerityConfig.SERVER.enableFinalBoss.get() ? 100 : 94;
        while (accumulator >= 1.0D && level < cap) {
            accumulator -= 1.0D;
            level++;
            setDirty();
        }
        if (level >= cap) {
            accumulator = 0;
        }
    }

    public int getLevel() {
        return level;
    }

    /** Admin/command setter. Clamps to 0-100 and resets the accumulator. */
    public void setLevel(int newLevel) {
        this.level = Math.max(0, Math.min(100, newLevel));
        this.accumulator = 0;
        setDirty();
    }

    public CorruptionStage getStage() {
        return CorruptionStage.fromLevel(level);
    }

    // =========================================================================
    //  Rate control (OP-settable, persisted, server-wide)
    // =========================================================================

    /** The speed multiplier actually used: the runtime override if set, else the config. */
    public double getEffectiveSpeed() {
        return rateOverride >= 0 ? rateOverride : VerityConfig.SERVER.progressionSpeed.get();
    }

    /** True if an OP has set a runtime override (rather than following the config). */
    public boolean hasRateOverride() {
        return rateOverride >= 0;
    }

    /** Sets the runtime speed multiplier (1.0 = default, 2.0 = twice as fast). */
    public void setSpeedMultiplier(double multiplier) {
        this.rateOverride = Math.max(0.0D, multiplier);
        setDirty();
    }

    /** Clears the override so progression follows the {@code progressionSpeed} config again. */
    public void resetRate() {
        this.rateOverride = -1.0D;
        setDirty();
    }

    /**
     * Sets the speed so Verity goes from 0 to fully corrupted in the given number of
     * real minutes (of accumulated playtime). Converts the target time into the
     * equivalent multiplier.
     */
    public void setTimeToFull(double minutes) {
        double safeMinutes = Math.max(0.1D, minutes);
        setSpeedMultiplier(minutesAtSpeedOne() / safeMinutes);
    }

    /** Estimated real minutes to climb the full 0-100 at the current effective speed. */
    public double estimatedMinutesToFull() {
        double speed = getEffectiveSpeed();
        if (speed <= 0) return Double.POSITIVE_INFINITY;
        return minutesAtSpeedOne() / speed;
    }

    /** Minutes to fully corrupt at multiplier 1.0 (~667 min). */
    private static double minutesAtSpeedOne() {
        return 100.0D / (BASE_RATE_PER_SECOND * 60.0D);
    }

    /** True once corruption has maxed out and the final boss should trigger. */
    public boolean isAtFinal() {
        return level >= 100 && VerityConfig.SERVER.enableFinalBoss.get();
    }

    public boolean isBossTriggered() {
        return bossTriggered;
    }

    public void setBossTriggered(boolean triggered) {
        this.bossTriggered = triggered;
        setDirty();
    }

    // =========================================================================
    //  Player memory
    // =========================================================================

    public PlayerMemory remember(ServerPlayer player) {
        PlayerMemory mem = memory.computeIfAbsent(player.getUUID(), id -> new PlayerMemory(player.getGameProfile().getName()));
        mem.name = player.getGameProfile().getName();
        return mem;
    }

    public PlayerMemory getMemory(UUID id) {
        return memory.get(id);
    }

    /** Records that Verity just interacted with a player (bumps encounter count + timestamp). */
    public void recordEncounter(ServerPlayer player) {
        PlayerMemory mem = remember(player);
        mem.encounters++;
        mem.lastSeenTick = player.level().getGameTime();
        setDirty();
    }

    // =========================================================================
    //  Persistence
    // =========================================================================

    public static CorruptionData load(CompoundTag tag, HolderLookup.Provider provider) {
        CorruptionData data = new CorruptionData();
        data.level = tag.getInt("level");
        data.accumulator = tag.getDouble("accumulator");
        data.bossTriggered = tag.getBoolean("bossTriggered");
        data.rateOverride = tag.contains("rateOverride") ? tag.getDouble("rateOverride") : -1.0D;
        ListTag list = tag.getList("memory", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            PlayerMemory mem = new PlayerMemory(e.getString("name"));
            mem.encounters = e.getInt("encounters");
            mem.lastSeenTick = e.getLong("lastSeen");
            data.memory.put(e.getUUID("id"), mem);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("level", level);
        tag.putDouble("accumulator", accumulator);
        tag.putBoolean("bossTriggered", bossTriggered);
        tag.putDouble("rateOverride", rateOverride);
        ListTag list = new ListTag();
        memory.forEach((id, mem) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", id);
            e.putString("name", mem.name == null ? "" : mem.name);
            e.putInt("encounters", mem.encounters);
            e.putLong("lastSeen", mem.lastSeenTick);
            list.add(e);
        });
        tag.put("memory", list);
        return tag;
    }

    /** What Verity remembers about one player. */
    public static final class PlayerMemory {
        public String name;
        public int encounters;
        public long lastSeenTick;

        public PlayerMemory(String name) {
            this.name = name;
        }
    }
}

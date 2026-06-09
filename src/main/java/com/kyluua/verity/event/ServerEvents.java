package com.kyluua.verity.event;

import com.kyluua.verity.Verity;
import com.kyluua.verity.entity.VerityBossEntity;
import com.kyluua.verity.entity.VerityCompanionEntity;
import com.kyluua.verity.network.CorruptionSyncPacket;
import com.kyluua.verity.network.VerityNetwork;
import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.registry.VerityEntities;
import com.kyluua.verity.registry.VeritySounds;
import com.kyluua.verity.registry.VerityItems;
import com.kyluua.verity.dialogue.DialogueEntry;
import com.kyluua.verity.dialogue.DialogueManager;
import com.kyluua.verity.progression.CorruptionStage;
import com.kyluua.verity.util.VeritySpeech;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/**
 * Server-side glue: drives progression, ticks the scare director, keeps clients in
 * sync, hands out the starting box, and triggers the final boss.
 *
 * <p>Registered on the Forge game event bus from {@link Verity}. All work is
 * throttled to once per second and skips empty levels, so it is effectively free on
 * the server tick.</p>
 */
public final class ServerEvents {

    /** Persistent-data key marking that a player has already received their box. */
    private static final String GOT_BOX_TAG = "verity_got_box";

    /** Mirror of the last level we broadcast, so we only sync on change. */
    private static int lastBroadcastLevel = -1;

    // =========================================================================
    //  Per-level tick (once per second).
    // =========================================================================
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getGameTime() % 20L != 0L) return; // once per second

        CorruptionData data = CorruptionData.get(level);

        // Corruption only ticks from playtime once (on the overworld) to avoid
        // multiplying across dimensions.
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            int before = data.getLevel();
            data.tickPlaytime();
            if (data.getLevel() != before) {
                broadcastCorruption(level, data);
            }
            maybeTriggerBoss(level, data);
        }

        // Scares run in whatever dimension players currently are.
        if (!level.players().isEmpty()) {
            ScareDirector.tick(level, data);
        }
    }

    /** Broadcasts the current corruption to every player when it changes. */
    private void broadcastCorruption(ServerLevel level, CorruptionData data) {
        if (data.getLevel() == lastBroadcastLevel) return;
        lastBroadcastLevel = data.getLevel();
        VerityNetwork.toAll(level.getServer(),
                new CorruptionSyncPacket(data.getLevel(), data.getStage().ordinal()));
    }

    // =========================================================================
    //  Final boss
    // =========================================================================
    private void maybeTriggerBoss(ServerLevel level, CorruptionData data) {
        if (!data.isAtFinal() || data.isBossTriggered()) return;
        data.setBossTriggered(true);

        ServerPlayer host = level.getServer().getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (host == null) return;

        spawnBoss(host.serverLevel(), host);
    }

    /** Spawns the boss near a player and announces the server-wide event. */
    public static void spawnBoss(ServerLevel level, ServerPlayer near) {
        // The companion(s) "become" the boss - clear them first.
        for (VerityCompanionEntity companion :
                level.getEntitiesOfClass(VerityCompanionEntity.class, near.getBoundingBox().inflate(128))) {
            companion.discard();
        }

        Vec3 spawn = near.position().add(near.getLookAngle().scale(8.0D));
        VerityBossEntity boss = VerityEntities.VERITY_BOSS.get().create(level);
        if (boss == null) return;
        boss.moveTo(spawn.x, near.getY(), spawn.z, near.getYRot() + 180, 0);
        boss.setCustomName(Component.literal("Verity").withStyle(ChatFormatting.DARK_RED));
        VerityBossEntity.prepare(boss);
        level.addFreshEntity(boss);

        // Audio + announcement to everyone.
        level.playSound(null, spawn.x, spawn.y, spawn.z, VeritySounds.BOSS_SPAWN.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(VeritySounds.BOSS_MUSIC.get()),
                    SoundSource.MASTER, p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, p.getRandom().nextLong()));
            p.sendSystemMessage(Component.literal("Verity is here.").withStyle(ChatFormatting.DARK_RED));
        }
        Verity.LOGGER.info("[Verity] Final boss triggered.");
    }

    // =========================================================================
    //  Player join: sync corruption + hand out the starting box.
    // =========================================================================
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        CorruptionData data = CorruptionData.get(level);

        // Verity remembers this player.
        data.remember(player);

        // Tell the client the current corruption immediately.
        VerityNetwork.toPlayer(player, new CorruptionSyncPacket(data.getLevel(), data.getStage().ordinal()));

        // Give the box exactly once - the spec's "starts in a box in the first
        // player's inventory". Here every player gets one on their first ever join
        // so anyone can be the one to open it.
        giveBoxIfFirstTime(player);
    }

    private void giveBoxIfFirstTime(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData();
        if (persisted.getBoolean(GOT_BOX_TAG)) return;

        persisted.putBoolean(GOT_BOX_TAG, true);

        ItemStack box = new ItemStack(VerityItems.VERITY_BOX.get());
        if (!player.getInventory().add(box)) {
            player.drop(box, false);
        }
        player.sendSystemMessage(Component.translatable("message.verity.received_box")
                .withStyle(ChatFormatting.GRAY));
    }

    // =========================================================================
    //  Chat: Verity answers when a player talks and a companion is nearby.
    // =========================================================================
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;
        ServerLevel level = player.serverLevel();

        // Verity only "hears" you when a companion is reasonably close.
        boolean listening = !level.getEntitiesOfClass(
                VerityCompanionEntity.class, player.getBoundingBox().inflate(48.0D)).isEmpty();
        if (!listening) return;

        CorruptionData data = CorruptionData.get(level);
        data.addInteraction(0.5D);     // talking to Verity feeds the corruption
        data.recordEncounter(player);

        CorruptionStage stage = data.getStage();
        DialogueEntry reply = DialogueManager.get().pickForStage(stage, player, data);
        if (reply == null) return;

        // Reply just after the player's own message is broadcast.
        player.server.execute(() -> VeritySpeech.speak(player, reply, stage.isHorror()));
    }

    /** Used by /verity reset to also clear the broadcast cache. */
    public static void resetBroadcastCache() {
        lastBroadcastLevel = -1;
    }
}

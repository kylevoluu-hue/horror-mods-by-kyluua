package com.kyluua.verity.item;

import com.kyluua.verity.dialogue.DialogueEntry;
import com.kyluua.verity.dialogue.DialogueManager;
import com.kyluua.verity.entity.VerityCompanionEntity;
import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.progression.CorruptionStage;
import com.kyluua.verity.registry.VerityEntities;
import com.kyluua.verity.registry.VeritySounds;
import com.kyluua.verity.util.VeritySpeech;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Verity Box - the cardboard box Verity ships inside.
 *
 * <p>Right-clicking "opens" the box: the companion entity spawns just in front of
 * the player, lets out its excited "Hey, let me out!" and greets the opener. This
 * is the player's first contact with Verity, so it also seeds a little corruption
 * and registers the opener in Verity's memory as the "owner".</p>
 */
public class VerityBoxItem extends Item {

    public VerityBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // All gameplay happens server-side; the client just swings.
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            openBox(serverLevel, serverPlayer, stack);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    /** Spawns Verity, plays the intro voice + greeting, and consumes the box. */
    private void openBox(ServerLevel level, ServerPlayer player, ItemStack stack) {
        // Spawn the companion a couple of blocks in front of the player at eye height.
        Vec3 spawn = player.getEyePosition().add(player.getLookAngle().scale(2.0D));
        VerityCompanionEntity verity = VerityEntities.VERITY_COMPANION.get().create(level);
        if (verity == null) return;

        verity.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0);
        verity.setOwnerId(player.getUUID());
        verity.setPersistenceRequired();
        level.addFreshEntity(verity);

        // The famous first line - excited and friendly.
        level.playSound(null, spawn.x, spawn.y, spawn.z,
                VeritySounds.VOICE_LETMEOUT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Progression: opening the box is the first real interaction.
        CorruptionData data = CorruptionData.get(level);
        data.addInteraction(2.0D);
        data.recordEncounter(player);

        // A warm greeting line from the friendly pool, addressed to the opener.
        DialogueEntry greeting = DialogueManager.get().pickForStage(CorruptionStage.FRIENDLY, player, data);
        if (greeting != null) {
            VeritySpeech.speak(player, greeting, false);
        }

        // The box is single-use.
        stack.shrink(1);
    }
}

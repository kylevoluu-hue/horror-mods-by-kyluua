package com.kyluua.verity.item;

import com.kyluua.verity.Verity;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Verity Box - the cardboard box Verity ships inside.
 *
 * <p>Right-clicking "opens" the box (works whether you are aiming at a block or at
 * the air): the companion spawns just in front of the player, lets out its excited
 * "Hey, let me out!" and greets the opener. This is the player's first contact with
 * Verity, so it also seeds a little corruption and registers the opener as "owner".</p>
 */
public class VerityBoxItem extends Item {

    public VerityBoxItem(Properties properties) {
        super(properties);
    }

    /** Right-click in the air. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            openBox(serverLevel, serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Right-click while aiming at a block - open the box here too (don't place anything). */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null
                && ctx.getLevel() instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            openBox(serverLevel, serverPlayer, ctx.getItemInHand());
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
    }

    /** Spawns Verity, plays the intro voice + greeting, and consumes the box. */
    private void openBox(ServerLevel level, ServerPlayer player, ItemStack stack) {
        // Immediate, unmistakable feedback so the player knows the box opened
        // (the audio is a placeholder, so this text is Verity's "voice" for now).
        VeritySpeech.say(player, "Hey — let me out!", false);

        // Spawn horizontally in front of the player at chest height, so it never
        // ends up buried in the ground when the player is looking down.
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 spawn = player.position().add(flat.scale(1.5D)).add(0, 1.0D, 0);

        try {
            VerityCompanionEntity verity = VerityEntities.VERITY_COMPANION.get().create(level);
            if (verity != null) {
                verity.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0);
                verity.setOwnerId(player.getUUID());
                verity.setPersistenceRequired();
                level.addFreshEntity(verity);
                level.playSound(null, spawn.x, spawn.y, spawn.z,
                        VeritySounds.VOICE_LETMEOUT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            } else {
                Verity.LOGGER.error("[Verity] Companion entity type produced a null entity.");
            }
        } catch (Exception ex) {
            Verity.LOGGER.error("[Verity] Failed to open box / spawn companion", ex);
        }

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

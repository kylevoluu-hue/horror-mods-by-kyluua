package com.kyluua.verity.command;

import com.kyluua.verity.dialogue.DialogueEntry;
import com.kyluua.verity.dialogue.DialogueManager;
import com.kyluua.verity.entity.VerityCompanionEntity;
import com.kyluua.verity.event.ScareDirector;
import com.kyluua.verity.event.ServerEvents;
import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.progression.CorruptionStage;
import com.kyluua.verity.registry.VerityEntities;
import com.kyluua.verity.util.StructureLocator;
import com.kyluua.verity.util.VeritySpeech;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

/**
 * The {@code /verity} command tree.
 *
 * <p>Player-facing verbs ({@code summon}, {@code dismiss}, {@code ask},
 * {@code point}) are available to everyone; administrative verbs
 * ({@code corruption}, {@code stage}, {@code event}, {@code reset}) require
 * permission level 2, so only operators / server admins can drive progression
 * directly.</p>
 */
public final class VerityCommand {

    private VerityCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("verity");

        // ---- Player commands ------------------------------------------------
        root.then(Commands.literal("summon").executes(ctx -> summon(ctx.getSource())));
        root.then(Commands.literal("dismiss").executes(ctx -> dismiss(ctx.getSource())));

        root.then(Commands.literal("ask")
                .then(Commands.argument("question", StringArgumentType.greedyString())
                        .executes(ctx -> ask(ctx.getSource(), StringArgumentType.getString(ctx, "question")))));

        root.then(Commands.literal("point")
                .then(Commands.literal("village").executes(ctx -> point(ctx.getSource(), "village")))
                .then(Commands.literal("structure").executes(ctx -> point(ctx.getSource(), "structure")))
                .then(Commands.literal("ore").executes(ctx -> point(ctx.getSource(), "ore"))));

        // ---- Admin commands (permission level 2) ----------------------------
        root.then(Commands.literal("corruption").requires(s -> s.hasPermission(2))
                .then(Commands.literal("get").executes(ctx -> getCorruption(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> setCorruption(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                .executes(ctx -> addCorruption(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))));

        LiteralArgumentBuilder<CommandSourceStack> stage = Commands.literal("stage").requires(s -> s.hasPermission(2));
        for (CorruptionStage st : CorruptionStage.values()) {
            stage.then(Commands.literal(st.name().toLowerCase())
                    .executes(ctx -> setStage(ctx.getSource(), st)));
        }
        root.then(stage);

        // Corruption rate over time (OP-settable, persisted, server-wide).
        root.then(Commands.literal("rate").requires(s -> s.hasPermission(2))
                .then(Commands.literal("get").executes(ctx -> getRate(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.0))
                                .executes(ctx -> setRate(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "multiplier")))))
                .then(Commands.literal("time")
                        .then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0.1))
                                .executes(ctx -> setRateTime(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "minutes")))))
                .then(Commands.literal("reset").executes(ctx -> resetRate(ctx.getSource()))));

        root.then(Commands.literal("event").requires(s -> s.hasPermission(2))
                .then(Commands.literal("scare").executes(ctx -> forceScare(ctx.getSource())))
                .then(Commands.literal("boss").executes(ctx -> spawnBoss(ctx.getSource()))));

        root.then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                .executes(ctx -> reset(ctx.getSource())));

        dispatcher.register(root);
    }

    // =========================================================================
    //  Player verbs
    // =========================================================================

    private static int summon(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 spawn = player.getEyePosition().add(player.getLookAngle().scale(2.0));
        VerityCompanionEntity verity = VerityEntities.VERITY_COMPANION.get().create(level);
        if (verity == null) return 0;
        verity.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0);
        verity.setOwnerId(player.getUUID());
        verity.setPersistenceRequired();
        level.addFreshEntity(verity);
        source.sendSuccess(() -> Component.literal("Verity has appeared.").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int dismiss(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        var list = level.getEntitiesOfClass(VerityCompanionEntity.class, player.getBoundingBox().inflate(64));
        list.forEach(VerityCompanionEntity::discard);
        source.sendSuccess(() -> Component.literal("Dismissed " + list.size() + " companion(s).").withStyle(ChatFormatting.GRAY), false);
        return list.size();
    }

    private static int ask(CommandSourceStack source, String question) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CorruptionData data = CorruptionData.get(player.serverLevel());
        data.addInteraction(1.0);
        data.recordEncounter(player);
        CorruptionStage stage = data.getStage();
        DialogueEntry reply = DialogueManager.get().pickForStage(stage, player, data);
        if (reply != null) {
            VeritySpeech.speak(player, reply, stage.isHorror());
        }
        return 1;
    }

    private static int point(CommandSourceStack source, String what) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();

        BlockPos found = null;
        if (what.equals("village") || what.equals("structure")) {
            Optional<BlockPos> opt = StructureLocator.nearestStructure(level, origin, StructureLocator.VILLAGE, 100);
            found = opt.orElse(null);
        } else if (what.equals("ore")) {
            found = StructureLocator.nearestBlock(level, origin,
                    net.minecraft.tags.BlockTags.IRON_ORES, 24);
        }

        if (found == null) {
            player.sendSystemMessage(Component.literal("Verity » I... can't sense one nearby.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        String dir = compassDirection(origin, found);
        BlockPos f = found;
        player.sendSystemMessage(Component.literal(
                "Verity » That way - to the " + dir + ". (" + f.getX() + ", " + f.getZ() + ")")
                .withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    // =========================================================================
    //  Admin verbs
    // =========================================================================

    private static int getCorruption(CommandSourceStack source) {
        CorruptionData data = CorruptionData.get(source.getLevel());
        source.sendSuccess(() -> Component.literal(
                "Corruption: " + data.getLevel() + "/100 (stage " + data.getStage().displayNumber()
                        + " - " + data.getStage().name() + ")").withStyle(ChatFormatting.AQUA), false);
        return data.getLevel();
    }

    private static int setCorruption(CommandSourceStack source, int value) {
        ServerLevel level = source.getLevel();
        CorruptionData data = CorruptionData.get(level);
        data.setLevel(value);
        ServerEvents.resetBroadcastCache();
        com.kyluua.verity.network.VerityNetwork.toAll(level.getServer(),
                new com.kyluua.verity.network.CorruptionSyncPacket(data.getLevel(), data.getStage().ordinal()));
        source.sendSuccess(() -> Component.literal("Corruption set to " + value).withStyle(ChatFormatting.GREEN), true);
        return value;
    }

    private static int addCorruption(CommandSourceStack source, int amount) {
        CorruptionData data = CorruptionData.get(source.getLevel());
        return setCorruption(source, data.getLevel() + amount);
    }

    private static int setStage(CommandSourceStack source, CorruptionStage stage) {
        return setCorruption(source, stage.threshold);
    }

    // ---- Corruption rate over time -----------------------------------------

    private static int getRate(CommandSourceStack source) {
        CorruptionData data = CorruptionData.get(source.getLevel());
        double speed = data.getEffectiveSpeed();
        double minutes = data.estimatedMinutesToFull();
        String src = data.hasRateOverride() ? "command override" : "config (progressionSpeed)";
        source.sendSuccess(() -> Component.literal(String.format(
                "Corruption rate: x%.2f (%s). ~%.1f min to go 0 -> 100 of playtime.",
                speed, src, minutes)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int setRate(CommandSourceStack source, double multiplier) {
        CorruptionData data = CorruptionData.get(source.getLevel());
        data.setSpeedMultiplier(multiplier);
        source.sendSuccess(() -> Component.literal(String.format(
                "Corruption speed set to x%.2f (~%.1f min to fully corrupt).",
                data.getEffectiveSpeed(), data.estimatedMinutesToFull()))
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setRateTime(CommandSourceStack source, double minutes) {
        CorruptionData data = CorruptionData.get(source.getLevel());
        data.setTimeToFull(minutes);
        source.sendSuccess(() -> Component.literal(String.format(
                "Verity will fully corrupt in ~%.1f min of playtime (speed x%.2f).",
                minutes, data.getEffectiveSpeed())).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int resetRate(CommandSourceStack source) {
        CorruptionData data = CorruptionData.get(source.getLevel());
        data.resetRate();
        source.sendSuccess(() -> Component.literal(String.format(
                "Corruption rate reset to config (x%.2f).", data.getEffectiveSpeed()))
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int forceScare(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        CorruptionData data = CorruptionData.get(level);
        // Temporarily ensure we are at least unsettling so a scare can fire.
        if (data.getStage() == CorruptionStage.FRIENDLY) {
            source.sendFailure(Component.literal("Verity is still friendly - raise corruption first."));
            return 0;
        }
        ScareDirector.tick(level, data);
        source.sendSuccess(() -> Component.literal("Nudged the scare director.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int spawnBoss(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerEvents.spawnBoss(player.serverLevel(), player);
        CorruptionData.get(player.serverLevel()).setBossTriggered(true);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        CorruptionData data = CorruptionData.get(level);
        data.setLevel(0);
        data.setBossTriggered(false);
        ScareDirector.reset();
        ServerEvents.resetBroadcastCache();
        com.kyluua.verity.network.VerityNetwork.toAll(level.getServer(),
                new com.kyluua.verity.network.CorruptionSyncPacket(0, 0));
        source.sendSuccess(() -> Component.literal("Verity has been reset.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** Rough 8-point compass direction from a to b. */
    private static String compassDirection(BlockPos a, BlockPos b) {
        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        if (angle < 0) angle += 360;
        String[] dirs = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        return dirs[(int) Math.round(angle / 45.0) % 8];
    }
}

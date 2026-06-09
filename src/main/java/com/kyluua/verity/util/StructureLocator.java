package com.kyluua.verity.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Helpers for Verity's "let me point you to something" feature. Wraps vanilla
 * structure location and a small ore scan so {@code /verity point} (and friendly
 * stage tips) can nudge players toward villages, structures or nearby ores.
 */
public final class StructureLocator {

    private StructureLocator() {}

    /** Vanilla tag of structures eligible when the player asks for "structure". */
    public static final TagKey<Structure> VILLAGE =
            TagKey.create(Registries.STRUCTURE, net.minecraft.resources.ResourceLocation.withDefaultNamespace("village"));

    /**
     * Finds the nearest structure of the given tag, returning its position or empty.
     *
     * @param searchRadius in chunks
     */
    public static Optional<BlockPos> nearestStructure(ServerLevel level, BlockPos origin, TagKey<Structure> tag, int searchRadius) {
        var found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, level.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE).getOrCreateTag(tag), origin, searchRadius, false);
        return Optional.ofNullable(found).map(com.mojang.datafixers.util.Pair::getFirst);
    }

    /**
     * Scans a cube around the origin for the nearest block matching {@code oreTag}.
     * Deliberately bounded so it never threatens TPS.
     *
     * @param radius blocks (kept small, e.g. 24)
     */
    @Nullable
    public static BlockPos nearestBlock(ServerLevel level, BlockPos origin, TagKey<Block> oreTag, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(oreTag)) {
                        double d = origin.distSqr(cursor);
                        if (d < bestDist) {
                            bestDist = d;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }
}

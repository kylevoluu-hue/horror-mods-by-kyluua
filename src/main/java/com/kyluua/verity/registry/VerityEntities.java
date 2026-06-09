package com.kyluua.verity.registry;

import com.kyluua.verity.Verity;
import com.kyluua.verity.entity.HallucinationEntity;
import com.kyluua.verity.entity.VerityBossEntity;
import com.kyluua.verity.entity.VerityCompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;

/**
 * Entity types for Verity, plus their attribute registration.
 *
 * <p>Three entities exist: the {@link VerityCompanionEntity} (the smiley), the
 * {@link VerityBossEntity} (the final form) and the {@link HallucinationEntity}
 * (player-specific fake sightings).</p>
 */
public final class VerityEntities {

    private VerityEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Verity.MOD_ID);

    /** The floating smiley companion. */
    public static final Supplier<EntityType<VerityCompanionEntity>> VERITY_COMPANION =
            ENTITY_TYPES.register("verity_companion",
                    () -> EntityType.Builder.of(VerityCompanionEntity::new, MobCategory.CREATURE)
                            .sized(0.7F, 0.7F)
                            .clientTrackingRange(10)
                            .build("verity_companion"));

    /** The tall humanoid final boss. */
    public static final Supplier<EntityType<VerityBossEntity>> VERITY_BOSS =
            ENTITY_TYPES.register("verity_boss",
                    () -> EntityType.Builder.of(VerityBossEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 3.9F)            // very tall, thin
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("verity_boss"));

    /** Player-specific hallucination sighting (usually spawned client-side only). */
    public static final Supplier<EntityType<HallucinationEntity>> HALLUCINATION =
            ENTITY_TYPES.register("hallucination",
                    () -> EntityType.Builder.of(HallucinationEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(12)
                            .noSummon()                   // not spawnable via /summon
                            .build("hallucination"));

    /** Registers default attributes for every mob. Fired on the mod event bus. */
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(VERITY_COMPANION.get(), VerityCompanionEntity.createAttributes().build());
        event.put(VERITY_BOSS.get(), VerityBossEntity.createAttributes().build());
        event.put(HALLUCINATION.get(), HallucinationEntity.createAttributes().build());
    }
}

package com.kyluua.verity.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A hallucination - a fake sighting that exists only to unnerve a single player.
 *
 * <p>Hallucinations are normally spawned <em>client-side only</em>: the scare
 * director sends a {@code HallucinationPacket} to one player, whose client builds
 * this entity locally and drops it into the client level. Because it never exists
 * on the server, only that one player ever sees it - perfect for player-specific
 * paranoia. It has no collision, takes no damage, ignores gravity, and quietly
 * removes itself after a short lifespan or the moment the player gets close (so it
 * always "disappears when you look properly").</p>
 */
public class HallucinationEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation STARE = RawAnimation.begin().thenLoop("stare");

    /** Ticks remaining before this sighting fades on its own. */
    private int lifespan = 100;

    public HallucinationEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void setLifespan(int ticks) {
        this.lifespan = ticks;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data: hallucinations are purely local.
    }

    @Override
    public void tick() {
        super.tick();
        // Always face the nearest player so it feels like it is watching them.
        Player nearest = this.level().getNearestPlayer(this, 32.0D);
        if (nearest != null) {
            this.getLookControl().setLookAt(nearest, 60.0F, 60.0F);
            // Vanish the instant the player closes in - "it was never there".
            if (this.distanceToSqr(nearest) < 16.0D) {
                this.discard();
                return;
            }
        }
        if (--lifespan <= 0) {
            this.discard();
        }
    }

    // Never interfere with the world.
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 0, state -> state.setAndContinue(STARE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

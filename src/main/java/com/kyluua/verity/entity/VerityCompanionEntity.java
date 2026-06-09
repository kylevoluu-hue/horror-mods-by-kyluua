package com.kyluua.verity.entity;

import com.kyluua.verity.progression.CorruptionData;
import com.kyluua.verity.progression.CorruptionStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * The Verity companion - the floating yellow smiley that the player releases from
 * the box. It is a flying, no-gravity {@link PathfinderMob} that hovers near and
 * follows the nearest player.
 *
 * <p>The entity itself is deliberately "dumb": it knows how to float, follow, look
 * at players and show a face. <em>What</em> face it shows and <em>when</em> it does
 * something creepy is driven by the server-side {@link CorruptionData} and the
 * scare director, so all of the horror stays authoritative and multiplayer-safe.</p>
 *
 * <p>Animations are handled by GeckoLib via three controllers (idle bob, smile,
 * and a distortion overlay) that key off the synced {@link VeritySmileState}.</p>
 */
public class VerityCompanionEntity extends PathfinderMob implements GeoEntity {

    /** Synced face state ordinal so every client renders the same smile. */
    private static final EntityDataAccessor<Integer> DATA_SMILE =
            SynchedEntityData.defineId(VerityCompanionEntity.class, EntityDataSerializers.INT);

    /** Synced 0-100 corruption mirror, used purely for client-side glow/tint intensity. */
    private static final EntityDataAccessor<Integer> DATA_CORRUPTION =
            SynchedEntityData.defineId(VerityCompanionEntity.class, EntityDataSerializers.INT);

    /** Synced flag: is Verity currently staring straight at a player? Drives a special anim. */
    private static final EntityDataAccessor<Boolean> DATA_STARING =
            SynchedEntityData.defineId(VerityCompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- GeckoLib animation references ---------------------------------------
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("float");
    private static final RawAnimation STARE = RawAnimation.begin().thenLoop("stare");
    private static final RawAnimation DISTORT = RawAnimation.begin().thenLoop("distort");

    /** UUID of the player who first opened the box - used by dialogue memory. */
    @Nullable
    private UUID ownerId;

    public VerityCompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Hover instead of walk; pathfind through air; ignore fall damage entirely.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    /** Base attributes. Verity is fragile and slow on purpose - it is a presence, not a fighter. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        // Hover-follow the nearest player, and keep looking at whoever is close.
        this.goalSelector.addGoal(1, new HoverFollowGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SMILE, VeritySmileState.NORMAL.ordinal());
        builder.define(DATA_CORRUPTION, 0);
        builder.define(DATA_STARING, false);
    }

    // =========================================================================
    //  Server tick: keep face/corruption in sync with the world's corruption.
    // =========================================================================
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server && this.tickCount % 20 == 0) {
            CorruptionData data = CorruptionData.get(server);
            int level = data.getLevel();
            setCorruptionMirror(level);
            setSmileState(VeritySmileState.forStage(CorruptionStage.fromLevel(level)));
        }
    }

    // =========================================================================
    //  Behaviour hooks used by the scare director (all called server-side).
    // =========================================================================

    /** Silently relocates Verity just behind a player - the "it was right there" scare. */
    public void teleportBehind(Player player) {
        Vec3 look = player.getLookAngle().scale(-2.5D);
        Vec3 pos = player.position().add(look).add(0, player.getEyeHeight() * 0.5D, 0);
        this.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0);
        this.getNavigation().stop();
    }

    /** Briefly vanish (server hides it by moving far + making it invisible) then it returns next tick batch. */
    public void blink() {
        this.setInvisible(true);
    }

    public void unblink() {
        this.setInvisible(false);
    }

    public void setStaring(boolean staring) {
        this.entityData.set(DATA_STARING, staring);
    }

    public boolean isStaring() {
        return this.entityData.get(DATA_STARING);
    }

    public void setSmileState(VeritySmileState state) {
        this.entityData.set(DATA_SMILE, state.ordinal());
    }

    public VeritySmileState getSmileState() {
        return VeritySmileState.values()[this.entityData.get(DATA_SMILE)];
    }

    public void setCorruptionMirror(int level) {
        this.entityData.set(DATA_CORRUPTION, level);
    }

    /** 0-100 corruption, read by the client renderer for glow/tint. */
    public int getCorruptionMirror() {
        return this.entityData.get(DATA_CORRUPTION);
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
    }

    // --- Make the companion behave like a hovering presence ------------------
    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false; // never takes fall damage
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // persistent - Verity does not despawn naturally
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("VerityOwner", ownerId);
        }
        tag.putInt("VeritySmile", this.entityData.get(DATA_SMILE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("VerityOwner")) {
            ownerId = tag.getUUID("VerityOwner");
        }
        if (tag.contains("VeritySmile")) {
            this.entityData.set(DATA_SMILE, tag.getInt("VeritySmile"));
        }
    }

    // =========================================================================
    //  GeckoLib
    // =========================================================================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement/idle controller: float while moving, idle bob otherwise.
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (this.isStaring()) {
                return state.setAndContinue(STARE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(FLOAT);
            }
            return state.setAndContinue(IDLE);
        }));

        // Corruption overlay: once distorted, a subtle wobble/glitch plays on top.
        controllers.add(new AnimationController<>(this, "distortion", 2, state -> {
            if (getSmileState().ordinal() >= VeritySmileState.DISTORTED.ordinal()) {
                return state.setAndContinue(DISTORT);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================================
    //  Goal: hover near and follow the nearest player.
    // =========================================================================
    private static final class HoverFollowGoal extends Goal {
        private final VerityCompanionEntity verity;
        @Nullable private Player target;

        HoverFollowGoal(VerityCompanionEntity verity) {
            this.verity = verity;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            this.target = verity.level().getNearestPlayer(verity, 64.0D);
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && verity.distanceToSqr(target) > 9.0D;
        }

        @Override
        public void tick() {
            if (target == null) return;
            verity.getLookControl().setLookAt(target, 30.0F, 30.0F);
            // Aim for a point at the player's eye level, a couple of blocks out.
            double hover = target.getEyeY() + 0.6D;
            verity.getNavigation().moveTo(target.getX(), hover, target.getZ(), 1.0D);
        }
    }
}

package com.kyluua.verity.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerBossEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The final form of Verity: a tall, thin, pale humanoid horror (see reference
 * IMG_3883). This is a multi-phase boss with a server-synced boss bar.
 *
 * <p>Phases are driven by health: as the boss loses health it speeds up, its
 * movement gets more unnatural, and (via the synced phase value) the renderer
 * swaps to more aggressive animations. The fight is spawned by a server-wide
 * event when corruption reaches 100 (or via {@code /verity event boss}).</p>
 */
public class VerityBossEntity extends Monster implements GeoEntity {

    /** Synced phase (1-3) so clients can switch animation sets. */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(VerityBossEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** The pink boss bar shown to nearby players, kept in sync server-side. */
    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation LUNGE = RawAnimation.begin().thenPlay("lunge");
    private static final RawAnimation RAGE = RawAnimation.begin().thenLoop("rage");

    public VerityBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.xpReward = 100;
    }

    /** Tall, slow, heavy-hitting and durable - a looming threat rather than a swarm. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PHASE, 1);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Keep the boss bar progress and phase in sync with health (server side only).
        if (!this.level().isClientSide) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            updatePhase();
        }
    }

    /** Three phases at 100% / 66% / 33% health, each faster and more aggressive. */
    private void updatePhase() {
        float pct = this.getHealth() / this.getMaxHealth();
        int phase = pct > 0.66F ? 1 : (pct > 0.33F ? 2 : 3);
        if (phase != this.entityData.get(DATA_PHASE)) {
            this.entityData.set(DATA_PHASE, phase);
            // Each new phase ramps speed for that "unnatural acceleration" feel.
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.26D + 0.06D * (phase - 1));
        }
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    // --- Boss bar visibility -------------------------------------------------
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    // =========================================================================
    //  GeckoLib
    // =========================================================================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (getPhase() >= 3) {
                return state.setAndContinue(RAGE);
            }
            if (this.swinging) {
                return state.setAndContinue(LUNGE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Convenience used by the spawn event to finalize the boss after placing it. */
    public static void prepare(VerityBossEntity boss) {
        boss.setPersistenceRequired();
    }
}

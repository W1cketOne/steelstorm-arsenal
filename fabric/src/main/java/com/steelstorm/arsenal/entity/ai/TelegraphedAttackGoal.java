package com.steelstorm.arsenal.entity.ai;

import com.steelstorm.arsenal.entity.SteelstormMonster;
import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A big attack with a visible wind-up: the mob stops, raises its arms (synced "charging" flag),
 * shows warning particles and plays a sound, then strikes. Players get {@code windup} ticks to react.
 */
public class TelegraphedAttackGoal extends Goal {
    private final SteelstormMonster mob;
    private final double minRange;
    private final double maxRange;
    private final int windup;
    private final int cooldown;
    private final SoundEvent warnSound;
    private final Consumer<LivingEntity> strike;
    private final BooleanSupplier enabled;
    private int timer;
    private int recovery;
    private long readyAt;

    public TelegraphedAttackGoal(SteelstormMonster mob, double minRange, double maxRange, int windup, int cooldown,
                                 SoundEvent warnSound, BooleanSupplier enabled, Consumer<LivingEntity> strike) {
        this.mob = mob;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.windup = windup;
        this.cooldown = cooldown;
        this.warnSound = warnSound;
        this.enabled = enabled;
        this.strike = strike;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.level().getGameTime() < readyAt || !enabled.getAsBoolean()) {
            return false;
        }
        double dist = mob.distanceTo(target);
        return dist >= minRange && dist <= maxRange && mob.hasLineOfSight(target) && mob.getRandom().nextInt(4) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        // Keep running through the recovery pause after the strike: that's the player's opening.
        return recovery > 0 || (timer > 0 && mob.getTarget() != null && mob.getTarget().isAlive());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        timer = windup;
        recovery = 0;
        mob.setCharging(true);
        mob.getNavigation().stop();
        mob.playSound(warnSound, 1.2F, 0.8F);
    }

    @Override
    public void tick() {
        if (recovery > 0) {
            recovery--;
            mob.getNavigation().stop();
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (mob.level() instanceof ServerLevel level && timer % 3 == 0) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.4, mob.getZ(), 1, 0.1, 0.1, 0.1, 0);
            level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY(0.8), mob.getZ(), 4, 0.4, 0.4, 0.4, 0.05);
        }
        if (--timer <= 0) {
            mob.setCharging(false);
            if (target != null && target.isAlive()) {
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                com.steelstorm.arsenal.network.PlayerAnimPayload.send(mob, com.steelstorm.arsenal.anim.CastPose.SLAM);
                strike.accept(target);
            }
            recovery = Math.max(10, windup);
        }
    }

    @Override
    public void stop() {
        mob.setCharging(false);
        readyAt = mob.level().getGameTime() + cooldown;
        timer = 0;
        recovery = 0;
    }

    public static void playAt(LivingEntity mob, SoundEvent sound, float volume, float pitch) {
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }
}

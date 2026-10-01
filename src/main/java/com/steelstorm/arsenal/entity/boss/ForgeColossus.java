package com.steelstorm.arsenal.entity.boss;

import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.entity.ai.MobStrikes;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A three-block-tall war machine of iron and basalt with a molten heart, built to guard a forge.
 * Pounds the ground, sweeps its fists round, charges, and rains molten slag; at half health it
 * overheats, cracks glowing, and starts erupting.
 */
public class ForgeColossus extends AnimatedBoss {
    public static final int POUND = 1;
    public static final int SWEEP = 2;
    public static final int BARRAGE = 3;
    public static final int CHARGE = 4;
    public static final int ERUPTION = 5;
    public static final int OVERHEAT = 6;
    private static final int MOLTEN = 0xFF7A1A;
    private static final int HOT = 0xFFD27A;

    private final List<BossMove> moves = List.of(
            BossMove.of(POUND, 36, 0, 7, 70, this::pound).weight(14),
            BossMove.of(SWEEP, 34, 0, 5.5, 90, this::sweep).weight(12),
            BossMove.of(BARRAGE, 50, 5, 28, 140, this::barrage).weight(10),
            BossMove.of(CHARGE, 34, 7, 22, 120, this::charge).weight(9),
            BossMove.of(ERUPTION, 56, 0, 14, 200, this::eruption).weight(10).when(this::enraged));
    private final BossMove overheat = BossMove.of(OVERHEAT, 40, 0, 100, 0, this::overheat);
    private final Set<Integer> chargeHits = new HashSet<>();

    public ForgeColossus(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 420.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MoveGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            protected int getAttackInterval() {
                return adjustedTickDelay(30);
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected List<BossMove> moves() {
        return moves;
    }

    @Override
    protected BossMove phaseTwoMove() {
        return overheat;
    }

    private float dmg(float multiplier) {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
    }

    private ServerLevel server() {
        return (ServerLevel) level();
    }

    private void shake(Vec3 at, double radius, float strength, int ticks) {
        for (ServerPlayer p : server().players()) {
            if (p.distanceToSqr(at) < radius * radius) {
                Stamina.shake(p, strength, ticks);
            }
        }
    }

    // ------------------------------------------------------------------ moves

    /** Both fists raised high, then brought down: a crater and shockwave rings. */
    private void pound(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.IRON_GOLEM_HURT, 2.0F, 0.5F);
            Fx.circle(level, position(), this, 0xFF3B1F, MOLTEN, 8.0F, 20);
        }
        if (t == 20) {
            Vec3 at = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
            Shockwaves.ring(level, this, at, 9.0F, 0.7F, dmg(1.0F), 0.9, MOLTEN, e -> e.igniteForSeconds(3));
            Shockwaves.crater(level, at, 3.0F, 1.4F);
            Fx.halo(level, at, MOLTEN, HOT, 10.0F, 18);
            Fx.impact(level, at.add(0, 0.5, 0), MOLTEN, 3.2F);
            Fx.sound(level, at, ModSounds.ABILITY_SHOCKWAVE, 2.0F, 0.6F);
            Fx.sound(level, at, ModSounds.ABILITY_GROUND_CRACK, 2.0F, 0.6F);
            shake(at, 24, 1.6F, 14);
        }
        if (t == 27 && enraged()) {
            Shockwaves.ring(level, this, position(), 13.0F, 0.9F, dmg(0.6F), 0.6, HOT, null);
            Fx.halo(level, position(), HOT, MOLTEN, 13.0F, 20);
        }
    }

    /** One fist sweeps all the way round, then the other back. */
    private void sweep(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 2) {
            playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 1.4F);
        }
        if (t == 14 || t == 24) {
            for (LivingEntity e : MobStrikes.victims(this, 6.0)) {
                if (CombatUtil.specialHurt(this, e, dmg(0.8F))) {
                    Vec3 away = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                    e.setDeltaMovement(away.x * 1.6, 0.55, away.z * 1.6);
                    e.hurtMarked = true;
                }
            }
            for (int k = 0; k < 4; k++) {
                float yaw = getYRot() + k * 90 + (t == 24 ? 45 : 0);
                Vec3 at = position().add(Vec3.directionFromRotation(0, yaw).scale(3)).add(0, 1.5, 0);
                Fx.slash(level, at, yaw + 90, 0, 0, MOLTEN, 2.0F);
            }
            Fx.halo(level, position(), MOLTEN, HOT, 6.5F, 10);
            Fx.sound(level, position(), ModSounds.WEAPON_SWING_HEAVY, 2.0F, 0.5F);
            shake(position(), 12, 0.7F, 6);
        }
    }

    /** The heart flares and lobs molten slag; every landing spot is marked a moment before it hits. */
    private void barrage(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
            Fx.pillar(level, position(), MOLTEN, HOT, 1.2F, 6, 40);
        }
        if (t >= 8 && t <= 38 && t % (enraged() ? 4 : 6) == 0) {
            Vec3 aim = target.position().add((random.nextDouble() - 0.5) * 6, 0, (random.nextDouble() - 0.5) * 6);
            BlockPos ground = Shockwaves.ground(level, aim.x, aim.y + 3, aim.z);
            Vec3 spot = ground == null ? aim : new Vec3(aim.x, ground.getY() + 1, aim.z);
            Fx.circle(level, spot, null, 0xFF3B1F, MOLTEN, 2.4F, 16);
            Vec3 from = position().add(0, 2.6, 0);
            for (int k = 1; k <= 6; k++) {
                Vec3 p = from.lerp(spot, k / 6.0).add(0, Math.sin(Math.PI * k / 6.0) * 4, 0);
                level.sendParticles(ModParticles.ORB.get().with(MOLTEN, 1.6F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            ServerScheduler.schedule(14, () -> {
                if (!isAlive()) {
                    return;
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(spot, spot).inflate(2.6, 2, 2.6),
                        e -> Shockwaves.canHit(this, e))) {
                    CombatUtil.specialHurt(this, e, dmg(0.75F));
                    e.igniteForSeconds(4);
                }
                Fx.sunburst(level, spot.add(0, 0.6, 0), MOLTEN, HOT, 3.0F, 12, 10);
                Fx.halo(level, spot, MOLTEN, HOT, 3.5F, 10);
                level.sendParticles(ModParticles.SMOKE.get().with(0x2A1A10, 1.8F), spot.x, spot.y + 0.5, spot.z, 10, 0.6, 0.4, 0.6, 0.03);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, spot.x, spot.y + 0.3, spot.z, 8, 0.5, 0.2, 0.5, 0.1);
                Fx.sound(level, spot, ModSounds.ABILITY_GROUND_CRACK, 1.2F, 0.9F);
            });
        }
    }

    /** Lowers its head and thunders forward, trampling everything in the way. */
    private void charge(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            chargeHits.clear();
            playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
        }
        if (t >= 10 && t < 24) {
            Vec3 dir = (t == 10 ? target.position().subtract(position()) : getDeltaMovement()).multiply(1, 0, 1).normalize();
            if (dir.lengthSqr() < 0.01) {
                dir = getLookAngle().multiply(1, 0, 1).normalize();
            }
            setDeltaMovement(dir.x * 0.85, getDeltaMovement().y, dir.z * 0.85);
            setYRot((float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90);
            yBodyRot = getYRot();
            hurtMarked = true;
            for (LivingEntity e : MobStrikes.victims(this, 2.4)) {
                if (chargeHits.add(e.getId()) && CombatUtil.specialHurt(this, e, dmg(0.9F))) {
                    e.setDeltaMovement(dir.x * 1.8, 0.7, dir.z * 1.8);
                    e.hurtMarked = true;
                }
            }
            level.sendParticles(ModParticles.SMOKE.get().with(0x5A4A40, 1.4F), getX(), getY() + 0.2, getZ(), 3, 0.8, 0.1, 0.8, 0.02);
            if (t % 3 == 0) {
                Fx.sound(level, position(), SoundEvents.IRON_GOLEM_STEP, 2.0F, 0.6F);
                shake(position(), 10, 0.4F, 4);
            }
        }
        if (t == 24) {
            setDeltaMovement(Vec3.ZERO);
            Shockwaves.ring(level, this, position(), 5.0F, 0.6F, dmg(0.6F), 0.5, MOLTEN, null);
            Fx.halo(level, position(), MOLTEN, HOT, 5.5F, 12);
        }
    }

    /** Phase two: geysers of molten rock burst from the ground in widening rings. */
    private void eruption(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.5F);
            Fx.gyro(level, position(), this, MOLTEN, HOT, 2.2F, 2.0F, 50);
        }
        if (t >= 16 && t <= 40 && (t - 16) % 8 == 0) {
            int ring = (t - 16) / 8;
            float r = 3.5F + ring * 3.0F;
            int count = 6 + ring * 3;
            for (int k = 0; k < count; k++) {
                double a = Math.PI * 2 * k / count + ring * 0.4;
                Vec3 spot = position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Fx.circle(level, spot, null, 0xFF3B1F, MOLTEN, 1.4F, 8);
                ServerScheduler.schedule(8, () -> {
                    if (!isAlive()) {
                        return;
                    }
                    Fx.pillar(level, spot, MOLTEN, HOT, 0.6F, 5.0F, 12);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, spot.x, spot.y + 0.3, spot.z, 4, 0.3, 0.2, 0.3, 0.1);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(spot, spot).inflate(1.4, 3, 1.4),
                            e -> Shockwaves.canHit(this, e))) {
                        if (CombatUtil.specialHurt(this, e, dmg(0.6F))) {
                            e.setDeltaMovement(e.getDeltaMovement().x, 1.0, e.getDeltaMovement().z);
                            e.hurtMarked = true;
                            e.igniteForSeconds(4);
                        }
                    }
                });
            }
            Fx.sound(level, position(), ModSounds.ABILITY_RUMBLE, 1.6F, 0.7F + ring * 0.1F);
            shake(position(), 20, 0.8F, 8);
        }
    }

    /** Below half health: roars, the molten heart cracks the armour open, and two Iron Revenants answer. */
    private void overheat(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.4F);
            Fx.pillar(level, position(), MOLTEN, HOT, 2.0F, 14.0F, 40);
            Fx.circle(level, position(), this, MOLTEN, HOT, 6.0F, 40);
        }
        if (t == 18) {
            setEnraged();
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0, false, false));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, false));
            Fx.halo(level, position(), HOT, MOLTEN, 14.0F, 24);
            Fx.sunburst(level, position().add(0, 2.5, 0), MOLTEN, HOT, 6.0F, 16, 20);
            Fx.sparkles(level, position().add(0, 2, 0), MOLTEN, 40, 2.0);
            Shockwaves.ring(level, this, position(), 8.0F, 0.8F, dmg(0.5F), 1.0, MOLTEN, null);
            shake(position(), 30, 2.0F, 20);
            for (int i = 0; i < 2; i++) {
                Mob minion = ModEntities.IRON_REVENANT.get().create(level);
                if (minion != null) {
                    Vec3 at = position().add(i == 0 ? 3 : -3, 0, 2);
                    minion.moveTo(at.x, at.y, at.z, getYRot(), 0);
                    minion.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    minion.setTarget(target);
                    level.addFreshEntity(minion);
                    Fx.pillar(level, at, MOLTEN, HOT, 0.8F, 5, 14);
                }
            }
        }
    }

    // ------------------------------------------------------------------ misc

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && tickCount % (enraged() ? 3 : 8) == 0) {
            // Embers drift off the heart.
            Vec3 heart = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(0.9)).add(0, 2.2, 0);
            level.sendParticles(ModParticles.GLOW.get().with(MOLTEN, 1.2F), heart.x, heart.y, heart.z, 1, 0.4, 0.4, 0.4, 0.02);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel level) {
            Fx.impact(level, target.position().add(0, 1, 0), MOLTEN, 1.6F);
            Fx.sound(level, position(), SoundEvents.IRON_GOLEM_ATTACK, 1.5F, 0.6F);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Its armour plates shrug off arrows; the molten heart (phase two) is softer.
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            amount *= 0.5F;
        }
        return super.hurt(source, enraged() ? amount * 1.15F : amount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }
}

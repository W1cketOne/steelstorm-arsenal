package com.steelstorm.arsenal.entity.boss;

import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.entity.SpectralWeaponEntity;
import com.steelstorm.arsenal.entity.ai.MobStrikes;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A four-armed duelist of moonlight, drifting a hand's breadth above the ground. Hurls crescents,
 * blinks behind you, dances in a storm of four blades and calls swords down from the sky; at half
 * health the moon eclipses and she fights twice as fast.
 */
public class MoonbladeRevenant extends AnimatedBoss {
    public static final int CRESCENTS = 1;
    public static final int BLINK = 2;
    public static final int DANCE = 3;
    public static final int MOONFALL = 4;
    public static final int ECLIPSE = 5;
    private static final int MOON = 0x9CC4FF;
    private static final int PALE = 0xE8F0FF;
    private static final int SOUL = 0xB48CFF;

    private final List<BossMove> moves = List.of(
            BossMove.of(CRESCENTS, 30, 3, 22, 60, this::crescents).weight(12),
            BossMove.of(BLINK, 28, 4, 18, 90, this::blink).weight(10),
            BossMove.of(DANCE, 50, 0, 6, 110, this::dance).weight(12),
            BossMove.of(MOONFALL, 60, 0, 24, 180, this::moonfall).weight(9).when(this::enraged));
    private final BossMove eclipse = BossMove.of(ECLIPSE, 40, 0, 100, 0, this::eclipse);

    public MoonbladeRevenant(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        xpReward = 180;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MoveGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override
            protected int getAttackInterval() {
                return adjustedTickDelay(enraged() ? 12 : 20);
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
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
        return eclipse;
    }

    private float dmg(float multiplier) {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
    }

    private ServerLevel server() {
        return (ServerLevel) level();
    }

    private ItemStack sword() {
        return new ItemStack(ModItems.legendary(LegendaryWeaponItem.Legendary.MOONVEIL).get());
    }

    // ------------------------------------------------------------------ moves

    /** Three crescents of moonlight, one from each of three swords in turn. */
    private void crescents(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.5F, 1.3F);
        }
        if (t == 10 || t == 15 || t == 20 || (enraged() && t == 25)) {
            Vec3 from = getEyePosition().add(0, -0.5, 0);
            Vec3 dir = target.getEyePosition().subtract(from).normalize();
            float roll = t == 10 ? 30 : t == 15 ? -30 : t == 20 ? 90 : 0;
            SlashWaveEntity.fire(this, from, dir, 1.1F, 26, 1.6F, roll, dmg(0.7F), MOON, false,
                    e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)));
            Fx.sparkles(level, from, MOON, 8, 0.4);
            Fx.sound(level, from, ModSounds.ABILITY_SLASH_WAVE, 1.2F, 1.4F);
        }
    }

    /** Vanishes in petals of light and reappears behind you, both upper swords crossing. */
    private void blink(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 4) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
            BlockPos ground = Shockwaves.ground(level, behind.x, behind.y + 2, behind.z);
            if (ground != null) {
                behind = new Vec3(behind.x, ground.getY() + 1, behind.z);
            }
            Fx.sparkles(level, position().add(0, 1.5, 0), MOON, 24, 0.8);
            Fx.halo(level, position(), MOON, PALE, 3.0F, 10);
            teleportTo(behind.x, behind.y, behind.z);
            Vec3 face = target.position().subtract(behind);
            setYRot((float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90);
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            Fx.sparkles(level, position().add(0, 1.5, 0), PALE, 24, 0.8);
            Fx.halo(level, position(), PALE, MOON, 3.0F, 10);
            Fx.sound(level, position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.4F);
        }
        if (t == 12) {
            MobStrikes.cone(this, 4.0, 80, dmg(1.1F), 0.6);
            Vec3 at = getEyePosition().add(getLookAngle().scale(1.4));
            Fx.slash(level, at, getYRot(), 0, 45, MOON, 1.8F);
            Fx.slash(level, at, getYRot(), 0, -45, PALE, 1.8F);
            Fx.sound(level, at, ModSounds.ABILITY_KATANA_DRAW, 1.2F, 1.2F);
        }
    }

    /** Spins with all four swords out, pulling everything into the whirl. */
    private void dance(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            Fx.gyro(level, position(), this, MOON, PALE, 3.2F, 1.4F, 50);
            Fx.circle(level, position(), this, SOUL, MOON, 5.0F, 50);
            playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5F, 1.2F);
        }
        if (t >= 8 && t <= 44 && t % 4 == 0) {
            for (LivingEntity e : MobStrikes.victims(this, 5.0)) {
                if (CombatUtil.specialHurt(this, e, dmg(0.35F))) {
                    Vec3 in = position().subtract(e.position()).multiply(1, 0, 1).normalize().scale(0.35);
                    e.setDeltaMovement(in.x, 0.1, in.z);
                    e.hurtMarked = true;
                }
            }
            for (int k = 0; k < 4; k++) {
                float yaw = t * 30 + k * 90;
                Vec3 at = position().add(Vec3.directionFromRotation(0, yaw).scale(2.2)).add(0, 1.4, 0);
                Fx.slash(level, at, yaw + 90, 0, 0, k % 2 == 0 ? MOON : SOUL, 1.2F);
            }
            Fx.sound(level, position(), ModSounds.WEAPON_SWING, 1.0F, 1.2F + (t % 8) * 0.03F);
        }
    }

    /** Phase two: spectral swords fall around you in rings, each landing spot marked first. */
    private void moonfall(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.5F, 1.0F);
            Fx.pillar(level, position(), MOON, PALE, 1.0F, 10, 40);
        }
        if (t >= 10 && t <= 46 && t % 6 == 4) {
            int wave = (t - 10) / 6;
            Vec3 c = target.position();
            for (int k = 0; k < 3; k++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = wave == 0 && k == 0 ? 0 : 1.5 + random.nextDouble() * 4;
                Vec3 spot = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                BlockPos ground = Shockwaves.ground(level, spot.x, spot.y + 3, spot.z);
                Vec3 at = ground == null ? spot : new Vec3(spot.x, ground.getY() + 1, spot.z);
                Fx.circle(level, at, null, SOUL, MOON, 1.8F, 14);
                SpectralWeaponEntity.drop(level, sword(), at, 2.2F, MOON, 6, 9, 6, 20, sw -> {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.9, 2, 1.9),
                            e -> Shockwaves.canHit(this, e))) {
                        CombatUtil.specialHurt(this, e, dmg(0.8F));
                        e.addEffect(new MobEffectInstance(ModEffects.STAGGER, 20, 0));
                    }
                    Fx.halo(level, at, MOON, PALE, 2.6F, 10);
                    Fx.sparkles(level, at.add(0, 0.5, 0), PALE, 10, 0.6);
                    Fx.sound(level, at, ModSounds.ABILITY_BLADE_FALL, 1.0F, 1.2F);
                });
            }
        }
    }

    /** At half health the moon eclipses: a burst of void light, and she moves twice as fast from then on. */
    private void eclipse(int t, LivingEntity target) {
        ServerLevel level = server();
        if (t == 0) {
            playSound(SoundEvents.WITHER_SPAWN, 1.0F, 1.6F);
            Fx.gyro(level, position(), this, SOUL, MOON, 2.0F, 1.6F, 40);
            Fx.pillar(level, position(), SOUL, PALE, 1.6F, 16, 40);
        }
        if (t == 20) {
            setEnraged();
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1, false, false));
            Fx.halo(level, position(), SOUL, MOON, 14.0F, 24);
            Fx.sunburst(level, position().add(0, 1.8, 0), SOUL, PALE, 6.0F, 16, 20);
            Fx.orbit(level, position(), this, MOON, PALE, 8, 1.6F, 1.6F, 20 * 30);
            Shockwaves.ring(level, this, position(), 8.0F, 0.9F, dmg(0.5F), 0.8, SOUL, null);
            for (LivingEntity e : MobStrikes.victims(this, 14)) {
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            }
        }
    }

    // ------------------------------------------------------------------ misc

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && tickCount % 4 == 0) {
            level.sendParticles(ModParticles.SPARKLE.get().with(enraged() ? SOUL : MOON, 0.9F), getX(), getY() + 0.4, getZ(), 1, 0.3, 0.2, 0.3, 0.01);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel level) {
            Fx.slash(level, getEyePosition().add(getLookAngle().scale(1.2)), getYRot(), 0, random.nextBoolean() ? 30 : -30, MOON, 1.3F);
            Fx.sound(level, position(), ModSounds.WEAPON_SWING, 1.0F, 1.3F);
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}

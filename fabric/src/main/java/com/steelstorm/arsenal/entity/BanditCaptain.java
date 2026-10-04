package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.entity.ai.MobStrikes;
import com.steelstorm.arsenal.entity.ai.TelegraphedAttackGoal;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Leader of the bandits, carrying the key to their vault. Rallies nearby bandits with a war cry,
 * leaps at you with a ground-shaking cleave, and spins into a whirlwind when you get close.
 */
public class BanditCaptain extends SteelstormBoss {
    public BanditCaptain(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_6);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0) // + the battleaxe it carries
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    private float damage(float multiplier) {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TelegraphedAttackGoal(this, 0.0, 16.0, 20, 360, ModSounds.ENTITY_CAPTAIN_TAUNT.get(),
                () -> getHealth() < getMaxHealth() * 0.85F, target -> warCry()));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 4.0, 12.0, 12, 120, SoundEvents.VINDICATOR_CELEBRATE,
                () -> true, this::leap));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 0.0, 3.5, 14, 150, SoundEvents.VINDICATOR_AMBIENT,
                () -> true, target -> whirlwind()));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            protected int getAttackInterval() {
                return adjustedTickDelay(26);
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, BanditDuelist.class, BanditArcher.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Rally every bandit nearby and shake the player's resolve. */
    private void warCry() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (Monster ally : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(14),
                m -> m instanceof BanditDuelist || m instanceof BanditArcher || m == this)) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            Fx.burst(level, ModParticles.GLOW.get(), 0xFF6B6B, 1.3F, ally.getBoundingBox().getCenter(), 8, 0.3, 0.05);
        }
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10), p -> !p.isCreative())) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        Fx.ring(level, position(), 0xFF6B6B, 10.0F);
        Fx.sound(level, position(), ModSounds.ABILITY_WAR_CRY, 1.6F, 0.85F);
    }

    /** Leap at the target and slam down with a shockwave. */
    private void leap(LivingEntity target) {
        Vec3 to = target.position().subtract(position());
        setDeltaMovement(to.x * 0.17, 0.8, to.z * 0.17);
        hurtMarked = true;
        playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.1F);
        ServerScheduler.schedule(15, () -> {
            if (!isAlive() || !(level() instanceof ServerLevel level)) {
                return;
            }
            Shockwaves.ring(level, this, position(), 4.0F, 0.8F, damage(1.1F), 0.6, 0xFF6B6B, null);
            Shockwaves.crater(level, position(), 1.5F, 0.8F);
            Fx.impact(level, position().add(0, 0.4, 0), 0xFF6B6B, 1.6F);
            Fx.sound(level, position(), ModSounds.ABILITY_SHOCKWAVE, 1.2F, 1.1F);
        });
    }

    /** Spin with the axe, hitting everything around three times. */
    private void whirlwind() {
        for (int i = 0; i < 3; i++) {
            final int pulse = i;
            ServerScheduler.schedule(i * 5, () -> {
                if (!isAlive() || !(level() instanceof ServerLevel level)) {
                    return;
                }
                for (LivingEntity e : MobStrikes.victims(this, 3.2)) {
                    if (e.distanceTo(this) < 3.6 && e.hurt(damageSources().mobAttack(this), damage(0.6F))) {
                        e.knockback(0.4, getX() - e.getX(), getZ() - e.getZ());
                    }
                }
                float yaw = getYRot() + pulse * 120;
                Vec3 c = position().add(0, 1.0, 0);
                Fx.slash(level, c.add(Vec3.directionFromRotation(0, yaw).scale(1.3)), yaw + 90, 0, 0, 0xFF6B6B, 1.1F);
                Fx.slash(level, c.add(Vec3.directionFromRotation(0, yaw + 180).scale(1.3)), yaw + 270, 0, 0, 0xFF6B6B, 1.1F);
                Fx.sound(level, position(), ModSounds.WEAPON_SWING_HEAVY, 1.0F, 0.9F + pulse * 0.1F);
            });
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        WeaponTier tier = random.nextFloat() < 0.3F ? WeaponTier.DIAMOND : WeaponTier.IRON;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.weapon(WeaponType.BATTLEAXE, tier).get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        setDropChance(EquipmentSlot.MAINHAND, 0.25F);
        setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, spawnType, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return data;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VINDICATOR_DEATH;
    }
}

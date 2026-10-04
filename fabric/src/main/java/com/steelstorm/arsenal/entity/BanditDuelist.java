package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.entity.ai.TelegraphedAttackGoal;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A sword-fighting bandit. Sometimes parries your hits, and telegraphs a lunging strike. */
public class BanditDuelist extends SteelstormMonster {
    private long parryReadyAt;

    public BanditDuelist(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0) // + 4-6 from the sword it carries
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TelegraphedAttackGoal(this, 2.0, 5.0, 14, 100, SoundEvents.ARMOR_EQUIP_IRON.value(),
                () -> true, this::lunge));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** Dashes at the target and strikes for heavy damage. */
    private void lunge(LivingEntity target) {
        Vec3 to = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        setDeltaMovement(to.x * 1.1, 0.2, to.z * 1.1);
        hurtMarked = true;
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F);
        if (distanceTo(target) < 4.5) {
            target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Sometimes parries a melee hit from the front.
        if (!level().isClientSide && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && !isCharging() && level().getGameTime() >= parryReadyAt && getRandom().nextFloat() < 0.25F
                && CombatUtil.isInFront(this, attacker.position())) {
            parryReadyAt = level().getGameTime() + 60;
            attacker.addEffect(new MobEffectInstance(ModEffects.STAGGER, 25, 0));
            playSound(SoundEvents.ANVIL_PLACE, 0.5F, 1.8F);
            playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.2F);
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getEyeY() - 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.4);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        WeaponType type = random.nextBoolean() ? WeaponType.LONGSWORD : WeaponType.KATANA;
        WeaponTier tier = random.nextFloat() < 0.2F ? WeaponTier.IRON : WeaponTier.STONE;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.weapon(type, tier).get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.12F);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty,
                                                                     net.minecraft.world.entity.MobSpawnType spawnType,
                                                                     net.minecraft.world.entity.SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, spawnType, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return data;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }
}

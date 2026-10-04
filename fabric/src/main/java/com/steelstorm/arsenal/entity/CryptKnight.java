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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * A long-dead champion guarding a crypt. Splits the ground with its greatsword, dashes through the
 * air in a trail of souls, and calls up the dead behind a soul shield when badly hurt.
 */
public class CryptKnight extends SteelstormBoss {
    private static final int SOUL = 0x7FA7FF;
    private boolean shielded;

    public CryptKnight(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0) // + the greatsword it carries
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    private float damage(float multiplier) {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TelegraphedAttackGoal(this, 0.0, 32.0, 24, 20 * 60, SoundEvents.EVOKER_PREPARE_SUMMON,
                () -> !shielded && getHealth() < getMaxHealth() * 0.5F, target -> soulShield()));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 2.0, 9.0, 18, 120, SoundEvents.EVOKER_PREPARE_ATTACK,
                () -> true, this::groundCleave));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 5.0, 14.0, 10, 140, SoundEvents.WITHER_SKELETON_AMBIENT,
                () -> true, this::soulDash));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            protected int getAttackInterval() {
                return adjustedTickDelay(30);
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Skeleton.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Drive the greatsword into the ground: a line of erupting stone runs at the target. */
    private void groundCleave(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        Vec3 start = position().add(dir.scale(1.2));
        Shockwaves.line(level, this, start, dir, 10, 1.0, damage(1.2F), 0.7, SOUL, null);
        Fx.slash(level, getEyePosition().add(dir.scale(1.5)), getYRot(), 0, 90, SOUL, 1.3F);
        Fx.impact(level, start.add(0, 0.3, 0), SOUL, 1.5F);
        Fx.sound(level, start, ModSounds.ABILITY_GROUND_CRACK, 1.3F, 0.8F);
    }

    /** Rush the target in a blur of souls and strike on arrival. */
    private void soulDash(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = position();
        Vec3 to = target.position().subtract(from);
        Vec3 dir = to.multiply(1, 0, 1).normalize();
        double dist = Math.max(0, to.horizontalDistance() - 1.5);
        setDeltaMovement(dir.x * Math.min(2.2, dist * 0.35), 0.25, dir.z * Math.min(2.2, dist * 0.35));
        hurtMarked = true;
        for (int i = 0; i < 6; i++) {
            Fx.burst(level, ModParticles.GLOW.get(), SOUL, 1.4F, from.add(dir.scale(i * dist / 6)).add(0, 1, 0), 3, 0.2, 0.4, 0.2, 0.02);
        }
        Fx.sound(level, from, ModSounds.ABILITY_DASH, 1.0F, 0.7F);
        ServerScheduler.schedule(8, () -> {
            if (!isAlive()) {
                return;
            }
            MobStrikes.cone(this, 3.5, 70, damage(1.3F), 0.8);
            Fx.slash(level, getEyePosition().add(getLookAngle().scale(1.5)), getYRot(), 0, 20, SOUL, 1.2F);
            Fx.sound(level, position(), ModSounds.WEAPON_SWING_HEAVY, 1.0F, 0.8F);
        });
    }

    /** Below half health, once: a barrier of souls and three skeletons answer the knight's call. */
    private void soulShield() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        shielded = true;
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        Fx.ring(level, position(), SOUL, 6.0F);
        Fx.burst(level, ModParticles.GLOW.get(), SOUL, 1.8F, getBoundingBox().getCenter(), 40, 0.8, 0.1);
        Fx.sound(level, position(), ModSounds.ENTITY_WARLORD_ROAR, 1.4F, 1.4F);
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2 / 3;
            BlockPos pos = BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3);
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton == null) {
                continue;
            }
            skeleton.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, getYRot(), 0);
            skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
            skeleton.setTarget(getTarget());
            level.addFreshEntity(skeleton);
            Fx.burst(level, ModParticles.SMOKE.get(), 0x2A2E3A, 1.6F, Vec3.atBottomCenterOf(pos).add(0, 0.6, 0), 12, 0.3, 0.4, 0.3, 0.02);
            Fx.burst(level, ModParticles.GLOW.get(), SOUL, 1.4F, Vec3.atBottomCenterOf(pos).add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (hasEffect(MobEffects.DAMAGE_RESISTANCE) && level() instanceof ServerLevel level && tickCount % 3 == 0) {
            double a = tickCount * 0.3;
            Vec3 c = position().add(Math.cos(a) * 1.0, 1.0 + Math.sin(tickCount * 0.1) * 0.5, Math.sin(a) * 1.0);
            Fx.shoot(level, ModParticles.GLOW.get(), SOUL, 1.2F, c, new Vec3(0, 0.03, 0));
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.weapon(WeaponType.GREATSWORD, WeaponTier.DIAMOND).get()));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        setDropChance(EquipmentSlot.MAINHAND, 0.08F);
        setDropChance(EquipmentSlot.HEAD, 0.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, spawnType, data);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Shielded", shielded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shielded = tag.getBoolean("Shielded");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.NETHERITE_BLOCK_STEP, 0.4F, 0.7F);
    }
}

package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.entity.ai.MobStrikes;
import com.steelstorm.arsenal.entity.ai.TelegraphedAttackGoal;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * The Storm Herald, summoned at a Storm Altar. It floats above the battlefield calling down
 * lightning on marked spots and hurling storm crescents. Below half health the storm breaks:
 * it dives into the ground in thunderous slams, and is left stunned and open afterwards.
 */
public class StormHerald extends SteelstormBoss {
    private static final int STORM = 0x7FD8FF;
    private int phase = 1;
    private int stunned;
    @Nullable
    private BlockPos home;
    private float orbit;
    /** Counts through its flight cycle: aloft (casting) for a while, then a low swoop you can hit. */
    private int flightTimer;
    private boolean wasSwooping;

    public StormHerald(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
        xpReward = 300;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public void setHome(BlockPos pos) {
        this.home = pos;
    }

    public boolean isStunned() {
        return stunned > 0;
    }

    /** Low and close for a few seconds, slashing at whoever it circles: the window for melee fighters. */
    public boolean isSwooping() {
        int cycle = phase >= 2 ? 260 : 300;
        return flightTimer % cycle > cycle - 110;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new TelegraphedAttackGoal(this, 0.0, 26.0, 16, 80, ModSounds.ENTITY_HERALD_CAST.get(),
                () -> !isStunned() && !isSwooping(), target -> lightningCall(target)));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 3.0, 22.0, 12, 110, SoundEvents.EVOKER_PREPARE_ATTACK,
                () -> !isStunned() && !isSwooping(), this::stormCrescents));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 0.0, 18.0, 22, 220, SoundEvents.EVOKER_PREPARE_SUMMON,
                () -> phase >= 2 && !isStunned() && !isSwooping(), this::thunderDive));
        goalSelector.addGoal(4, new HoverGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ------------------------------------------------------------------ attacks

    /** Marks spots around the target with glowing rings; lightning strikes them a second later. */
    private void lightningCall(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int count = phase >= 2 ? 5 : 3;
        List<Vec3> spots = new ArrayList<>();
        spots.add(target.position());
        for (int i = 1; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 1.5 + random.nextDouble() * 3.5;
            Vec3 p = target.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            BlockPos g = Shockwaves.ground(level, p.x, p.y + 1, p.z);
            spots.add(g != null ? new Vec3(p.x, g.getY() + 1, p.z) : p);
        }
        for (Vec3 spot : spots) {
            Fx.ring(level, spot, STORM, 1.8F);
            Fx.burst(level, ModParticles.RUNE.get(), STORM, 1.2F, spot.add(0, 0.1, 0), 6, 0.8, 0.02, 0.8, 0.0);
        }
        for (int i = 0; i < spots.size(); i++) {
            Vec3 spot = spots.get(i);
            ServerScheduler.schedule(20 + i * 2, () -> {
                if (!isAlive()) {
                    return;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(spot.x, spot.y, spot.z);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                for (LivingEntity e : MobStrikes.victims(this, 64)) {
                    if (e.position().distanceToSqr(spot) < 2.2 * 2.2 && e.hurt(damageSources().indirectMagic(this, this), 9.0F)) {
                        e.addEffect(new MobEffectInstance(ModEffects.STAGGER, 20, 0));
                    }
                }
                Fx.sparks(level, Fx.LIGHTNING, spot.add(0, 0.3, 0), 14, 0.7);
                Fx.sound(level, spot, ModSounds.ABILITY_THUNDER, 1.6F, 0.9F + random.nextFloat() * 0.2F);
            });
        }
    }

    /** Three crescents of storm-light, fanned out at the target. */
    private void stormCrescents(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from).normalize();
        float baseYaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) -Math.toDegrees(Math.asin(aim.y));
        int count = phase >= 2 ? 5 : 3;
        for (int i = 0; i < count; i++) {
            float yaw = baseYaw + (i - (count - 1) / 2.0F) * 14;
            Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
            SlashWaveEntity.fire(this, from.add(dir.scale(1.0)), dir, 0.95F, 22, 1.6F, 90, 7.0F, STORM, false, null);
        }
        Fx.sound(level, from, ModSounds.ABILITY_SLASH_WAVE, 1.4F, 1.2F);
        Fx.sound(level, from, ModSounds.ABILITY_ZAP, 1.0F, 1.0F);
    }

    /** Dive onto the target and slam the ground with a thunder ring. Leaves the Herald stunned. */
    private void thunderDive(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 to = target.position().subtract(position());
        setDeltaMovement(to.scale(0.22).add(0, -0.4, 0));
        hurtMarked = true;
        setNoGravity(false);
        Fx.sound(level, position(), ModSounds.ABILITY_DASH, 1.4F, 0.6F);
        ServerScheduler.schedule(12, () -> {
            if (!isAlive()) {
                return;
            }
            Vec3 at = position();
            Shockwaves.ring(level, this, at, 8.0F, 0.9F, 8.0F, 0.8, STORM, e -> e.addEffect(new MobEffectInstance(ModEffects.STAGGER, 30, 0)));
            Shockwaves.crater(level, at, 2.5F, 1.2F);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(at.x + Math.cos(a) * 4, at.y, at.z + Math.sin(a) * 4);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
            Fx.impact(level, at.add(0, 0.5, 0), STORM, 3.0F);
            Fx.sound(level, at, ModSounds.ABILITY_SHOCKWAVE, 1.6F, 0.8F);
            Fx.sound(level, at, ModSounds.ABILITY_THUNDER, 1.6F, 0.7F);
            for (ServerPlayer p : level.players()) {
                double d = p.position().distanceTo(at);
                if (d < 24) {
                    Stamina.shake(p, (float) (1.2 * (1 - d / 28)), 14);
                }
            }
            // Stunned on the ground: the moment to strike back.
            stunned = 60;
        });
    }

    // ------------------------------------------------------------------ state

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() != null) {
            flightTimer++;
        }
        boolean swooping = isSwooping() && getTarget() != null;
        if (swooping != wasSwooping && level() instanceof ServerLevel level) {
            wasSwooping = swooping;
            Fx.sound(level, position(), swooping ? ModSounds.ABILITY_DASH : ModSounds.ENTITY_HERALD_CAST, 1.4F, swooping ? 0.7F : 1.2F);
            Fx.burst(level, ModParticles.GLOW.get(), STORM, 1.6F, getBoundingBox().getCenter(), 16, 0.5, 0.8, 0.5, 0.05);
        }
        if (phase == 1 && getHealth() < getMaxHealth() * 0.5F) {
            phase = 2;
            bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            bossEvent.setDarkenScreen(true);
            for (ServerPlayer p : bossEvent.getPlayers()) {
                p.displayClientMessage(Component.translatable("message.steelstorm.herald_phase2").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            }
            playSound(ModSounds.ENTITY_HERALD_CAST.get(), 2.5F, 0.6F);
            if (level() instanceof ServerLevel level) {
                Fx.ring(level, position(), STORM, 12.0F);
                level.setWeatherParameters(0, 20 * 60 * 3, true, true);
            }
        }
        if (stunned > 0) {
            stunned--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1, 0.2));
            if (level() instanceof ServerLevel level && stunned % 4 == 0) {
                Fx.burst(level, ModParticles.SPARK.get(), STORM, 1.0F, position().add(0, getBbHeight() + 0.2, 0), 3, 0.3, 0.1);
            }
            if (stunned == 0) {
                setNoGravity(true);
                setDeltaMovement(0, 0.5, 0);
            }
        }
        if (level() instanceof ServerLevel level && tickCount % 3 == 0) {
            Fx.burst(level, ModParticles.GLOW.get(), STORM, 1.1F, getBoundingBox().getCenter(), 2, 0.5, 0.8, 0.5, 0.01);
            if (phase >= 2 && tickCount % 100 == 0) {
                staticDischarge(level);
            }
        }
    }

    /** Phase two: every few seconds an arc of lightning leaps to a nearby player. */
    private void staticDischarge(ServerLevel level) {
        List<LivingEntity> victims = MobStrikes.victims(this, 12);
        if (victims.isEmpty()) {
            return;
        }
        LivingEntity v = victims.get(random.nextInt(victims.size()));
        Vec3 a = getBoundingBox().getCenter();
        Vec3 b = v.getBoundingBox().getCenter();
        for (int i = 0; i <= 12; i++) {
            Vec3 p = a.lerp(b, i / 12.0).add(random.nextGaussian() * 0.15, random.nextGaussian() * 0.15, random.nextGaussian() * 0.15);
            Fx.burst(level, ModParticles.SPARK.get(), Fx.LIGHTNING, 1.2F, p, 1, 0.02, 0.0);
        }
        v.hurt(damageSources().indirectMagic(this, this), 3.0F);
        Fx.sound(level, b, ModSounds.ABILITY_ZAP, 1.0F, 1.2F);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_LIGHTNING) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Stunned on the ground it takes extra damage.
        return super.hurt(source, isStunned() ? amount * 1.4F : amount);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.legendary(LegendaryWeaponItem.Legendary.TEMPEST_EDGE).get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
        tag.putInt("Phase", phase);
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = Math.max(1, tag.getInt("Phase"));
        home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null;
        setNoGravity(true);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    /**
     * Floats a few blocks above the ground, circling its target at a distance, low enough to be
     * reached with a jump and a long weapon. Without a target it drifts around its altar.
     */
    private class HoverGoal extends Goal {
        HoverGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !isStunned() && isNoGravity();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            Vec3 anchor;
            double radius;
            boolean swoop = isSwooping() && target != null;
            if (target != null && target.isAlive()) {
                anchor = target.position();
                radius = swoop ? 2.2 : (phase >= 2 ? 5.0 : 6.0);
                getLookControl().setLookAt(target, 30, 30);
                if (swoop && tickCount % 20 == 0 && distanceToSqr(target) < 3.5 * 3.5) {
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (target.hurt(damageSources().mobAttack(StormHerald.this), 6.0F) && level() instanceof ServerLevel level) {
                        Fx.slash(level, target.getBoundingBox().getCenter(), getYRot(), 0, 30, STORM, 1.0F);
                        Fx.sparks(level, Fx.LIGHTNING, target.getBoundingBox().getCenter(), 6, 0.5);
                        Fx.sound(level, target.position(), ModSounds.WEAPON_HIT_METAL, 1.0F, 1.3F);
                    }
                }
            } else {
                anchor = home != null ? Vec3.atBottomCenterOf(home) : position();
                radius = 4.0;
            }
            orbit += phase >= 2 ? 0.035F : 0.022F;
            Vec3 desired = anchor.add(Math.cos(orbit) * radius, 0, Math.sin(orbit) * radius);
            BlockPos g = Shockwaves.ground((ServerLevel) level(), desired.x, anchor.y + 2, desired.z);
            double groundY = g != null ? g.getY() + 1 : anchor.y;
            double height = isSwooping() && target != null ? 0.4 : 2.2;
            desired = new Vec3(desired.x, groundY + height + Math.sin(tickCount * 0.08) * 0.3, desired.z);
            Vec3 to = desired.subtract(position());
            Vec3 v = to.length() > 0.1 ? to.normalize().scale(Math.min(0.32, to.length() * 0.12)) : Vec3.ZERO;
            setDeltaMovement(getDeltaMovement().scale(0.75).add(v.scale(0.35)));
        }
    }
}

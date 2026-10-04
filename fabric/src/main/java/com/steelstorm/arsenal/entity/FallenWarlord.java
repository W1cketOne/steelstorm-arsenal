package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.entity.ai.MobStrikes;
import com.steelstorm.arsenal.entity.ai.TelegraphedAttackGoal;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
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
 * The Fallen Warlord. Phase 1: greatsword swings and a telegraphed cleave. Phase 2 (below 2/3
 * health): also summons Iron Revenants. Phase 3 (below 1/3): enraged, faster, and leaps at you.
 */
public class FallenWarlord extends SteelstormMonster {
    private static final net.minecraft.resources.ResourceLocation ENRAGE_SPEED = SteelstormArsenal.id("warlord_enrage_speed");
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.steelstorm.fallen_warlord"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int phase = 1;

    public FallenWarlord(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 250;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0) // + 10 from the netherite greatsword it carries
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public int phase() {
        return phase;
    }

    /** Keeps the boss inside its colosseum when it has no target. */
    public void setArenaCenter(BlockPos center) {
        restrictTo(center, 9);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Phase 2+: raise the dead.
        goalSelector.addGoal(1, new TelegraphedAttackGoal(this, 0.0, 24.0, 30, 400, SoundEvents.EVOKER_PREPARE_SUMMON,
                () -> phase >= 2 && nearbyRevenants() < 4, target -> summonRevenants()));
        // Phase 3: leap at the target and slam down.
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 4.0, 14.0, 14, 120, SoundEvents.RAVAGER_ROAR,
                () -> phase >= 3, this::leapSlam));
        // Every phase: a wide greatsword cleave, faster when enraged.
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 0.0, 4.5, 20, 70, SoundEvents.EVOKER_PREPARE_ATTACK,
                () -> phase < 3, target -> cleave()));
        goalSelector.addGoal(2, new TelegraphedAttackGoal(this, 0.0, 4.5, 11, 45, SoundEvents.EVOKER_PREPARE_ATTACK,
                () -> phase >= 3, target -> cleave()));
        // A heavy greatsword: slower swings than a zombie, faster once enraged.
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            protected int getAttackInterval() {
                return adjustedTickDelay(phase >= 3 ? 24 : 34);
            }
        });
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, IronRevenant.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private void cleave() {
        MobStrikes.cone(this, 5.0, 60, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35F, 1.2);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.5F);
        playSound(SoundEvents.ANVIL_LAND, 0.5F, 0.5F);
    }

    private void leapSlam(LivingEntity target) {
        Vec3 to = target.position().subtract(position());
        setDeltaMovement(to.x * 0.18, 0.75, to.z * 0.18);
        hurtMarked = true;
        playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.7F);
        ServerScheduler.schedule(16, () -> {
            if (isAlive()) {
                MobStrikes.slam(this, 4.0, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F, 0.7);
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.6F);
            }
        });
    }

    private int nearbyRevenants() {
        return level().getEntitiesOfClass(IronRevenant.class, getBoundingBox().inflate(24)).size();
    }

    private void summonRevenants() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        int count = 2 + (phase >= 3 ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            BlockPos pos = BlockPos.containing(getX() + Math.cos(angle) * 4, getY(), getZ() + Math.sin(angle) * 4);
            IronRevenant revenant = ModEntities.IRON_REVENANT.get().create(server);
            if (revenant == null) {
                continue;
            }
            revenant.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, getYRot(), 0);
            revenant.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
            revenant.setTarget(getTarget());
            server.addFreshEntity(revenant);
            server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.6, 0.3, 0.05);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.02);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.6F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        float fraction = getHealth() / getMaxHealth();
        int newPhase = fraction > 2.0F / 3.0F ? 1 : (fraction > 1.0F / 3.0F ? 2 : 3);
        if (newPhase > phase) {
            enterPhase(newPhase);
        }
        bossEvent.setProgress(fraction);
        if (tickCount % 10 == 0 && level() instanceof ServerLevel server) {
            // Only players close by (or being fought) see the bar.
            for (ServerPlayer player : server.players()) {
                double d = player.distanceToSqr(this);
                boolean shown = bossEvent.getPlayers().contains(player);
                if (!shown && (d < 40 * 40 || getTarget() == player)) {
                    bossEvent.addPlayer(player);
                } else if (shown && d > 56 * 56 && getTarget() != player) {
                    bossEvent.removePlayer(player);
                }
            }
        }
        if (phase >= 3 && level() instanceof ServerLevel server && tickCount % 4 == 0) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(1.0), getZ(), 2, 0.4, 0.3, 0.4, 0.01);
        }
    }

    private void enterPhase(int newPhase) {
        phase = newPhase;
        String key = newPhase == 2 ? "message.steelstorm.warlord_phase2" : "message.steelstorm.warlord_phase3";
        for (ServerPlayer player : bossEvent.getPlayers()) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
        if (newPhase >= 2) {
            bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        }
        if (newPhase >= 3) {
            bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(ENRAGE_SPEED)) {
                speed.addPermanentModifier(new AttributeModifier(ENRAGE_SPEED, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
        } else {
            playSound(SoundEvents.WITHER_AMBIENT, 1.5F, 0.6F);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        // The bar appears by distance in customServerAiStep; display-only (NoAI) warlords never show it.
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.weapon(WeaponType.GREATSWORD, WeaponTier.NETHERITE).get()));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
        tag.putInt("Phase", phase);
        if (hasRestriction()) {
            tag.putLong("Arena", getRestrictCenter().asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = Math.max(1, tag.getInt("Phase"));
        if (tag.contains("Arena")) {
            restrictTo(BlockPos.of(tag.getLong("Arena")), 9);
        }
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
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
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }
}

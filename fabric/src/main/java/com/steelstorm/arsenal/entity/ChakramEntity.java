package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown chakram. Flies straight, then bounces between up to three enemies, then flies back to
 * its thrower (passing through blocks on the way back) and returns to their inventory.
 *
 * <p>Abilities also throw spectral chakrams: glowing copies that vanish when they come back. A
 * sawblade chakram stops where it hits and grinds everything around it for a while first.</p>
 */
public class ChakramEntity extends Projectile implements ThrownWeaponEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ChakramEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_SPECTRAL = SynchedEntityData.defineId(ChakramEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_BOUNCES = 3;
    private static final double SPEED = 1.3;
    public static final int SPECTRAL_COLOR = 0x67E8F9;

    private enum State { OUTBOUND, HOMING, GRINDING, RETURNING }

    private State state = State.OUTBOUND;
    private final Set<Integer> hitIds = new HashSet<>();
    @Nullable
    private LivingEntity homingTarget;
    private int age;
    private int stateAge;
    private float damage = 6.0F;
    private int grindTicks;

    public ChakramEntity(EntityType<? extends ChakramEntity> type, Level level) {
        super(type, level);
    }

    public ChakramEntity(Level level, LivingEntity owner, ItemStack stack) {
        this(ModEntities.CHAKRAM.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        entityData.set(DATA_ITEM, stack.copy());
        Vec3 look = owner.getLookAngle();
        setDeltaMovement(look.scale(SPEED));
    }

    /**
     * A glowing copy thrown by an ability. With `grindTicks` above zero it becomes a sawblade
     * that hovers where it lands, cutting everything nearby, before it comes back.
     */
    public static ChakramEntity spectral(LivingEntity owner, Vec3 dir, float damage, int grindTicks) {
        ChakramEntity c = new ChakramEntity(ModEntities.CHAKRAM.get(), owner.level());
        c.setOwner(owner);
        c.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        c.entityData.set(DATA_SPECTRAL, true);
        c.setDeltaMovement(dir.normalize().scale(SPEED));
        c.damage = damage;
        c.grindTicks = grindTicks;
        owner.level().addFreshEntity(c);
        return c;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_SPECTRAL, false);
    }

    @Override
    public ItemStack getRenderStack() {
        ItemStack stack = entityData.get(DATA_ITEM);
        return stack.isEmpty() ? new ItemStack(ModItems.CHAKRAM.get()) : stack;
    }

    public boolean isSpectral() {
        return entityData.get(DATA_SPECTRAL);
    }

    @Override
    public boolean spins() {
        return true;
    }

    @Override
    public boolean glows() {
        return isSpectral();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        stateAge++;
        if (!level().isClientSide) {
            serverTick();
            if (isRemoved()) {
                return;
            }
        }
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (level().isClientSide) {
            if (isSpectral()) {
                level().addParticle(ModParticles.GLOW.get().with(SPECTRAL_COLOR, 1.2F), getX(), getY(), getZ(), 0, 0, 0);
            } else if (age % 2 == 0) {
                level().addParticle(ModParticles.SPARK.get().with(0xDCE6F0, 0.8F), getX(), getY(), getZ(), 0, 0, 0);
            }
        }
    }

    private void serverTick() {
        Entity owner = getOwner();
        if (age > 240 || (state == State.RETURNING && (owner == null || !owner.isAlive() || owner.level() != level()))) {
            dropAndDiscard();
            return;
        }
        switch (state) {
            case OUTBOUND -> {
                HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
                if (hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() instanceof LivingEntity target) {
                    if (grindTicks > 0) {
                        startGrinding();
                    } else {
                        strike(target);
                    }
                } else if (hit.getType() == HitResult.Type.BLOCK || stateAge > 14) {
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.3F, 2.0F);
                    }
                    if (grindTicks > 0) {
                        startGrinding();
                    } else {
                        startReturning();
                    }
                }
            }
            case HOMING -> {
                if (homingTarget == null || !homingTarget.isAlive() || stateAge > 30) {
                    startReturning();
                    return;
                }
                Vec3 to = homingTarget.getBoundingBox().getCenter().subtract(position());
                if (to.length() < 1.0) {
                    strike(homingTarget);
                } else {
                    setDeltaMovement(to.normalize().scale(SPEED));
                }
            }
            case GRINDING -> grind();
            case RETURNING -> {
                Vec3 to = owner.getEyePosition().subtract(0, 0.4, 0).subtract(position());
                if (to.length() < 1.4) {
                    catchBy(owner);
                } else {
                    setDeltaMovement(to.normalize().scale(Math.min(SPEED * 1.2, to.length())));
                }
            }
        }
    }

    private void startGrinding() {
        state = State.GRINDING;
        stateAge = 0;
        setDeltaMovement(Vec3.ZERO);
        Fx.sound(level(), position(), ModSounds.WEAPON_CHAKRAM_SPIN, 1.0F, 0.8F);
    }

    /** Sawblade: hover in place, drift toward the nearest enemy and cut everything within reach. */
    private void grind() {
        if (!(getOwner() instanceof LivingEntity owner) || stateAge > grindTicks) {
            startReturning();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity nearest = null;
        double best = 4.0 * 4.0;
        for (LivingEntity e : CombatUtil.around(owner, position(), 4.0)) {
            double d = e.getBoundingBox().getCenter().distanceToSqr(position());
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        if (nearest != null) {
            Vec3 to = nearest.getBoundingBox().getCenter().subtract(position());
            setDeltaMovement(to.length() > 0.5 ? to.normalize().scale(0.18) : Vec3.ZERO);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.7));
        }
        if (stateAge % 5 == 0) {
            for (LivingEntity e : CombatUtil.around(owner, position(), 1.9)) {
                if (CombatUtil.specialHurt(owner, e, damage)) {
                    Vec3 at = e.getBoundingBox().getCenter();
                    Fx.sparks(level, SPECTRAL_COLOR, at, 5, 0.45);
                    Fx.burst(level, ModParticles.BLOOD.get(), 0xFFFFFF, 1.0F, at, 3, 0.2, 0.08);
                }
            }
            Fx.sound(level, position(), ModSounds.WEAPON_CHAKRAM_SPIN, 0.8F, 1.5F);
        }
        Fx.sparks(level, 0xFFE9A8, position().add(0, -0.1, 0), 2, 0.35);
    }

    private void strike(LivingEntity target) {
        Entity owner = getOwner();
        hitIds.add(target.getId());
        target.invulnerableTime = 0;
        boolean hurt;
        if (isSpectral() && owner instanceof LivingEntity livingOwner) {
            hurt = CombatUtil.specialHurt(livingOwner, target, damage);
        } else {
            hurt = target.hurt(damageSources().thrown(this, owner == null ? this : owner), damage);
        }
        if (level() instanceof ServerLevel level) {
            Vec3 at = target.getBoundingBox().getCenter();
            Fx.slash(level, at, getYRot() + level.random.nextFloat() * 60 - 30, 0, level.random.nextFloat() * 40 - 20,
                    isSpectral() ? SPECTRAL_COLOR : 0xDCE6F0, 0.6F);
            if (hurt) {
                Fx.sparks(level, 0xFFFFFF, at, 4, 0.4);
            }
            Fx.sound(level, at, ModSounds.WEAPON_HIT_METAL, 0.7F, 1.6F);
            if (!isSpectral()) {
                getRenderStack().hurtAndBreak(1, level, owner instanceof net.minecraft.server.level.ServerPlayer l ? l : null, item -> {
                });
            }
        }
        if (hitIds.size() >= MAX_BOUNCES) {
            startReturning();
            return;
        }
        LivingEntity next = null;
        double best = 8.0 * 8.0;
        if (owner instanceof LivingEntity livingOwner) {
            for (LivingEntity candidate : CombatUtil.around(livingOwner, target.position(), 8.0)) {
                double d = candidate.distanceToSqr(target);
                if (!hitIds.contains(candidate.getId()) && d < best) {
                    best = d;
                    next = candidate;
                }
            }
        }
        if (next == null) {
            startReturning();
        } else {
            homingTarget = next;
            state = State.HOMING;
            stateAge = 0;
        }
    }

    private void startReturning() {
        state = State.RETURNING;
        stateAge = 0;
        homingTarget = null;
        noPhysics = true;
    }

    private void catchBy(Entity owner) {
        ItemStack stack = entityData.get(DATA_ITEM);
        if (owner instanceof Player player && !stack.isEmpty() && !isSpectral()) {
            if (!player.getAbilities().instabuild && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        if (isSpectral() && level() instanceof ServerLevel level) {
            Fx.burst(level, ModParticles.GLOW.get(), SPECTRAL_COLOR, 1.2F, position(), 8, 0.2, 0.05);
        }
        discard();
    }

    private void dropAndDiscard() {
        ItemStack stack = entityData.get(DATA_ITEM);
        if (!isSpectral() && !stack.isEmpty() && !(getOwner() instanceof Player player && player.getAbilities().instabuild)) {
            spawnAtLocation(stack, 0.1F);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !hitIds.contains(target.getId())
                && (!(getOwner() instanceof LivingEntity owner) || CombatUtil.isEnemyOf(owner, target));
    }

    @Override
    public boolean shouldBeSaved() {
        return !isSpectral();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ItemStack stack = entityData.get(DATA_ITEM);
        if (!stack.isEmpty()) {
            tag.put("Item", stack.save(registryAccess()));
        }
        tag.putInt("Age", age);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ITEM, ItemStack.parseOptional(registryAccess(), tag.getCompound("Item")));
        age = tag.getInt("Age");
        startReturning();
    }
}

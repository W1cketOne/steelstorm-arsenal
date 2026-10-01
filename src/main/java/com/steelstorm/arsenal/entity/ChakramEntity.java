package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
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
 */
public class ChakramEntity extends Projectile implements ThrownWeaponEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ChakramEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final int MAX_BOUNCES = 3;
    private static final double SPEED = 1.3;

    private enum State { OUTBOUND, HOMING, RETURNING }

    private State state = State.OUTBOUND;
    private final Set<Integer> hitIds = new HashSet<>();
    @Nullable
    private LivingEntity homingTarget;
    private int age;
    private int stateAge;

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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getRenderStack() {
        ItemStack stack = entityData.get(DATA_ITEM);
        return stack.isEmpty() ? new ItemStack(ModItems.CHAKRAM.get()) : stack;
    }

    @Override
    public boolean spins() {
        return true;
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
        }
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (level().isClientSide && age % 2 == 0) {
            level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
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
                    strike(target);
                } else if (hit.getType() == HitResult.Type.BLOCK || stateAge > 14) {
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.3F, 2.0F);
                    }
                    startReturning();
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

    private void strike(LivingEntity target) {
        Entity owner = getOwner();
        hitIds.add(target.getId());
        target.invulnerableTime = 0;
        target.hurt(damageSources().thrown(this, owner == null ? this : owner), 6.0F);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        getRenderStack().hurtAndBreak(1, (ServerLevel) level(), owner instanceof LivingEntity l ? l : null, item -> {
        });
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
        if (owner instanceof Player player && !stack.isEmpty()) {
            if (!player.getAbilities().instabuild && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        discard();
    }

    private void dropAndDiscard() {
        ItemStack stack = entityData.get(DATA_ITEM);
        if (!stack.isEmpty() && !(getOwner() instanceof Player player && player.getAbilities().instabuild)) {
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

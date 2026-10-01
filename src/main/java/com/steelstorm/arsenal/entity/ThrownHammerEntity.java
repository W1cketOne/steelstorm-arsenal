package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The warhammer itself, hurled end over end. It smashes into the first enemy or wall it meets
 * with a small shockwave, then flies back into the thrower's hand, hitting anything on the way.
 * The item really leaves the inventory while it flies and goes back into the same slot.
 */
public class ThrownHammerEntity extends Projectile implements ThrownWeaponEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ThrownHammerEntity.class,
            EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(ThrownHammerEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final double SPEED = 1.35;

    private int slot = -1;
    private float impactDamage;
    private float returnDamage;
    private int maxOutTicks = 11;
    private int age;
    private final Set<Integer> returnHits = new HashSet<>();

    public ThrownHammerEntity(EntityType<? extends ThrownHammerEntity> type, Level level) {
        super(type, level);
    }

    public ThrownHammerEntity(ServerPlayer owner, ItemStack stack, int slot, float impactDamage, float returnDamage, double range) {
        this(ModEntities.THROWN_HAMMER.get(), owner.level());
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.3, owner.getZ());
        entityData.set(DATA_ITEM, stack);
        this.slot = slot;
        this.impactDamage = impactDamage;
        this.returnDamage = returnDamage;
        this.maxOutTicks = (int) Math.ceil(range / SPEED);
        setDeltaMovement(owner.getLookAngle().scale(SPEED));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_RETURNING, false);
    }

    @Override
    public ItemStack getRenderStack() {
        ItemStack stack = entityData.get(DATA_ITEM);
        return stack.isEmpty() ? new ItemStack(ModItems.weapon(WeaponType.WARHAMMER, WeaponTier.IRON).get()) : stack;
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (!level().isClientSide) {
            serverTick();
            if (isRemoved()) {
                return;
            }
        } else {
            Vec3 c = position();
            level().addParticle(ModParticles.GLOW.get().with(0xFF9A3C, 1.6F), c.x, c.y, c.z, 0, 0.02, 0);
            if (age % 2 == 0) {
                level().addParticle(ModParticles.SPARK.get().with(0xFFD166, 1.0F), c.x, c.y, c.z,
                        (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
            }
        }
        Vec3 m = getDeltaMovement();
        setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
    }

    private void serverTick() {
        ServerLevel level = (ServerLevel) level();
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer player) || !player.isAlive() || player.level() != level || age > 200) {
            dropAndDiscard();
            return;
        }
        if (age % 6 == 1) {
            Fx.sound(level, position(), ModSounds.WEAPON_CHAKRAM_SPIN, 0.7F, 0.6F);
        }
        if (!isReturning()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() instanceof LivingEntity target) {
                smash(player, hit.getLocation(), target);
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                smash(player, hit.getLocation(), null);
            } else if (age >= maxOutTicks) {
                startReturning();
            }
            return;
        }
        Vec3 to = player.getEyePosition().subtract(0, 0.5, 0).subtract(position());
        double dist = to.length();
        if (dist < 1.5) {
            catchBy(player);
            return;
        }
        setDeltaMovement(to.normalize().scale(Math.min(SPEED * 1.4 + age * 0.01, dist)));
        AABB box = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.6);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> CombatUtil.isEnemyOf(player, e))) {
            if (returnHits.add(e.getId()) && CombatUtil.specialHurt(player, e, returnDamage)) {
                Fx.impact(level, e.getBoundingBox().getCenter(), 0xFF9A3C, 0.9F);
                Fx.sound(level, e.position(), ModSounds.WEAPON_HIT_METAL, 0.8F, 0.8F);
            }
        }
    }

    private void smash(ServerPlayer player, Vec3 at, LivingEntity target) {
        ServerLevel level = (ServerLevel) level();
        if (target != null && CombatUtil.specialHurt(player, target, impactDamage)) {
            target.addEffect(new MobEffectInstance(ModEffects.STAGGER, 50, 0));
            target.addEffect(new MobEffectInstance(ModEffects.ARMOR_BREAK, 100, 0));
            Vec3 push = getDeltaMovement().normalize();
            target.knockback(0.8, -push.x, -push.z);
        }
        Fx.impact(level, at, 0xFF9A3C, 2.0F);
        Fx.sparks(level, 0xFFD166, at, 16, 0.7);
        Fx.burst(level, ModParticles.SMOKE.get(), 0x6B6158, 1.6F, at, 10, 0.5, 0.06);
        Fx.sound(level, at, ModSounds.ABILITY_SHOCKWAVE, 1.1F, 1.1F);
        Fx.sound(level, at, ModSounds.WEAPON_HIT_METAL, 1.0F, 0.6F);
        BlockPos ground = GroundWaveEntity.ground(level, at.x, at.y, at.z);
        if (ground != null && at.y - (ground.getY() + 1) < 2.5) {
            Shockwaves.ring(level, player, new Vec3(at.x, ground.getY() + 1, at.z), 3.5F, 0.7F, impactDamage * 0.4F, 0.5,
                    0xFF9A3C, null);
        }
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(at);
            if (d < 14) {
                com.steelstorm.arsenal.combat.Stamina.shake(p, (float) (0.8 * (1 - d / 16)), 8);
            }
        }
        startReturning();
    }

    private void startReturning() {
        entityData.set(DATA_RETURNING, true);
        noPhysics = true;
        setDeltaMovement(getDeltaMovement().scale(-0.3));
    }

    private void catchBy(ServerPlayer player) {
        ItemStack stack = entityData.get(DATA_ITEM);
        if (!stack.isEmpty()) {
            boolean placed = false;
            if (slot >= 0 && slot < player.getInventory().items.size() && player.getInventory().items.get(slot).isEmpty()) {
                player.getInventory().items.set(slot, stack);
                placed = true;
            }
            if (!placed && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.inventoryMenu.broadcastChanges();
        }
        Fx.sound(level(), player.position(), ModSounds.WEAPON_HAMMER_CATCH, 1.0F, 1.0F);
        Fx.burst((ServerLevel) level(), ModParticles.GLOW.get(), 0xFF9A3C, 1.4F, player.getBoundingBox().getCenter(), 10, 0.4, 0.05);
        entityData.set(DATA_ITEM, ItemStack.EMPTY);
        discard();
    }

    private void dropAndDiscard() {
        ItemStack stack = entityData.get(DATA_ITEM);
        if (!stack.isEmpty()) {
            spawnAtLocation(stack, 0.1F);
            entityData.set(DATA_ITEM, ItemStack.EMPTY);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && (!(getOwner() instanceof LivingEntity owner) || CombatUtil.isEnemyOf(owner, target));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ItemStack stack = entityData.get(DATA_ITEM);
        if (!stack.isEmpty()) {
            tag.put("Item", stack.save(registryAccess()));
        }
        tag.putInt("Slot", slot);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ITEM, ItemStack.parseOptional(registryAccess(), tag.getCompound("Item")));
        slot = tag.getInt("Slot");
        // After a reload the throw is over: fly straight back (or drop if the owner is gone).
        startReturning();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}

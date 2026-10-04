package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A thrown spear: hits once, sticks in the ground, and can be picked back up. */
public class ThrownSpear extends AbstractArrow implements ThrownWeaponEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.ITEM_STACK);
    private boolean dealtDamage;

    public ThrownSpear(EntityType<? extends ThrownSpear> type, Level level) {
        super(type, level);
    }

    public ThrownSpear(Level level, LivingEntity owner, ItemStack spear) {
        super(ModEntities.THROWN_SPEAR.get(), owner, level, spear, null);
        entityData.set(DATA_ITEM, spear.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getRenderStack() {
        ItemStack stack = entityData.get(DATA_ITEM);
        return stack.isEmpty() ? getDefaultPickupItem() : stack;
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) {
            dealtDamage = true;
        }
        super.tick();
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        float damage = getPickupItemStackOrigin().getItem() instanceof WeaponItem weapon ? weapon.attackDamage() * 1.25F : 7.0F;
        DamageSource source = damageSources().trident(this, owner == null ? this : owner);
        dealtDamage = true;
        if (target.hurt(source, damage) && target instanceof LivingEntity living) {
            doKnockback(living, source);
            doPostHurtEffects(living);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1.0F, 0.9F);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.weapon(WeaponType.SPEAR, WeaponTier.IRON).get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected float getWaterInertia() {
        return 0.9F;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dealtDamage = tag.getBoolean("DealtDamage");
        entityData.set(DATA_ITEM, getPickupItemStackOrigin().copy());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", dealtDamage);
    }
}

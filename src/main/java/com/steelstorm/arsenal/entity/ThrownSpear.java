package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A thrown spear. Keeps the exact item stack (durability, enchantments, kill count) and can be picked back up. */
public class ThrownSpear extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.ITEM_STACK);

    private float impactDamage = 6.0f;
    private boolean dealtDamage;

    public ThrownSpear(EntityType<? extends ThrownSpear> type, Level level) {
        super(type, level);
    }

    public ThrownSpear(Level level, LivingEntity owner, ItemStack stack, float impactDamage) {
        super(ModEntities.THROWN_SPEAR.get(), owner, level, stack, null);
        this.impactDamage = impactDamage;
        this.entityData.set(DATA_ITEM, getPickupItemStackOrigin().copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    /** The stack shown by the renderer. */
    public ItemStack getDisplayStack() {
        ItemStack stack = this.entityData.get(DATA_ITEM);
        return stack.isEmpty() ? getDefaultPickupItem() : stack;
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }
        super.tick();
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return this.dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        float damage = impactDamage;
        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), target, source, damage);
        }
        this.dealtDamage = true;
        if (target.hurt(source, damage)) {
            if (this.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, target, source, this.getWeaponItem());
            }
            if (target instanceof LivingEntity living) {
                this.doKnockback(living, source);
                this.doPostHurtEffects(living);
                if (!living.isAlive()) {
                    ItemStack stack = getPickupItemStackOrigin();
                    stack.set(ModDataComponents.KILL_COUNT.get(), stack.getOrDefault(ModDataComponents.KILL_COUNT.get(), 0) + 1);
                }
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0f, 0.9f);
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
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
    public void playerTouch(Player player) {
        if (this.ownedBy(player) || this.getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    protected float getWaterInertia() {
        return 0.9f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", this.dealtDamage);
        tag.putFloat("ImpactDamage", this.impactDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dealtDamage = tag.getBoolean("DealtDamage");
        if (tag.contains("ImpactDamage")) {
            this.impactDamage = tag.getFloat("ImpactDamage");
        }
        this.entityData.set(DATA_ITEM, getPickupItemStackOrigin().copy());
    }
}

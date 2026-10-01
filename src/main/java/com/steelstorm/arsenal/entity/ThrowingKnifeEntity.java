package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** A fast, light throwing knife. 30% chance to cause Bleed; drops back on the ground after a hit. */
public class ThrowingKnifeEntity extends AbstractArrow implements ThrownWeaponEntity {
    public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level level) {
        super(type, level);
    }

    public ThrowingKnifeEntity(Level level, LivingEntity owner, ItemStack knife) {
        super(ModEntities.THROWING_KNIFE.get(), owner, level, knife, null);
        setBaseDamage(2.0);
    }

    @Override
    public ItemStack getRenderStack() {
        return getPickupItemStackOrigin();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        // Arrows vanish on hit; a knife clatters to the ground so it can be recovered.
        if (isRemoved() && !level().isClientSide && pickup == Pickup.ALLOWED && random.nextFloat() < 0.75F) {
            spawnAtLocation(getPickupItem(), 0.1F);
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (random.nextFloat() < 0.3F) {
            BleedEffect.apply(target, 100);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.THROWING_KNIFE.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }
}

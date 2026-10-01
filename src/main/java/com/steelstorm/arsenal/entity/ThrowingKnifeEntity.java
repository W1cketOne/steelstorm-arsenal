package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A fast, light throwing knife. 30% chance to cause Bleed; drops back on the ground after a hit.
 *
 * <p>Abilities throw spectral knives (glowing copies that vanish after landing), venom-coated
 * knives (poison plus bleed), and the Blink Knife, which pulls its thrower to wherever it lands.</p>
 */
public class ThrowingKnifeEntity extends AbstractArrow implements ThrownWeaponEntity {
    /** 0 = normal, 1 = spectral, 2 = venom, 3 = blink. */
    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(ThrowingKnifeEntity.class, EntityDataSerializers.BYTE);
    public static final byte NORMAL = 0;
    public static final byte SPECTRAL = 1;
    public static final byte VENOM = 2;
    public static final byte BLINK = 3;

    public static final int SPECTRAL_COLOR = 0xC7D2FE;
    public static final int VENOM_COLOR = 0x7CFF6B;
    public static final int BLINK_COLOR = 0xA78BFA;

    private boolean venomCoated;

    public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level level) {
        super(type, level);
    }

    public ThrowingKnifeEntity(Level level, LivingEntity owner, ItemStack knife) {
        super(ModEntities.THROWING_KNIFE.get(), owner, level, knife, null);
        setBaseDamage(2.0);
    }

    /** A knife made of light: it can't be picked up and fades shortly after landing. */
    public static ThrowingKnifeEntity spectral(LivingEntity owner, Vec3 from, Vec3 velocity, float damage, byte mode) {
        ThrowingKnifeEntity knife = new ThrowingKnifeEntity(owner.level(), owner, new ItemStack(ModItems.THROWING_KNIFE.get()));
        knife.setPos(from.x, from.y, from.z);
        knife.setDeltaMovement(velocity);
        double horizontal = velocity.horizontalDistance();
        knife.setYRot((float) Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        knife.setXRot((float) Math.toDegrees(Math.atan2(velocity.y, horizontal)));
        knife.yRotO = knife.getYRot();
        knife.xRotO = knife.getXRot();
        knife.pickup = Pickup.DISALLOWED;
        knife.entityData.set(DATA_MODE, mode);
        knife.venomCoated = mode == VENOM;
        // Arrow damage is speed x base damage.
        knife.setBaseDamage(damage / Math.max(0.5, velocity.length()));
        owner.level().addFreshEntity(knife);
        return knife;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODE, NORMAL);
    }

    public byte mode() {
        return entityData.get(DATA_MODE);
    }

    /** Coats a normal thrown knife in venom (Venom Coat). */
    public void coatInVenom() {
        venomCoated = true;
        if (mode() == NORMAL) {
            entityData.set(DATA_MODE, VENOM);
        }
    }

    private boolean isSpectral() {
        return pickup == Pickup.DISALLOWED && mode() != NORMAL;
    }

    public int glowColor() {
        return switch (mode()) {
            case VENOM -> VENOM_COLOR;
            case BLINK -> BLINK_COLOR;
            default -> SPECTRAL_COLOR;
        };
    }

    @Override
    public boolean glows() {
        return mode() != NORMAL;
    }

    @Override
    public ItemStack getRenderStack() {
        return getPickupItemStackOrigin();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (mode() != NORMAL && !inGround) {
                level().addParticle(ModParticles.GLOW.get().with(glowColor(), 0.9F), getX(), getY(), getZ(), 0, 0, 0);
            }
        } else if (isSpectral() && (inGroundTime > 10 || tickCount > 100)) {
            Fx.burst((ServerLevel) level(), ModParticles.GLOW.get(), glowColor(), 1.0F, position(), 5, 0.1, 0.03);
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (isSpectral() && result.getEntity() instanceof LivingEntity target) {
            // A flurry of spectral knives should all land, not bounce off the hurt cooldown.
            target.invulnerableTime = 0;
        }
        Entity hitEntity = result.getEntity();
        super.onHitEntity(result);
        if (mode() == BLINK && !level().isClientSide && getOwner() instanceof ServerPlayer player) {
            Vec3 behind = hitEntity.position().add(hitEntity.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.2));
            blink(player, behind);
        }
        // Arrows vanish on hit; a knife clatters to the ground so it can be recovered.
        if (isRemoved() && !level().isClientSide && pickup == Pickup.ALLOWED && random.nextFloat() < 0.75F) {
            spawnAtLocation(getPickupItem(), 0.1F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (mode() == BLINK && !level().isClientSide && getOwner() instanceof ServerPlayer player) {
            Vec3 at = result.getLocation().add(Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.6));
            blink(player, at);
            discard();
        }
    }

    /** Moves the thrower to `at`, or as close as fits without putting them inside a wall. */
    private void blink(ServerPlayer player, Vec3 at) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        Vec3 spot = null;
        for (int dy = 0; dy <= 2 && spot == null; dy++) {
            for (Vec3 candidate : new Vec3[]{at.add(0, -dy * 0.5, 0), at.add(0, dy * 0.5, 0)}) {
                AABB box = player.getDimensions(player.getPose()).makeBoundingBox(candidate);
                if (level.noCollision(player, box)) {
                    spot = candidate;
                    break;
                }
            }
        }
        if (spot == null) {
            BlockPos below = BlockPos.containing(at);
            Vec3 above = Vec3.atBottomCenterOf(below.above());
            if (level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(above))) {
                spot = above;
            }
        }
        if (spot == null || spot.distanceTo(from) > 40) {
            return;
        }
        Fx.burst(level, ModParticles.SMOKE.get(), 0x2A1F3D, 1.6F, from.add(0, 1, 0), 14, 0.3, 0.5, 0.3, 0.02);
        Fx.burst(level, ModParticles.GLOW.get(), BLINK_COLOR, 1.3F, from.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        player.teleportTo(spot.x, spot.y, spot.z);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        Fx.burst(level, ModParticles.SMOKE.get(), 0x2A1F3D, 1.6F, spot.add(0, 1, 0), 14, 0.3, 0.5, 0.3, 0.02);
        Fx.burst(level, ModParticles.GLOW.get(), BLINK_COLOR, 1.3F, spot.add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        Fx.sound(level, spot, ModSounds.ABILITY_SMOKE, 1.0F, 1.3F);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (venomCoated || mode() == VENOM) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            BleedEffect.apply(target, 100);
            if (level() instanceof ServerLevel level) {
                Fx.burst(level, ModParticles.GLOW.get(), VENOM_COLOR, 1.2F, target.getBoundingBox().getCenter(), 8, 0.3, 0.04);
            }
        } else if (random.nextFloat() < 0.3F) {
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

    @Override
    public boolean shouldBeSaved() {
        return !isSpectral() && super.shouldBeSaved();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Venom", venomCoated);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        venomCoated = tag.getBoolean("Venom");
    }
}

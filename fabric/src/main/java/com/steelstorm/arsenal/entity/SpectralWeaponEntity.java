package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A giant glowing copy of a weapon that hangs in the sky for a moment, then drops point first
 * and sticks in the ground. What happens on impact is up to the ability that summoned it.
 *
 * <p>Its whole flight is a function of its age, so the client animates it on its own and never
 * needs position updates.</p>
 */
public class SpectralWeaponEntity extends Entity implements IEntityWithComplexSpawn {
    private ItemStack stack = ItemStack.EMPTY;
    private float scale = 2.0F;
    private int color = Fx.STEEL;
    private int hover = 8;
    private float dropHeight = 10.0F;
    private int fallTicks = 5;
    private int stuckTicks = 24;
    private float sink = 0.6F;
    private Vec3 impact = Vec3.ZERO;
    @Nullable
    private Consumer<SpectralWeaponEntity> onImpact;

    public SpectralWeaponEntity(EntityType<? extends SpectralWeaponEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /**
     * Summons a weapon `height` blocks above `impact` that drops after `hover` ticks.
     *
     * @param onImpact runs on the server the moment the point hits the ground
     */
    public static SpectralWeaponEntity drop(ServerLevel level, ItemStack stack, Vec3 impact, float scale, int color, int hover,
                                            float height, int fallTicks, int stuckTicks, @Nullable Consumer<SpectralWeaponEntity> onImpact) {
        SpectralWeaponEntity e = new SpectralWeaponEntity(ModEntities.SPECTRAL_WEAPON.get(), level);
        e.stack = stack.copyWithCount(1);
        e.impact = impact;
        e.scale = scale;
        e.color = color;
        e.hover = hover;
        e.dropHeight = height;
        e.fallTicks = Math.max(1, fallTicks);
        e.stuckTicks = stuckTicks;
        e.onImpact = onImpact;
        e.setYRot(level.random.nextFloat() * 360.0F);
        Vec3 p = e.positionAt(0);
        e.setPos(p.x, p.y, p.z);
        e.setOldPosAndRot();
        level.addFreshEntity(e);
        return e;
    }

    public ItemStack stack() {
        return stack;
    }

    public float scale() {
        return scale;
    }

    public int color() {
        return color;
    }

    public Vec3 impact() {
        return impact;
    }

    public boolean hasLanded() {
        return tickCount >= hover + fallTicks;
    }

    /** Where the tip is `age` ticks after the summon. */
    public Vec3 positionAt(float age) {
        if (age < hover) {
            float bob = Mth.sin(age * 0.5F) * 0.08F;
            float appear = Mth.clamp(age / 4.0F, 0, 1);
            return impact.add(0, dropHeight + 1.5F * (1 - appear) + bob, 0);
        }
        float t = (age - hover) / fallTicks;
        if (t < 1) {
            return impact.add(0, dropHeight * (1 - t * t), 0);
        }
        return impact.add(0, -sink, 0);
    }

    /** 0..1 while the weapon fades in; used to grow it out of nothing. */
    public float appear(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / 4.0F, 0.05F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 p = positionAt(tickCount);
        setPos(p.x, p.y, p.z);
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (tickCount == hover + fallTicks && onImpact != null) {
            onImpact.accept(this);
        }
        if (tickCount >= hover + fallTicks + stuckTicks) {
            discard();
        }
    }

    private void clientTick() {
        double len = 1.4 * scale;
        if (tickCount < hover) {
            // A circle of runes marks where it will land.
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = 0.6 + scale * 0.35;
                level().addParticle(ModParticles.RUNE.get().with(color, 0.9F), impact.x + Math.cos(a) * r, impact.y + 0.1,
                        impact.z + Math.sin(a) * r, 0, 0.02, 0);
            }
        }
        if (!hasLanded()) {
            for (int i = 0; i < 3; i++) {
                double up = random.nextDouble() * len;
                level().addParticle(ModParticles.GLOW.get().with(color, 1.2F), getX() + (random.nextDouble() - 0.5) * 0.4,
                        getY() + up, getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0.05, 0);
            }
        } else if (random.nextInt(3) == 0) {
            level().addParticle(ModParticles.GLOW.get().with(color, 1.0F), getX() + (random.nextDouble() - 0.5) * 0.5,
                    getY() + random.nextDouble() * len * 0.6, getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0.04, 0);
        }
    }

    @Override
    public void onClientRemoval() {
        double len = 1.4 * scale;
        for (int i = 0; i < 24; i++) {
            level().addParticle(ModParticles.GLOW.get().with(color, 1.5F), getX() + (random.nextDouble() - 0.5) * 0.6,
                    getY() + random.nextDouble() * len, getZ() + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.1, 0.05 + random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.1);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        buf.writeFloat(scale);
        buf.writeInt(color);
        buf.writeVarInt(hover);
        buf.writeFloat(dropHeight);
        buf.writeVarInt(fallTicks);
        buf.writeVarInt(stuckTicks);
        buf.writeDouble(impact.x);
        buf.writeDouble(impact.y);
        buf.writeDouble(impact.z);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        scale = buf.readFloat();
        color = buf.readInt();
        hover = buf.readVarInt();
        dropHeight = buf.readFloat();
        fallTicks = buf.readVarInt();
        stuckTicks = buf.readVarInt();
        impact = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}

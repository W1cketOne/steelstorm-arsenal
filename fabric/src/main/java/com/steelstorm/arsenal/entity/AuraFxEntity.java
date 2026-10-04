package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A purely visual, glowing effect: an expanding halo ring, a gyroscope of rings, orbiting orbs, a
 * pillar of light, a sunburst or a spinning magic circle. Everything is sent once when it spawns and
 * then animated by the client, so it costs no network traffic while it plays.
 */
public class AuraFxEntity extends Entity implements IEntityWithComplexSpawn {
    public enum Style { HALO, GYRO, ORBIT, PILLAR, SUNBURST, CIRCLE, CRACKS }

    private Style style = Style.HALO;
    private int color = 0xFFFFFF;
    private int color2 = 0xFFFFFF;
    private float radius = 4;
    private int duration = 20;
    private int count = 8;
    private float height = 1;
    private int followId = -1;

    public AuraFxEntity(EntityType<? extends AuraFxEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /**
     * @param color  main colour; {@code color2} is the secondary (inner rings, orb cores, sparkles)
     * @param radius final radius (halos, circles, orbits, sunbursts) or beam width (pillars)
     * @param count  orbs in an orbit, rays in a sunburst
     * @param height pillar height, orbit height above the feet
     * @param follow an entity to stay centred on (null to stay put)
     */
    public static AuraFxEntity spawn(ServerLevel level, Style style, Vec3 at, int color, int color2, float radius, int duration,
                                     int count, float height, @Nullable Entity follow) {
        AuraFxEntity fx = new AuraFxEntity(ModEntities.AURA_FX.get(), level);
        fx.style = style;
        fx.color = color;
        fx.color2 = color2;
        fx.radius = radius;
        fx.duration = Math.max(2, duration);
        fx.count = count;
        fx.height = height;
        fx.followId = follow == null ? -1 : follow.getId();
        fx.setPos(at.x, at.y, at.z);
        level.addFreshEntity(fx);
        return fx;
    }

    public Style style() {
        return style;
    }

    public int color() {
        return color;
    }

    public int color2() {
        return color2;
    }

    public float radius() {
        return radius;
    }

    public int duration() {
        return duration;
    }

    public int count() {
        return count;
    }

    public float height() {
        return height;
    }

    @Nullable
    public Entity follow() {
        return followId < 0 ? null : level().getEntity(followId);
    }

    /** 0..1 over the effect's life. */
    public float progress(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / duration, 0, 1);
    }

    /** Where the effect is centred this frame (on its followed entity if any). */
    public Vec3 center(float partialTick) {
        Entity follow = follow();
        return follow != null ? follow.getPosition(partialTick) : getPosition(partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Entity follow = follow();
            if (follow != null) {
                setPos(follow.getX(), follow.getY(), follow.getZ());
            }
            if (tickCount >= duration) {
                discard();
            }
            return;
        }
        // Sparkles thrown off the effect.
        Vec3 c = center(1.0F);
        float p = progress(0);
        switch (style) {
            case HALO -> {
                float r = radius * easeOut(p);
                for (int i = 0; i < 4; i++) {
                    float a = random.nextFloat() * Mth.TWO_PI;
                    level().addParticle(ModParticles.SPARKLE.get().with(i % 2 == 0 ? color : color2, 0.9F + random.nextFloat() * 0.6F),
                            c.x + Mth.cos(a) * r, c.y + 0.15 + random.nextFloat() * 0.5, c.z + Mth.sin(a) * r, 0, 0.02, 0);
                }
            }
            case ORBIT -> {
                float t = tickCount * 0.35F;
                for (int i = 0; i < count; i++) {
                    if (random.nextInt(3) != 0) {
                        continue;
                    }
                    float a = t + Mth.TWO_PI * i / count;
                    level().addParticle(ModParticles.SPARKLE.get().with(color2, 0.6F), c.x + Mth.cos(a) * radius,
                            c.y + height + Mth.sin(a * 2) * 0.25, c.z + Mth.sin(a) * radius, 0, -0.01, 0);
                }
            }
            case PILLAR, CIRCLE -> {
                for (int i = 0; i < 3; i++) {
                    float a = random.nextFloat() * Mth.TWO_PI;
                    float r = random.nextFloat() * radius;
                    level().addParticle(ModParticles.SPARKLE.get().with(random.nextBoolean() ? color : color2, 0.8F),
                            c.x + Mth.cos(a) * r, c.y + random.nextFloat() * (style == Style.PILLAR ? height : 0.6F), c.z + Mth.sin(a) * r,
                            0, 0.08 + random.nextFloat() * 0.1, 0);
                }
            }
            default -> { }
        }
    }

    public static float easeOut(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
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
        buf.writeVarInt(style.ordinal());
        buf.writeInt(color);
        buf.writeInt(color2);
        buf.writeFloat(radius);
        buf.writeVarInt(duration);
        buf.writeVarInt(count);
        buf.writeFloat(height);
        buf.writeVarInt(followId);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        style = Style.values()[Mth.clamp(buf.readVarInt(), 0, Style.values().length - 1)];
        color = buf.readInt();
        color2 = buf.readInt();
        radius = buf.readFloat();
        duration = buf.readVarInt();
        count = buf.readVarInt();
        height = buf.readFloat();
        followId = buf.readVarInt();
    }
}

package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A crescent of energy flying away from a sword swing. It passes through enemies, hitting each
 * one once per pass, and breaks against walls. A boomerang wave flies back to its owner and can
 * hit everything a second time.
 */
public class SlashWaveEntity extends Projectile implements IEntityWithComplexSpawn {
    private int color = Fx.STEEL;
    private float halfWidth = 2.0F;
    private float roll;
    private boolean boomerang;
    private boolean returning;
    private int maxAge = 14;
    private float speed = 1.2F;
    private float damage;
    @Nullable
    private Consumer<LivingEntity> onHit;
    private final Set<Integer> hit = new HashSet<>();

    public SlashWaveEntity(EntityType<? extends SlashWaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /**
     * Fires a wave from `start` along `dir`.
     *
     * @param halfWidth half the width of the crescent, in blocks
     * @param roll      0 for a horizontal crescent, 90 for a vertical one
     */
    public static SlashWaveEntity fire(LivingEntity owner, Vec3 start, Vec3 dir, float speed, float range, float halfWidth, float roll,
                                       float damage, int color, boolean boomerang, @Nullable Consumer<LivingEntity> onHit) {
        SlashWaveEntity wave = new SlashWaveEntity(ModEntities.SLASH_WAVE.get(), owner.level());
        wave.setOwner(owner);
        wave.setPos(start.x, start.y, start.z);
        Vec3 v = dir.normalize().scale(speed);
        wave.setDeltaMovement(v);
        wave.speed = speed;
        wave.maxAge = Math.max(2, Math.round(range / speed));
        wave.halfWidth = halfWidth;
        wave.roll = roll;
        wave.damage = damage;
        wave.color = color;
        wave.boomerang = boomerang;
        wave.onHit = onHit;
        wave.faceMotion();
        owner.level().addFreshEntity(wave);
        return wave;
    }

    public int color() {
        return color;
    }

    public float halfWidth() {
        return halfWidth;
    }

    public float roll() {
        return roll;
    }

    /** Waves grow out of the swing in their first ticks and fade at the end of their flight. */
    public float scaleAt(float partialTick) {
        float age = tickCount + partialTick;
        float grow = Mth.clamp(age / 3.0F, 0.35F, 1.0F);
        float fade = returning ? 1.0F : Mth.clamp((maxAge + 3 - age) / 4.0F, 0.0F, 1.0F);
        return grow * (boomerang ? 1.0F : Math.max(0.25F, fade));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            serverTick();
            if (isRemoved()) {
                return;
            }
        } else if (tickCount > 1) {
            trailParticles();
        }
        Vec3 m = getDeltaMovement();
        setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        faceMotion();
    }

    private void serverTick() {
        ServerLevel level = (ServerLevel) level();
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity livingOwner) || !owner.isAlive() || owner.level() != level || tickCount > 120) {
            discard();
            return;
        }
        Vec3 m = getDeltaMovement();
        // Everything the crescent sweeps through this tick.
        AABB sweep = getBoundingBox().expandTowards(m).inflate(halfWidth * 0.85, roll > 45 ? halfWidth * 0.7 : 0.9, halfWidth * 0.85);
        Vec3 dir = m.lengthSqr() < 1e-6 ? Vec3.ZERO : m.normalize();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> CombatUtil.isEnemyOf(livingOwner, e))) {
            if (hit.contains(e.getId())) {
                continue;
            }
            Vec3 to = e.getBoundingBox().getCenter().subtract(position());
            double along = to.dot(dir);
            if (along < -1.5 || along > m.length() + 1.5) {
                continue;
            }
            hit.add(e.getId());
            if (CombatUtil.specialHurt(livingOwner, e, damage)) {
                Vec3 at = e.getBoundingBox().getCenter();
                Fx.impact(level, at, color, 1.0F);
                Fx.sparks(level, color, at, 5, 0.4);
                e.knockback(0.35, -dir.x, -dir.z);
                if (onHit != null) {
                    onHit.accept(e);
                }
            }
        }
        // Walls stop the wave (a boomerang wave turns back instead).
        BlockPos at = BlockPos.containing(position().add(m));
        if (!returning && GroundWaveEntity.isSolid(level, at) && GroundWaveEntity.isSolid(level, at.above())) {
            Fx.burst(level, ModParticles.GLOW.get(), color, 1.5F, position(), 14, 0.4, 0.15);
            Fx.sparks(level, color, position(), 8, 0.5);
            if (boomerang) {
                turnBack();
            } else {
                Fx.sound(level, position(), ModSounds.WEAPON_HIT_METAL, 0.6F, 1.4F);
                discard();
            }
            return;
        }
        if (!returning && tickCount >= maxAge) {
            if (boomerang) {
                turnBack();
            } else {
                Fx.burst(level, ModParticles.GLOW.get(), color, 1.2F, position(), 8, 0.4, 0.05);
                discard();
            }
            return;
        }
        if (returning) {
            Vec3 to = owner.getBoundingBox().getCenter().subtract(position());
            if (to.length() < 1.6) {
                Fx.burst(level, ModParticles.GLOW.get(), color, 1.2F, position(), 10, 0.3, 0.05);
                discard();
                return;
            }
            setDeltaMovement(to.normalize().scale(Math.min(speed * 1.25, to.length())));
        }
    }

    private void turnBack() {
        returning = true;
        hit.clear();
        Fx.sound(level(), position(), ModSounds.ABILITY_SLASH_WAVE, 0.8F, 1.3F);
    }

    private void faceMotion() {
        Vec3 m = getDeltaMovement();
        if (m.lengthSqr() > 1e-6) {
            setYRot((float) Math.toDegrees(Math.atan2(-m.x, m.z)));
            setXRot((float) -Math.toDegrees(Math.atan2(m.y, m.horizontalDistance())));
        }
    }

    private void trailParticles() {
        Vec3 m = getDeltaMovement();
        Vec3 dir = m.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : m.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        if (side.lengthSqr() < 1e-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        double r = Math.toRadians(roll);
        Vec3 axis = side.scale(Math.cos(r)).add(up.scale(Math.sin(r)));
        for (int i = 0; i < 3; i++) {
            double s = (random.nextDouble() * 2 - 1) * halfWidth * scaleAt(0);
            double back = (1 - Math.cos(s / halfWidth * 1.2)) * halfWidth * 0.55;
            Vec3 p = position().add(axis.scale(s)).subtract(dir.scale(back));
            level().addParticle(ModParticles.GLOW.get().with(color, 1.3F), p.x, p.y, p.z, -m.x * 0.1, -m.y * 0.1, -m.z * 0.1);
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    public boolean shouldBeSaved() {
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
        buf.writeInt(color);
        buf.writeFloat(halfWidth);
        buf.writeFloat(roll);
        buf.writeBoolean(boomerang);
        buf.writeVarInt(maxAge);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        color = buf.readInt();
        halfWidth = buf.readFloat();
        roll = buf.readFloat();
        boomerang = buf.readBoolean();
        maxAge = buf.readVarInt();
    }
}

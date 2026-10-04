package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.entity.EarthChunkEntity;
import com.steelstorm.arsenal.entity.GroundWaveEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ground attacks: rings of earth rippling outward and lines of erupting stone. The damage is
 * done here on the server, in step with the wave front; {@link GroundWaveEntity} draws the
 * ground heaving on every client. Nothing in the world is actually changed.
 */
public final class Shockwaves {
    /** The solid surface block at (x, z) near height y, or null if there's none close by. */
    @Nullable
    public static BlockPos ground(ServerLevel level, double x, double y, double z) {
        return GroundWaveEntity.ground(level, x, y, z);
    }

    /** Makes one ground block jump up and fall back (crater rims, debris). */
    public static void pop(ServerLevel level, BlockPos ground, float height, int lifetime) {
        BlockState state = level.getBlockState(ground);
        level.addFreshEntity(new EarthChunkEntity(level, ground, state, height, lifetime, true));
        if (level.random.nextInt(2) == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), ground.getX() + 0.5, ground.getY() + 1.05,
                    ground.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.15);
        }
    }

    /** A crater: the ground around `center` bursts upward, tallest in the middle. */
    public static void crater(ServerLevel level, Vec3 center, float radius, float height) {
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > radius || level.random.nextFloat() < 0.25F) {
                    continue;
                }
                BlockPos g = ground(level, center.x + dx, center.y, center.z + dz);
                if (g != null) {
                    float h = height * (float) (0.45 + 0.55 * (1 - d / (radius + 0.5))) * (0.7F + level.random.nextFloat() * 0.5F);
                    pop(level, g, h, 14 + level.random.nextInt(8));
                }
            }
        }
        dust(level, center, radius);
    }

    /**
     * An expanding ring of rippling earth. Every enemy the ring passes is hit once.
     *
     * @param speed blocks the ring travels per tick
     */
    public static void ring(ServerLevel level, LivingEntity source, Vec3 center, float radius, float speed, float damage,
                            double launch, int color, @Nullable Consumer<LivingEntity> onHit) {
        BlockPos g = ground(level, center.x, center.y, center.z);
        Vec3 base = g != null ? new Vec3(center.x, g.getY() + 1, center.z) : center;
        GroundWaveEntity.ring(level, base, radius, speed, 0.7F + Math.min(0.6F, radius * 0.05F), color);
        Fx.cracks(level, base, color, Math.min(6.0F, 1.5F + radius * 0.45F));
        debris(level, base, Math.min(5.0F, radius * 0.5F));
        Fx.ring(level, base, color, radius);
        Set<Integer> hit = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(radius / speed));
        for (int step = 1; step <= steps; step++) {
            final float r = Math.min(radius, step * speed);
            final float inner = Math.max(0, r - speed - 0.8F);
            ServerScheduler.schedule(step, () -> {
                AABB box = new AABB(base, base).inflate(r + 1, 3, r + 1);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> canHit(source, e))) {
                    double d = Math.hypot(e.getX() - base.x, e.getZ() - base.z);
                    if (d >= inner && d <= r + 0.9 && Math.abs(e.getY() - base.y) < 3.5 && hit.add(e.getId())) {
                        if (damage > 0) {
                            CombatUtil.specialHurt(source, e, damage);
                        }
                        throwUp(e, base, launch);
                        if (onHit != null) {
                            onHit.accept(e);
                        }
                    }
                }
            });
        }
    }

    /** A line of erupting stone running away from `start`, `speed` blocks per tick. */
    public static void line(ServerLevel level, LivingEntity source, Vec3 start, Vec3 dir, int length, double width,
                            float damage, double launch, int color, @Nullable Consumer<LivingEntity> onHit) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize();
        GroundWaveEntity.line(level, start, flat, length, (float) width, 1.0F, 1.1F, color);
        Set<Integer> hit = new HashSet<>();
        for (int i = 1; i <= length; i++) {
            final int step = i;
            ServerScheduler.schedule(i, () -> {
                Vec3 p = start.add(flat.scale(step));
                Fx.burst(level, ModParticles.GLOW.get(), color, 1.3F, p.add(0, 0.4, 0), 3, 0.3, 0.1, 0.3, 0.03);
                AABB box = new AABB(p, p).inflate(width + 0.7, 3.0, width + 0.7);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> canHit(source, e))) {
                    if (hit.add(e.getId())) {
                        if (damage > 0) {
                            CombatUtil.specialHurt(source, e, damage);
                        }
                        e.setDeltaMovement(e.getDeltaMovement().x * 0.3, launch * resist(e), e.getDeltaMovement().z * 0.3);
                        e.hurtMarked = true;
                        if (onHit != null) {
                            onHit.accept(e);
                        }
                    }
                }
            });
        }
    }

    /** Chunks of the ground flung into the air, spinning (block particles fired upward). */
    public static void debris(ServerLevel level, Vec3 at, float power) {
        BlockPos g = ground(level, at.x, at.y + 0.5, at.z);
        if (g == null) {
            return;
        }
        BlockState state = level.getBlockState(g);
        if (state.isAir()) {
            return;
        }
        for (int i = 0; i < 18 + (int) (power * 6); i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = level.random.nextDouble() * power * 0.6;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x + Math.cos(a) * r, g.getY() + 1.1,
                    at.z + Math.sin(a) * r, 0, Math.cos(a) * 0.25, 0.5 + level.random.nextDouble() * 0.6, Math.sin(a) * 0.25, 1.0);
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, g.getY() + 1.2, at.z, 3, power * 0.3, 0.1, power * 0.3, 0.01);
    }

    /** A burst of debris and dust at a point, without damage. */
    public static void dust(ServerLevel level, Vec3 at, float radius) {
        BlockPos g = ground(level, at.x, at.y, at.z);
        if (g != null) {
            BlockState state = level.getBlockState(g);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, g.getY() + 1.05, at.z,
                    (int) (radius * 12), radius * 0.5, 0.1, radius * 0.5, 0.2);
        }
        Fx.burst(level, ModParticles.SMOKE.get(), 0x8A8378, 1.6F, at.add(0, 0.2, 0), (int) (radius * 4), radius * 0.4, 0.1, radius * 0.4, 0.03);
    }

    public static boolean canHit(LivingEntity source, LivingEntity e) {
        if (source instanceof Player) {
            return CombatUtil.isEnemyOf(source, e);
        }
        if (e == source || !e.isAlive()) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return source instanceof Mob mob && e == mob.getTarget();
    }

    public static double resist(LivingEntity e) {
        return Math.max(0.15, 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }

    public static void throwUp(LivingEntity e, Vec3 from, double launch) {
        if (launch <= 0) {
            return;
        }
        Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize();
        double r = resist(e);
        e.setDeltaMovement(away.x * 0.45 * r, launch * r, away.z * 0.45 * r);
        e.hurtMarked = true;
    }

    private Shockwaves() {
    }
}

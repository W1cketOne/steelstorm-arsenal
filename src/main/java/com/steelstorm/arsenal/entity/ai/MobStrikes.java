package com.steelstorm.arsenal.entity.ai;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Area attacks used by telegraphed enemy moves. Only hits players and whatever the mob is targeting. */
public final class MobStrikes {
    public static List<LivingEntity> victims(Mob mob, double radius) {
        return mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius, 2.0, radius),
                e -> e != mob && e.isAlive() && (e == mob.getTarget()
                        || (e instanceof Player p && !p.isCreative() && !p.isSpectator())));
    }

    /** Hits everything in a cone in front of the mob. */
    public static void cone(Mob mob, double range, double halfAngle, float damage, double knockback) {
        Vec3 look = Vec3.directionFromRotation(0, mob.getYRot()).normalize();
        double cos = Math.cos(Math.toRadians(halfAngle));
        for (LivingEntity e : victims(mob, range)) {
            Vec3 to = e.position().subtract(mob.position()).multiply(1, 0, 1);
            if (to.length() <= range + 0.5 && (to.length() < 1 || to.normalize().dot(look) >= cos)) {
                if (e.hurt(mob.damageSources().mobAttack(mob), damage)) {
                    e.knockback(knockback, -look.x, -look.z);
                }
            }
        }
        groundBurst(mob, mob.position().add(look.scale(range * 0.5)), range * 0.5);
    }

    /** Hits everything around the mob and throws it into the air. */
    public static void slam(Mob mob, double radius, float damage, double lift) {
        for (LivingEntity e : victims(mob, radius)) {
            if (e.distanceTo(mob) <= radius + 0.5 && e.hurt(mob.damageSources().mobAttack(mob), damage)) {
                Vec3 away = e.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
                e.setDeltaMovement(away.x * 0.5, lift, away.z * 0.5);
                e.hurtMarked = true;
            }
        }
        groundBurst(mob, mob.position(), radius);
    }

    public static void groundBurst(Mob mob, Vec3 center, double radius) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12;
            double x = center.x + Math.cos(angle) * radius * mob.getRandom().nextDouble();
            double z = center.z + Math.sin(angle) * radius * mob.getRandom().nextDouble();
            BlockState ground = level.getBlockState(BlockPos.containing(x, center.y - 0.5, z));
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, center.y + 0.1, z, 3, 0.1, 0.2, 0.1, 0.2);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z, 1, 0, 0, 0, 0);
    }

    private MobStrikes() {
    }
}

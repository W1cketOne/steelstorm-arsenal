package com.steelstorm.arsenal.fx;

import com.steelstorm.arsenal.registry.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-side helpers for spawning Steelstorm particles and sounds that every nearby player sees and hears. */
public final class Fx {
    public static final int WHITE = 0xFFFFFF;
    public static final int STEEL = 0xDCE6F0;
    public static final int GOLD = 0xFFD166;
    public static final int FIRE = 0xFF8A2A;
    public static final int BLOOD = 0xD0182C;
    public static final int FROST = 0x9FF3FF;
    public static final int VOID = 0xB15CFF;
    public static final int STORM = 0x7FD8FF;
    public static final int LIGHTNING = 0xFFF27A;
    public static final int PETAL = 0xFFB7D5;
    public static final int EARTH = 0xC9A27A;
    public static final int SHADOW = 0x3A2E5C;

    /** A cloud of particles around a point. */
    public static void burst(ServerLevel level, FxParticleType type, int color, float scale, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(type.with(color, scale), at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void burst(ServerLevel level, FxParticleType type, int color, float scale, Vec3 at, int count,
                             double sx, double sy, double sz, double speed) {
        level.sendParticles(type.with(color, scale), at.x, at.y, at.z, count, sx, sy, sz, speed);
    }

    /** One particle moving with an exact velocity. */
    public static void shoot(ServerLevel level, FxParticleType type, int color, float scale, Vec3 at, Vec3 velocity) {
        level.sendParticles(type.with(color, scale), at.x, at.y, at.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
    }

    /** Sparks flying out in every direction (metal hits, parries, lightning). */
    public static void sparks(ServerLevel level, int color, Vec3 at, int count, double power) {
        for (int i = 0; i < count; i++) {
            Vec3 v = new Vec3(level.random.nextGaussian(), level.random.nextDouble() * 1.2, level.random.nextGaussian()).normalize()
                    .scale(power * (0.4 + level.random.nextDouble() * 0.8));
            shoot(level, ModParticles.SPARK.get(), color, 1.0F, at, v);
        }
    }

    /** A crescent slash trail facing the given direction. */
    public static void slash(ServerLevel level, Vec3 at, float yaw, float pitch, float roll, int color, float scale) {
        level.sendParticles(ModParticles.SLASH.get().oriented(color, scale, yaw, pitch, roll), at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    /** A slash in front of an entity, matching where it is looking. */
    public static void slashFrom(ServerLevel level, Entity e, double distance, float roll, int color, float scale) {
        Vec3 look = e.getLookAngle();
        Vec3 at = e.getEyePosition().add(look.scale(distance)).add(0, -0.3, 0);
        slash(level, at, e.getYRot(), e.getXRot(), roll, color, scale);
    }

    /** A flat ring expanding along the ground to `radius` blocks. */
    public static void ring(ServerLevel level, Vec3 at, int color, float radius) {
        level.sendParticles(ModParticles.SHOCKWAVE.get().with(color, radius), at.x, at.y + 0.08, at.z, 1, 0, 0, 0, 0);
    }

    public static void impact(ServerLevel level, Vec3 at, int color, float scale) {
        level.sendParticles(ModParticles.IMPACT.get().with(color, scale), at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    public static void sound(Level level, Vec3 at, Holder<SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Plays for everyone near the player except the player (who already heard it client-side). */
    public static void soundForOthers(Player player, Holder<SoundEvent> sound, float volume, float pitch) {
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private Fx() {
    }
}

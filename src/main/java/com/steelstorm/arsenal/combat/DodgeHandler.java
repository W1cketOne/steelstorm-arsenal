package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DodgeHandler {
    /** Initial roll speed; with ground friction this covers roughly 3 blocks. */
    private static final double ROLL_SPEED = 1.3;

    public static void tryDodge(ServerPlayer player, float forward, float strafe) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isFallFlying() || player.isSleeping()) {
            return;
        }
        CombatData data = Stamina.data(player);
        long now = player.level().getGameTime();
        if (now < data.dodgeCooldownEnd) {
            return;
        }
        if (!Stamina.tryConsume(player, Config.DODGE_COST.get().floatValue())) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.6F, 1.4F);
            Stamina.sync(player, true);
            return;
        }
        // Clamp the client-reported input; with no input, roll backwards (away from danger).
        forward = Mth.clamp(forward, -1, 1);
        strafe = Mth.clamp(strafe, -1, 1);
        if (Math.abs(forward) < 0.1F && Math.abs(strafe) < 0.1F) {
            forward = -1;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        Vec3 dir = new Vec3(strafe * cos - forward * sin, 0, forward * cos + strafe * sin).normalize();
        double speed = player.onGround() ? ROLL_SPEED : ROLL_SPEED * 0.55;
        player.setDeltaMovement(dir.x * speed, player.onGround() ? 0.12 : Math.max(player.getDeltaMovement().y, 0.0), dir.z * speed);
        player.hurtMarked = true;
        player.resetFallDistance();

        data.dodgeCooldownEnd = now + Config.DODGE_COOLDOWN_TICKS.get();
        data.invulnerableUntil = now + Config.DODGE_INVULNERABILITY_TICKS.get();
        player.stopUsingItem();

        ServerLevel level = player.serverLevel();
        Fx.sound(level, player.position(), ModSounds.PLAYER_DODGE, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        Fx.burst(level, ModParticles.SMOKE.get(), 0xB8B2A7, 1.1F, player.position().add(0, 0.15, 0), 8, 0.35, 0.05, 0.35, 0.02);
        Vec3 behind = player.position().subtract(dir.scale(0.6)).add(0, 1.0, 0);
        Fx.burst(level, ModParticles.GLOW.get(), Fx.STEEL, 0.8F, behind, 5, 0.25, 0.4, 0.25, 0.0);
        Stamina.sync(player, true);
    }

    public static boolean isInvulnerable(ServerPlayer player) {
        return player.level().getGameTime() < Stamina.data(player).invulnerableUntil;
    }

    private DodgeHandler() {
    }
}

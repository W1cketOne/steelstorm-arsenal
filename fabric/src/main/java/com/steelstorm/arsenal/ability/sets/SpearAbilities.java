package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Spear: reach, mobility and a dragon's dive from the sky. */
public final class SpearAbilities {
    public static AbilitySet create() {
        return new AbilitySet("spear",
                Ability.of("spear_impale", "Impale",
                        "Lunge forward and drive the spear through everything in a 7-block line, pinning them in place.",
                        25, 120, SpearAbilities::impale),
                Ability.of("spear_vault_leap", "Vault Leap",
                        "Pole-vault high and far forward. You land with a shockwave that knocks nearby enemies up.",
                        25, 120, SpearAbilities::vaultLeap),
                Ability.of("spear_sweeping_arc", "Sweeping Arc",
                        "A huge sweep that covers almost everything in front of and beside you, throwing enemies back hard.",
                        30, 140, SpearAbilities::sweepingArc),
                Ability.ultimate("spear_dragon_dive", "Dragon Dive",
                        "Leap high into the air and dive like a dragon onto the spot you look at (up to 20 blocks away), "
                                + "blasting a crater and splitting the ground in four directions.",
                        600, SpearAbilities::dragonDive));
    }

    static boolean impale(AbilityContext ctx) {
        int c = ctx.color();
        ctx.dash(ctx.look(), 2.5);
        Vec3 from = ctx.eye().add(0, -0.3, 0);
        Vec3 dir = ctx.look();
        for (double d = 0.5; d <= 7; d += 0.35) {
            Vec3 p = from.add(dir.scale(d));
            Fx.shoot(ctx.level, ModParticles.GLOW.get(), c, 1.1F - (float) d * 0.08F, p, dir.scale(0.15));
        }
        Fx.impact(ctx.level, from.add(dir.scale(7)), c, 1.0F);
        for (LivingEntity e : ctx.line(from, dir, 7, 0.9)) {
            if (ctx.hit(e, 1.5F, c)) {
                ctx.pin(e, 30);
            }
        }
        ctx.sound(ModSounds.WEAPON_THROW, 1.0F, 0.8F);
        ctx.sound(ModSounds.WEAPON_HIT_METAL, 0.6F, 1.5F);
        return true;
    }

    static boolean vaultLeap(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 dir = ctx.flatLook();
        Shockwaves.dust(ctx.level, ctx.pos(), 1.0F);
        ctx.sound(ModSounds.ABILITY_DASH, 1.0F, 0.8F);
        ctx.leap(dir.scale(1.35).add(0, 0.95, 0), land -> {
            land.shockwave(land.pos(), 3.5F, 0.7F, 1.0F, 0.75, c, null);
            Fx.impact(land.level, land.pos().add(0, 0.3, 0), c, 1.5F);
            land.sound(ModSounds.ABILITY_SHOCKWAVE, 0.9F, 1.3F);
            land.shake(0.4F, 6);
        });
        for (int i = 1; i <= 8; i++) {
            ctx.later(i * 2, () -> Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.0F, ctx.pos().add(0, 0.8, 0), 3, 0.2, 0.3, 0.2, 0.0));
        }
        return true;
    }

    static boolean sweepingArc(AbilityContext ctx) {
        int c = ctx.color();
        float yaw = ctx.player.getYRot();
        Vec3 center = ctx.pos().add(0, 1.1, 0);
        ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw - 50).scale(1.8)), yaw - 50, 0, c, 1.4F);
        ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw + 50).scale(1.8)), yaw + 50, 0, c, 1.4F);
        ctx.later(2, () -> ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw).scale(2.2)), yaw, 0, 0xFFFFFF, 1.6F));
        for (LivingEntity e : ctx.cone(5.5, 100)) {
            if (ctx.hit(e, 1.1F, c)) {
                ctx.knockFrom(e, ctx.pos(), 1.5, 0.4);
            }
        }
        ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 1.1F, 1.0F);
        return true;
    }

    static boolean dragonDive(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 target = ctx.aimPoint(20);
        ctx.sound(ModSounds.ABILITY_DASH, 1.2F, 0.6F);
        Shockwaves.dust(ctx.level, ctx.pos(), 1.5F);
        ctx.data().noFallUntil = ctx.now() + 100;
        ctx.player.setDeltaMovement(0, 1.7, 0);
        ctx.player.hurtMarked = true;
        Fx.ring(ctx.level, target, c, 6.0F);
        Fx.burst(ctx.level, ModParticles.RUNE.get(), c, 1.3F, target.add(0, 0.2, 0), 20, 2.5, 0.1, 2.5, 0.01);
        for (int i = 1; i < 12; i++) {
            ctx.later(i, () -> {
                Vec3 p = ctx.pos().add(0, 0.5, 0);
                for (int k = 0; k < 4; k++) {
                    double a = ctx.level.random.nextDouble() * Math.PI * 2;
                    Fx.shoot(ctx.level, ModParticles.GLOW.get(), c, 1.4F, p, new Vec3(Math.cos(a) * 0.2, -0.3, Math.sin(a) * 0.2));
                }
            });
        }
        ctx.later(12, () -> {
            Vec3 to = target.subtract(ctx.pos());
            Vec3 v = to.normalize().scale(Math.min(3.0, 1.2 + to.length() * 0.1));
            ctx.player.setDeltaMovement(v);
            ctx.player.hurtMarked = true;
            ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 1.2F, 0.6F);
            com.steelstorm.arsenal.ability.AbilityManager.onLanding(ctx.player, 40, () -> {
                Vec3 at = ctx.pos();
                ctx.shockwave(at, 6.0F, 0.9F, 2.5F, 1.0, c, e -> ctx.stagger(e, 40));
                Shockwaves.crater(ctx.level, at, 3.0F, 1.4F);
                for (int k = 0; k < 4; k++) {
                    Vec3 dir = Vec3.directionFromRotation(0, ctx.player.getYRot() + 45 + k * 90);
                    ctx.fissure(at.add(dir.scale(1.5)), dir, 8, 0.8, 1.2F, 0.9, c, null);
                }
                Fx.impact(ctx.level, at.add(0, 0.5, 0), c, 3.2F);
                Fx.sparks(ctx.level, c, at.add(0, 0.3, 0), 20, 0.9);
                for (int k = 0; k < 30; k++) {
                    double a = k * Math.PI / 15;
                    Fx.shoot(ctx.level, ModParticles.GLOW.get(), c, 1.8F, at.add(0, 0.3, 0),
                            new Vec3(Math.cos(a) * 0.5, 0.35 + ctx.level.random.nextDouble() * 0.3, Math.sin(a) * 0.5));
                }
                ctx.sound(ModSounds.ABILITY_SHOCKWAVE, 1.5F, 0.7F);
                ctx.sound(ModSounds.ABILITY_GROUND_CRACK, 1.3F, 0.8F);
                ctx.shakeNearby(at, 28, 1.6F, 18);
            });
        });
        return true;
    }

    private SpearAbilities() {
    }
}

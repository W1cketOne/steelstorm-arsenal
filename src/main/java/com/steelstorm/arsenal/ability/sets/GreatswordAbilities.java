package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Greatsword: slow, enormous swings that split the ground. */
public final class GreatswordAbilities {
    public static AbilitySet create() {
        return new AbilitySet("greatsword",
                Ability.of("greatsword_ground_cleave", "Ground Cleave",
                        "Bring the blade down so hard the ground splits open in a line in front of you, launching enemies.",
                        35, 160, GreatswordAbilities::groundCleave),
                Ability.of("greatsword_crescent_wave", "Crescent Wave",
                        "Swing a wave of force that flies 16 blocks, passing through every enemy in its way.",
                        30, 140, GreatswordAbilities::crescentWave),
                Ability.of("greatsword_cyclone", "Cyclone",
                        "Spin with the blade outstretched for a second, hitting everything around you four times and dragging it in.",
                        35, 180, GreatswordAbilities::cyclone),
                Ability.ultimate("greatsword_colossus_strike", "Colossus Strike",
                        "Raise the greatsword for a moment, then slam it down: a 16-block fissure tears open ahead of you "
                                + "and a shockwave bursts out around you.",
                        600, GreatswordAbilities::colossusStrike));
    }

    static boolean groundCleave(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 dir = ctx.flatLook();
        Vec3 start = ctx.pos().add(dir.scale(1.4));
        ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 1.1F, 0.8F);
        ctx.slash(95, c, 1.4F, 1.8);
        ctx.later(3, () -> {
            for (LivingEntity e : ctx.cone(3.0, 45)) {
                ctx.hit(e, 1.4F, c);
            }
            ctx.fissure(start, dir, 8, 1.1, 1.2F, 0.75, c, null);
            Shockwaves.dust(ctx.level, start, 1.5F);
            Fx.impact(ctx.level, start.add(0, 0.3, 0), c, 1.8F);
            ctx.soundAt(start, ModSounds.ABILITY_GROUND_CRACK, 1.2F, 0.9F);
            ctx.shakeNearby(start, 14, 0.7F, 8);
        });
        return true;
    }

    static boolean crescentWave(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 start = ctx.eye().add(0, -0.45, 0).add(ctx.look().scale(1.2));
        ctx.slash(10, c, 1.4F, 1.6);
        SlashWaveEntity.fire(ctx.player, start, ctx.look(), 1.25F, 16, 2.4F, 18, ctx.dmg(1.3F), c, false, null);
        ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 1.1F, 0.9F);
        ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 0.8F, 1.0F);
        return true;
    }

    static boolean cyclone(AbilityContext ctx) {
        int c = ctx.color();
        ctx.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, 0, false, false, false));
        for (int i = 0; i < 4; i++) {
            final int pulse = i;
            ctx.later(i * 5, () -> {
                Vec3 center = ctx.pos();
                float baseYaw = ctx.player.getYRot() + pulse * 90;
                for (int k = 0; k < 3; k++) {
                    float yaw = baseYaw + k * 120;
                    Vec3 at = center.add(Vec3.directionFromRotation(0, yaw).scale(1.6)).add(0, 1.0, 0);
                    ctx.slashAt(at, yaw + 90, 0, c, 1.15F);
                }
                for (LivingEntity e : ctx.around(4.0)) {
                    if (ctx.hit(e, 0.7F, c)) {
                        ctx.pull(e, center, 0.35);
                    }
                }
                Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.2F, center.add(0, 0.6, 0), 8, 2.0, 0.2, 2.0, 0.02);
                ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 0.9F, 0.9F + pulse * 0.08F);
            });
        }
        return true;
    }

    static boolean colossusStrike(AbilityContext ctx) {
        int c = ctx.color();
        ctx.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 4, false, false, false));
        ctx.sound(ModSounds.ABILITY_CAST, 1.2F, 0.6F);
        for (int i = 0; i < 15; i += 3) {
            final int t = i;
            ctx.later(i, () -> {
                Vec3 eye = ctx.player.getBoundingBox().getCenter();
                for (int k = 0; k < 6; k++) {
                    double a = ctx.level.random.nextDouble() * Math.PI * 2;
                    Vec3 from = eye.add(Math.cos(a) * 2.5, ctx.level.random.nextDouble() * 2 - 0.5, Math.sin(a) * 2.5);
                    Fx.shoot(ctx.level, ModParticles.GLOW.get(), c, 1.5F, from, eye.subtract(from).scale(0.12));
                }
                Fx.ring(ctx.level, ctx.pos(), c, 1.0F + t * 0.1F);
            });
        }
        ctx.later(15, () -> {
            Vec3 dir = ctx.flatLook();
            Vec3 start = ctx.pos().add(dir.scale(1.5));
            ctx.slash(95, c, 2.4F, 2.0);
            ctx.slash(95, 0xFFFFFF, 1.8F, 2.2);
            ctx.fissure(start, dir, 16, 2.0, 3.0F, 1.1, c, e -> ctx.stagger(e, 40));
            ctx.shockwave(ctx.pos(), 5.0F, 0.8F, 2.0F, 0.7, c, null);
            Shockwaves.crater(ctx.level, start, 2.0F, 1.2F);
            Fx.impact(ctx.level, start.add(0, 0.4, 0), c, 3.0F);
            Fx.sparks(ctx.level, c, start.add(0, 0.3, 0), 24, 0.9);
            ctx.soundAt(start, ModSounds.ABILITY_SHOCKWAVE, 1.5F, 0.7F);
            ctx.soundAt(start, ModSounds.ABILITY_GROUND_CRACK, 1.4F, 0.7F);
            ctx.soundAt(start, ModSounds.ABILITY_RUMBLE, 1.2F, 0.8F);
            ctx.shakeNearby(start, 26, 1.5F, 18);
        });
        return true;
    }

    private GreatswordAbilities() {
    }
}

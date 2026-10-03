package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.entity.ThrowingKnifeEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Dual daggers: an assassin's kit of flurries, shadows and knives. */
public final class DaggerAbilities {
    public static AbilitySet create() {
        return new AbilitySet("dual_daggers",
                Ability.of("dual_daggers_flurry", "Flurry",
                        "Five lightning-quick stabs at the enemy in front of you. The last one opens a bleeding wound.",
                        30, 140, DaggerAbilities::flurry),
                Ability.of("dual_daggers_shadowstep", "Shadowstep",
                        "Step through the shadows to appear behind the enemy you're looking at (up to 12 blocks). "
                                + "Your next hit within 3 seconds is a backstab for 2.5x damage.",
                        25, 160, DaggerAbilities::shadowstep),
                Ability.of("dual_daggers_fan_of_knives", "Fan of Knives",
                        "Spin and fling a ring of twelve spectral knives in every direction.",
                        30, 160, DaggerAbilities::fanOfKnives).withCharges(3),
                Ability.ultimate("dual_daggers_death_blossom", "Death Blossom",
                        "Melt into the shadows for 5 seconds: you turn invisible and fast, enemies lose track of you, and "
                                + "your next four hits deal triple damage.",
                        600, DaggerAbilities::deathBlossom));
    }

    static boolean flurry(AbilityContext ctx) {
        LivingEntity target = ctx.aimed(4.5);
        if (target == null) {
            return false;
        }
        int c = ctx.color();
        for (int i = 0; i < 5; i++) {
            final int n = i;
            ctx.later(i * 2, () -> {
                if (!target.isAlive() || ctx.player.distanceToSqr(target) > 6 * 6) {
                    return;
                }
                Vec3 at = target.getBoundingBox().getCenter();
                ctx.hit(target, 0.55F, c);
                Fx.slash(ctx.level, at, ctx.player.getYRot(), 0, n % 2 == 0 ? 35 : -35, c, 0.55F);
                ctx.sound(ModSounds.WEAPON_HIT, 0.7F, 1.2F + n * 0.1F);
                ctx.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                if (n == 4) {
                    ctx.bleed(target, 140, 2);
                }
            });
        }
        return true;
    }

    static boolean shadowstep(AbilityContext ctx) {
        LivingEntity target = ctx.aimed(12.0);
        if (target == null) {
            return false;
        }
        Vec3 from = ctx.pos();
        if (!ctx.blinkTo(ctx.behind(target, 1.1), target.getEyePosition())) {
            return false;
        }
        int c = ctx.color();
        Fx.burst(ctx.level, ModParticles.SMOKE.get(), 0x241A38, 1.6F, from.add(0, 1, 0), 16, 0.35, 0.5, 0.35, 0.02);
        Fx.burst(ctx.level, ModParticles.SMOKE.get(), 0x241A38, 1.6F, ctx.pos().add(0, 1, 0), 16, 0.35, 0.5, 0.35, 0.02);
        Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.2F, ctx.pos().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.04);
        ctx.soundAt(from, ModSounds.ABILITY_SMOKE, 1.0F, 1.0F);
        ctx.sound(ModSounds.ABILITY_SMOKE, 1.0F, 1.3F);
        ctx.data().backstabUntil = ctx.now() + 60;
        if (target instanceof Mob mob && mob.getTarget() == ctx.player) {
            mob.getNavigation().stop();
        }
        return true;
    }

    static boolean fanOfKnives(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 from = ctx.pos().add(0, 1.2, 0);
        float damage = ctx.dmg(0.6F);
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(ctx.player.getYRot()) + i * Math.PI * 2 / 12;
            Vec3 dir = new Vec3(-Math.sin(a), 0.03, Math.cos(a));
            ThrowingKnifeEntity.spectral(ctx.player, from.add(dir.scale(0.6)), dir.scale(1.7), damage, ThrowingKnifeEntity.SPECTRAL);
        }
        Fx.ring(ctx.level, ctx.pos(), c, 3.0F);
        ctx.slash(0, c, 1.0F, 0.5);
        ctx.sound(ModSounds.WEAPON_THROW, 1.0F, 1.2F);
        ctx.later(2, () -> ctx.sound(ModSounds.WEAPON_THROW, 0.8F, 1.5F));
        return true;
    }

    static boolean deathBlossom(AbilityContext ctx) {
        int c = ctx.color();
        ctx.player.addEffect(new MobEffectInstance(ModEffects.SHADOW_VEIL, 100, 0, false, true, true));
        ctx.player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
        ctx.data().empoweredHits = 4;
        ctx.data().empoweredMultiplier = 3.0F;
        for (Mob mob : ctx.level.getEntitiesOfClass(Mob.class, ctx.player.getBoundingBox().inflate(32), m -> m.getTarget() == ctx.player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        Vec3 center = ctx.player.getBoundingBox().getCenter();
        Fx.burst(ctx.level, ModParticles.SMOKE.get(), 0x1A1026, 2.0F, center, 30, 0.8, 0.6, 0.8, 0.05);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12;
            Fx.shoot(ctx.level, ModParticles.PETAL.get(), c, 1.4F, center, new Vec3(Math.cos(a), 0.15, Math.sin(a)).scale(0.35));
        }
        ctx.sound(ModSounds.ABILITY_SMOKE, 1.2F, 0.7F);
        return true;
    }

    private DaggerAbilities() {
    }
}

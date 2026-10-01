package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Katana: blinding speed, delayed cuts and falling petals. */
public final class KatanaAbilities {
    public static AbilitySet create() {
        return new AbilitySet("katana",
                Ability.of("katana_flash_step", "Flash Step",
                        "Vanish and reappear 7 blocks ahead. Everything you passed is cut a heartbeat later and starts to bleed.",
                        25, 100, KatanaAbilities::flashStep),
                Ability.of("katana_iaido", "Iaido",
                        "Sheathe the blade, then draw it in one wide, lightning-fast cut that deals heavy damage and two stacks of Bleed.",
                        30, 140, KatanaAbilities::iaido),
                SignatureAbilities.windScar(),
                Ability.ultimate("katana_thousand_cuts", "Thousand Cuts",
                        "Blink between up to eight nearby enemies, cutting each one, then return to where you started as "
                                + "every wound opens at once. You can't be hurt while it lasts.",
                        600, KatanaAbilities::thousandCuts));
    }

    static void petals(AbilityContext ctx, Vec3 at, int count, double spread) {
        Fx.burst(ctx.level, ModParticles.PETAL.get(), Fx.PETAL, 1.2F, at, count, spread, spread * 0.6, spread, 0.06);
    }

    static boolean flashStep(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 from = ctx.pos();
        ctx.sound(ModSounds.ABILITY_KATANA_DRAW, 1.0F, 1.2F);
        petals(ctx, from.add(0, 1, 0), 14, 0.4);
        List<LivingEntity> passed = ctx.dash(ctx.look(), 7.0);
        petals(ctx, ctx.pos().add(0, 1, 0), 10, 0.4);
        ctx.later(5, () -> {
            for (LivingEntity e : passed) {
                if (e.isAlive() && ctx.hit(e, 1.1F, c)) {
                    ctx.bleed(e, 120, 1);
                }
                Fx.slash(ctx.level, e.getBoundingBox().getCenter(), ctx.player.getYRot() + 70, 0,
                        ctx.level.random.nextFloat() * 60 - 30, c, 0.9F);
                petals(ctx, e.getBoundingBox().getCenter(), 8, 0.3);
            }
            if (!passed.isEmpty()) {
                ctx.sound(ModSounds.WEAPON_HIT, 1.0F, 1.2F);
            }
        });
        return true;
    }

    static boolean iaido(AbilityContext ctx) {
        int c = ctx.color();
        ctx.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 9, 3, false, false, false));
        ctx.sound(ModSounds.ABILITY_KATANA_DRAW, 0.8F, 0.8F);
        Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.0F, ctx.player.getBoundingBox().getCenter(), 10, 0.5, 0.02);
        ctx.later(8, () -> {
            ctx.slash(0, c, 1.9F, 1.8);
            ctx.later(2, () -> ctx.slash(0, 0xFFFFFF, 1.5F, 2.1));
            for (LivingEntity e : ctx.cone(6.0, 60)) {
                if (ctx.hit(e, 1.8F, c)) {
                    ctx.bleed(e, 140, 2);
                }
                petals(ctx, e.getBoundingBox().getCenter(), 6, 0.3);
            }
            ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 1.0F, 1.4F);
            ctx.sound(ModSounds.ABILITY_KATANA_DRAW, 1.2F, 1.5F);
            ctx.shake(0.4F, 5);
        });
        return true;
    }


    static boolean thousandCuts(AbilityContext ctx) {
        List<LivingEntity> targets = new ArrayList<>(ctx.around(10.0));
        targets.removeIf(e -> !ctx.player.hasLineOfSight(e));
        if (targets.isEmpty()) {
            return false;
        }
        if (targets.size() > 8) {
            targets = targets.subList(0, 8);
        }
        int c = ctx.color();
        Vec3 start = ctx.pos();
        List<LivingEntity> victims = targets;
        int steps = victims.size();
        ctx.data().invulnerableUntil = ctx.now() + steps * 4L + 12;
        ctx.data().noFallUntil = ctx.now() + steps * 4L + 40;
        petals(ctx, start.add(0, 1, 0), 20, 0.5);
        for (int i = 0; i < steps; i++) {
            final LivingEntity target = victims.get(i);
            final int index = i;
            ctx.later(i * 4, () -> {
                if (!target.isAlive()) {
                    return;
                }
                Vec3 before = ctx.pos();
                if (ctx.blinkTo(ctx.behind(target, 1.0), null)) {
                    Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.0F, before.add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.02);
                }
                ctx.hit(target, 1.6F, c);
                ctx.bleed(target, 160, 1);
                Fx.slash(ctx.level, target.getBoundingBox().getCenter(), ctx.level.random.nextFloat() * 360, 0,
                        ctx.level.random.nextFloat() * 120 - 60, c, 1.1F);
                petals(ctx, target.getBoundingBox().getCenter(), 10, 0.4);
                ctx.soundAt(target.position(), ModSounds.ABILITY_KATANA_DRAW, 0.9F, 1.1F + index * 0.07F);
            });
        }
        ctx.later(steps * 4 + 4, () -> {
            ctx.blinkTo(start, null);
            ctx.sound(ModSounds.ABILITY_KATANA_DRAW, 1.3F, 0.7F);
            ctx.later(6, () -> {
                for (LivingEntity e : victims) {
                    if (!e.isAlive()) {
                        continue;
                    }
                    ctx.hit(e, 1.0F, 0xFFFFFF);
                    Vec3 at = e.getBoundingBox().getCenter();
                    Fx.slash(ctx.level, at, ctx.player.getYRot(), 0, 45, c, 1.3F);
                    Fx.slash(ctx.level, at, ctx.player.getYRot(), 0, -45, 0xFFFFFF, 1.3F);
                    petals(ctx, at, 14, 0.5);
                }
                ctx.sound(ModSounds.WEAPON_HIT, 1.3F, 0.8F);
                ctx.sound(ModSounds.ABILITY_PETALS, 1.2F, 0.9F);
                ctx.shake(0.6F, 8);
            });
        });
        return true;
    }

    private KatanaAbilities() {
    }
}

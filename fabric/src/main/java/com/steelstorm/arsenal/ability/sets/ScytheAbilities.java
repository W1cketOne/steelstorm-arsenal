package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.entity.VortexEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Scythe: wide reaping arcs, stolen life, and an eclipse that devours the battlefield. */
public final class ScytheAbilities {
    public static AbilitySet create() {
        return new AbilitySet("scythe",
                Ability.of("scythe_reap", "Reap",
                        "A full circle sweep that cuts everything around you and drains 15% of the damage as health.",
                        35, 160, ScytheAbilities::reap),
                Ability.of("scythe_soul_harvest", "Soul Harvest",
                        "Rip at the souls of every enemy within 9 blocks, dragging them toward you and Marking them for "
                                + "6 seconds. Marked enemies glow, take 15% more damage and heal you when you hit them.",
                        30, 180, ScytheAbilities::soulHarvest),
                Ability.of("scythe_deaths_crescent", "Death's Crescent",
                        "Throw a spinning crescent of death that flies out 12 blocks and comes back, cutting everything both ways.",
                        30, 140, ScytheAbilities::deathsCrescent).withCharges(2),
                SignatureAbilities.deathMark());
    }

    /** Wisps of stolen life drifting from the victim to the caster. */
    static void drainFx(AbilityContext ctx, LivingEntity from, int color) {
        Vec3 a = from.getBoundingBox().getCenter();
        Vec3 b = ctx.player.getBoundingBox().getCenter();
        Vec3 v = b.subtract(a).scale(0.09);
        for (int i = 0; i < 4; i++) {
            Fx.shoot(ctx.level, ModParticles.GLOW.get(), color, 1.3F, a.add(v.scale(i * 1.5)), v);
        }
    }

    static boolean reap(AbilityContext ctx) {
        int c = ctx.color();
        float yaw = ctx.player.getYRot();
        Vec3 center = ctx.pos().add(0, 1.0, 0);
        ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw).scale(1.6)), yaw, 0, c, 1.6F);
        ctx.later(2, () -> ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw + 180).scale(1.6)), yaw + 180, 0, c, 1.6F));
        float healed = 0;
        for (LivingEntity e : ctx.around(4.5)) {
            float before = e.getHealth();
            ctx.hit(e, 1.2F, c);
            healed += Math.max(0, before - e.getHealth());
            drainFx(ctx, e, c);
        }
        if (healed > 0) {
            ctx.player.heal(healed * 0.15F);
        }
        ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 1.1F, 0.8F);
        ctx.sound(ModSounds.ABILITY_VOID, 0.6F, 1.6F);
        return true;
    }

    static boolean soulHarvest(AbilityContext ctx) {
        List<LivingEntity> targets = ctx.around(9.0);
        if (targets.isEmpty()) {
            return false;
        }
        int c = ctx.color();
        Vec3 center = ctx.pos();
        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(ModEffects.MARKED, 120, 0));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
            ctx.pull(e, center, 1.3);
            Vec3 a = e.getBoundingBox().getCenter();
            Vec3 b = ctx.player.getBoundingBox().getCenter();
            for (int i = 0; i <= 10; i++) {
                Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.0F, a.lerp(b, i / 10.0), 1, 0.05, 0.0);
            }
            Fx.burst(ctx.level, ModParticles.RUNE.get(), c, 1.2F, a, 4, 0.3, 0.02);
        }
        Fx.ring(ctx.level, center, c, 9.0F);
        ctx.sound(ModSounds.ABILITY_VOID_HUM, 1.2F, 1.2F);
        ctx.sound(ModSounds.ABILITY_VOID, 0.8F, 1.4F);
        return true;
    }

    static boolean deathsCrescent(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 start = ctx.eye().add(0, -0.4, 0).add(ctx.look().scale(1.0));
        SlashWaveEntity.fire(ctx.player, start, ctx.look(), 1.0F, 12, 2.6F, 12, ctx.dmg(1.2F), c, true,
                e -> drainFx(ctx, e, c));
        ctx.slash(5, c, 1.3F, 1.5);
        ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 1.1F, 0.75F);
        return true;
    }


    private ScytheAbilities() {
    }
}

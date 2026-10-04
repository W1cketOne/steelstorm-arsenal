package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.entity.ChakramEntity;
import com.steelstorm.arsenal.entity.OrbitBladesEntity;
import com.steelstorm.arsenal.entity.ThrowingKnifeEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Abilities of the thrown weapons: the chakram and the throwing knife. */
public final class ThrownAbilities {
    public static AbilitySet chakram() {
        return new AbilitySet("chakram",
                Ability.of("chakram_sawblade", "Sawblade",
                        "Throw a spectral chakram that stops where it lands and grinds everything around it for 3 seconds before flying back.",
                        20, 160, ThrownAbilities::sawblade),
                Ability.of("chakram_twin_throw", "Twin Throw",
                        "Throw two spectral chakrams at once, angled apart. Each one bounces between enemies.",
                        25, 120, ThrownAbilities::twinThrow),
                Ability.of("chakram_guard_ring", "Guard Ring",
                        "Four spectral chakrams circle you for 5 seconds, cutting anything close and knocking arrows out of the air.",
                        25, 300, ThrownAbilities::guardRing),
                Ability.ultimate("chakram_blade_tempest", "Blade Tempest",
                        "Eight chakrams orbit you in a wide, deadly ring for 7 seconds, shredding everything they touch "
                                + "and blocking projectiles.",
                        600, ThrownAbilities::bladeTempest));
    }

    public static AbilitySet throwingKnife() {
        return new AbilitySet("throwing_knife",
                Ability.of("throwing_knife_volley", "Volley",
                        "Throw a fan of five spectral knives at once.",
                        15, 80, ThrownAbilities::volley),
                Ability.of("throwing_knife_blink", "Blink Knife",
                        "Throw a knife of shadow. Wherever it lands, you appear; if it hits an enemy, you appear behind them.",
                        20, 160, ThrownAbilities::blinkKnife),
                Ability.of("throwing_knife_venom", "Venom Coat",
                        "Coat your knives in venom: your next 8 throws poison and bleed whatever they hit.",
                        20, 400, ThrownAbilities::venomCoat),
                Ability.ultimate("throwing_knife_knife_storm", "Knife Storm",
                        "Forty spectral knives rain down on the spot you look at over a second and a half.",
                        600, ThrownAbilities::knifeStorm));
    }

    static boolean sawblade(AbilityContext ctx) {
        ChakramEntity.spectral(ctx.player, ctx.look(), ctx.dmg(0.4F), 60);
        ctx.sound(ModSounds.WEAPON_THROW, 1.0F, 0.9F);
        ctx.sound(ModSounds.WEAPON_CHAKRAM_SPIN, 1.0F, 1.0F);
        return true;
    }

    static boolean twinThrow(AbilityContext ctx) {
        for (int side = -1; side <= 1; side += 2) {
            Vec3 dir = Vec3.directionFromRotation(ctx.player.getXRot(), ctx.player.getYRot() + side * 20);
            ChakramEntity.spectral(ctx.player, dir, ctx.dmg(0.9F), 0);
        }
        ctx.sound(ModSounds.WEAPON_THROW, 1.0F, 1.1F);
        ctx.later(2, () -> ctx.sound(ModSounds.WEAPON_THROW, 0.9F, 1.3F));
        return true;
    }

    static boolean guardRing(AbilityContext ctx) {
        OrbitBladesEntity.spawn(ctx.player, ctx.weaponCopy(), 4, 2.2F, 100, 14.0F, ctx.dmg(0.4F), true, ctx.color());
        Fx.ring(ctx.level, ctx.pos(), ctx.color(), 2.5F);
        ctx.sound(ModSounds.WEAPON_CHAKRAM_SPIN, 1.2F, 0.9F);
        return true;
    }

    static boolean bladeTempest(AbilityContext ctx) {
        OrbitBladesEntity.spawn(ctx.player, ctx.weaponCopy(), 8, 4.0F, 140, 17.0F, ctx.dmg(0.6F), true, ctx.color());
        ctx.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 0, false, true, true));
        Fx.ring(ctx.level, ctx.pos(), ctx.color(), 4.5F);
        ctx.sound(ModSounds.WEAPON_CHAKRAM_SPIN, 1.4F, 0.7F);
        ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 1.0F, 1.2F);
        return true;
    }

    /** Venom Coat charges: the next knife ability or throw uses one. */
    static byte knifeMode(AbilityContext ctx) {
        if (ctx.data().venomKnives > 0) {
            ctx.data().venomKnives--;
            return ThrowingKnifeEntity.VENOM;
        }
        return ThrowingKnifeEntity.SPECTRAL;
    }

    static boolean volley(AbilityContext ctx) {
        byte mode = knifeMode(ctx);
        Vec3 from = ctx.eye().add(0, -0.15, 0);
        for (int i = -2; i <= 2; i++) {
            Vec3 dir = Vec3.directionFromRotation(ctx.player.getXRot(), ctx.player.getYRot() + i * 8);
            ThrowingKnifeEntity.spectral(ctx.player, from.add(dir.scale(0.5)), dir.scale(2.3), ctx.dmg(0.8F), mode);
        }
        ctx.sound(ModSounds.WEAPON_THROW, 1.0F, 1.3F);
        return true;
    }

    static boolean blinkKnife(AbilityContext ctx) {
        Vec3 dir = ctx.look();
        ThrowingKnifeEntity.spectral(ctx.player, ctx.eye().add(dir.scale(0.5)), dir.scale(2.5), ctx.dmg(1.0F), ThrowingKnifeEntity.BLINK);
        ctx.sound(ModSounds.WEAPON_THROW, 1.0F, 0.9F);
        ctx.sound(ModSounds.ABILITY_SMOKE, 0.6F, 1.6F);
        return true;
    }

    static boolean venomCoat(AbilityContext ctx) {
        ctx.data().venomKnives = 8;
        Vec3 c = ctx.player.getBoundingBox().getCenter();
        for (int i = 0; i < 20; i++) {
            double a = i * Math.PI / 10;
            Fx.shoot(ctx.level, ModParticles.GLOW.get(), ThrowingKnifeEntity.VENOM_COLOR, 1.3F,
                    c.add(Math.cos(a) * 0.8, (i % 5) * 0.2 - 0.4, Math.sin(a) * 0.8), new Vec3(0, 0.06, 0));
        }
        ctx.sound(ModSounds.ABILITY_BLOOD, 0.8F, 1.6F);
        return true;
    }

    static boolean knifeStorm(AbilityContext ctx) {
        Vec3 target = ctx.aimPoint(20);
        int c = ctx.color();
        Fx.ring(ctx.level, target, c, 5.5F);
        Fx.burst(ctx.level, ModParticles.RUNE.get(), c, 1.2F, target.add(0, 0.2, 0), 16, 2.5, 0.05, 2.5, 0.01);
        float damage = ctx.dmg(0.7F);
        for (int i = 0; i < 40; i++) {
            final int n = i;
            ctx.later(4 + i * 3 / 4, () -> {
                double a = ctx.level.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(ctx.level.random.nextDouble()) * 5.0;
                Vec3 land = target.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Vec3 from = land.add(-1.5 + ctx.level.random.nextDouble() * 3, 14, -1.5 + ctx.level.random.nextDouble() * 3);
                Vec3 v = land.subtract(from).normalize().scale(2.6);
                ThrowingKnifeEntity.spectral(ctx.player, from, v, damage, ThrowingKnifeEntity.SPECTRAL);
                if (n % 4 == 0) {
                    ctx.soundAt(land, ModSounds.WEAPON_THROW, 0.7F, 1.2F + ctx.level.random.nextFloat() * 0.4F);
                }
            });
        }
        return true;
    }

    private ThrownAbilities() {
    }
}

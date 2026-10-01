package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Battleaxe: a berserker's whirling, leaping, roaring kit. */
public final class BattleaxeAbilities {
    public static AbilitySet create() {
        return new AbilitySet("battleaxe",
                Ability.of("battleaxe_whirlwind", "Whirlwind",
                        "Spin like a whirlwind for over a second, hitting everything around you six times. You can keep moving.",
                        35, 160, BattleaxeAbilities::whirlwind),
                Ability.of("battleaxe_cleaving_leap", "Cleaving Leap",
                        "Leap to the spot you look at (up to 8 blocks) and bury the axe in the ground, breaking armour "
                                + "and staggering everything you land near.",
                        30, 140, BattleaxeAbilities::cleavingLeap),
                Ability.of("battleaxe_war_cry", "War Cry",
                        "A terrifying roar. Enemies within 8 blocks are weakened, slowed and staggered; you gain "
                                + "Strength and Speed for 6 seconds.",
                        25, 300, BattleaxeAbilities::warCry),
                Ability.ultimate("battleaxe_berserker_rage", "Berserker Rage",
                        "Fly into a rage for 8 seconds: swing 60% faster, hit 4 harder, heal from every blow, ignore "
                                + "Stagger, and every hit bursts into a wave of blood.",
                        900, BattleaxeAbilities::berserkerRage));
    }

    static boolean whirlwind(AbilityContext ctx) {
        int c = ctx.color();
        for (int i = 0; i < 6; i++) {
            final int pulse = i;
            ctx.later(i * 4, () -> {
                Vec3 center = ctx.pos().add(0, 1.0, 0);
                float yaw = ctx.player.getYRot() + pulse * 120;
                ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw).scale(1.4)), yaw + 90, 0, c, 1.2F);
                ctx.slashAt(center.add(Vec3.directionFromRotation(0, yaw + 180).scale(1.4)), yaw + 270, 0, c, 1.2F);
                for (LivingEntity e : ctx.around(3.3)) {
                    if (ctx.hit(e, 0.6F, c)) {
                        ctx.pull(e, ctx.pos(), 0.2);
                    }
                }
                Fx.burst(ctx.level, ModParticles.SMOKE.get(), 0x7A6E62, 1.2F, ctx.pos().add(0, 0.2, 0), 4, 1.2, 0.05, 1.2, 0.03);
                ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 0.8F, 1.0F + pulse * 0.06F);
            });
        }
        return true;
    }

    static boolean cleavingLeap(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 target = ctx.aimPoint(8.0);
        Vec3 to = target.subtract(ctx.pos());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        double dist = Math.min(8.0, flat.length());
        Vec3 dir = flat.lengthSqr() < 1e-4 ? ctx.flatLook() : flat.normalize();
        ctx.sound(ModSounds.ABILITY_DASH, 1.0F, 0.7F);
        ctx.leap(dir.scale(0.25 + dist * 0.11).add(0, 0.85, 0), land -> {
            Vec3 at = land.pos();
            land.slash(95, c, 1.3F, 1.2);
            for (LivingEntity e : land.around(at, 2.8)) {
                if (land.hit(e, 1.8F, c)) {
                    land.armorBreak(e, 120);
                    land.stagger(e, 40);
                }
            }
            Shockwaves.crater(land.level, at.add(land.flatLook().scale(1.0)), 2.0F, 1.0F);
            land.shockwave(at, 3.0F, 0.8F, 0, 0.3, c, null);
            Fx.impact(land.level, at.add(0, 0.4, 0), c, 1.8F);
            land.sound(ModSounds.ABILITY_SHOCKWAVE, 1.1F, 1.1F);
            land.sound(ModSounds.WEAPON_HIT_METAL, 0.9F, 0.6F);
            land.shakeNearby(at, 14, 0.8F, 8);
        });
        return true;
    }

    static boolean warCry(AbilityContext ctx) {
        int c = ctx.color();
        List<LivingEntity> enemies = ctx.around(8.0);
        for (LivingEntity e : enemies) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0));
            ctx.stagger(e, 30);
            ctx.knockFrom(e, ctx.pos(), 0.5, 0.15);
        }
        ctx.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
        ctx.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        Fx.ring(ctx.level, ctx.pos(), c, 8.0F);
        ctx.later(4, () -> Fx.ring(ctx.level, ctx.pos(), 0xFFB0A0, 6.0F));
        Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.5F, ctx.eye(), 16, 0.6, 0.15);
        ctx.sound(ModSounds.ABILITY_WAR_CRY, 1.4F, 1.0F);
        ctx.shakeNearby(ctx.pos(), 14, 0.6F, 10);
        return true;
    }

    static boolean berserkerRage(AbilityContext ctx) {
        ctx.player.addEffect(new MobEffectInstance(ModEffects.BERSERK, 160, 0, false, true, true));
        ctx.player.removeEffect(ModEffects.STAGGER);
        ctx.player.heal(4.0F);
        Vec3 center = ctx.player.getBoundingBox().getCenter();
        Fx.burst(ctx.level, ModParticles.BLOOD.get(), 0xFFFFFF, 1.4F, center, 30, 0.6, 0.3);
        Fx.burst(ctx.level, ModParticles.GLOW.get(), 0xFF3030, 1.8F, center, 24, 0.7, 0.1);
        Fx.ring(ctx.level, ctx.pos(), 0xD0182C, 6.0F);
        ctx.sound(ModSounds.ABILITY_BERSERK, 1.4F, 1.0F);
        ctx.sound(ModSounds.ABILITY_WAR_CRY, 1.0F, 0.7F);
        return true;
    }

    private BattleaxeAbilities() {
    }
}

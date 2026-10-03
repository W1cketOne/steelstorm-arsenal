package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatData;
import com.steelstorm.arsenal.entity.EarthChunkEntity;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.entity.SpectralWeaponEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Abilities that each do something no other ability does: absorbing damage, planted zones,
 * imprisoning, grappling, doom marks, detonating bleeds, lifting tornadoes.
 */
public final class SignatureAbilities {
    private SignatureAbilities() {
    }

    // ------------------------------------------------------------------ greatsword

    public static Ability titansGuard() {
        return Ability.of("greatsword_titans_guard", "Titan's Guard",
                "Plant your feet and raise the greatsword like a wall for 3 seconds: you take a quarter of the damage, and everything "
                        + "you soak up is hurled back in a blast in front of you when the guard drops.",
                30, 220, SignatureAbilities::titansGuardRun);
    }

    static boolean titansGuardRun(AbilityContext ctx) {
        int c = ctx.color();
        CombatData data = ctx.data();
        data.titanGuardUntil = ctx.now() + 60;
        data.titanStored = 0;
        ctx.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, false));
        Fx.gyro(ctx.level, ctx.pos(), ctx.player, c, Fx.WHITE, 1.6F, 1.0F, 60);
        ctx.sound(ModSounds.ABILITY_CAST, 1.0F, 0.6F);
        ctx.later(60, () -> {
            if (!ctx.alive()) {
                return;
            }
            float stored = data.titanStored;
            data.titanGuardUntil = 0;
            data.titanStored = 0;
            float bonus = Math.min(stored * 2.0F, ctx.dmg(4.0F));
            for (LivingEntity e : ctx.cone(6.0, 60)) {
                if (ctx.hit(e, 1.0F, c)) {
                    e.hurt(ctx.player.damageSources().playerAttack(ctx.player), bonus);
                    ctx.knockFrom(e, ctx.pos(), 1.4, 0.5);
                }
            }
            Vec3 front = ctx.pos().add(ctx.flatLook().scale(2.5)).add(0, 1, 0);
            Fx.sunburst(ctx.level, front, c, Fx.WHITE, 3.0F + stored * 0.15F, 14, 14);
            Fx.halo(ctx.level, ctx.pos(), c, Fx.WHITE, 6.0F, 16);
            Fx.sparkles(ctx.level, front, c, 30, 1.5);
            ctx.slash(0, c, 2.0F, 2.0);
            ctx.sound(ModSounds.ABILITY_SHOCKWAVE, 1.3F, 1.1F);
            ctx.shake(0.8F + Math.min(1.0F, stored / 20.0F), 10);
        });
        return true;
    }

    public static Ability swordSanctum() {
        return Ability.of("greatsword_sword_sanctum", "Sword Sanctum",
                "Hurl a giant spectral greatsword into the ground ahead. For 5 seconds it draws enemies in, slows them and burns them "
                        + "every second, then it explodes.",
                35, 260, SignatureAbilities::swordSanctumRun);
    }

    static boolean swordSanctumRun(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 at = ctx.aimPoint(10);
        SpectralWeaponEntity.drop(ctx.level, ctx.weaponCopy(), at, 3.2F, c, 4, 10, 6, 100, null);
        ctx.later(6, () -> {
            Fx.circle(ctx.level, at, null, c, Fx.WHITE, 5.0F, 96);
            Fx.halo(ctx.level, at, c, Fx.WHITE, 5.5F, 14);
            ctx.soundAt(at, ModSounds.ABILITY_GROUND_CRACK, 1.2F, 0.8F);
            ctx.shakeNearby(at, 12, 0.6F, 6);
        });
        for (int i = 1; i <= 5; i++) {
            ctx.later(6 + i * 20, () -> {
                for (LivingEntity e : ctx.around(at, 5.5)) {
                    if (ctx.hit(e, 0.5F, c)) {
                        ctx.slow(e, 30, 2);
                        ctx.pull(e, at, 0.35);
                    }
                }
                Fx.orbs(ctx.level, at.add(0, 0.5, 0), c, 10, 3.0);
                ctx.soundAt(at, ModSounds.ABILITY_ZAP, 0.6F, 0.6F);
            });
        }
        ctx.later(108, () -> {
            for (LivingEntity e : ctx.around(at, 6.0)) {
                if (ctx.hit(e, 2.0F, c)) {
                    ctx.launch(e, 0.6);
                }
            }
            Fx.pillar(ctx.level, at, c, Fx.WHITE, 1.4F, 12, 16);
            Fx.halo(ctx.level, at, Fx.WHITE, c, 8.0F, 18);
            Fx.impact(ctx.level, at.add(0, 1, 0), c, 3.0F);
            ctx.soundAt(at, ModSounds.ABILITY_SHOCKWAVE, 1.5F, 0.8F);
            ctx.shakeNearby(at, 18, 1.0F, 10);
        });
        return true;
    }

    // ------------------------------------------------------------------ warhammer

    public static Ability stonePrison() {
        return Ability.of("warhammer_stone_prison", "Stone Prison",
                "Slam the ground and seal the enemy you look at (up to 14 blocks) in a shell of rock. It can't move for 3 seconds, "
                        + "then the prison shatters for heavy damage.",
                30, 240, SignatureAbilities::stonePrisonRun);
    }

    static boolean stonePrisonRun(AbilityContext ctx) {
        LivingEntity target = ctx.aimed(14);
        if (target == null) {
            return false;
        }
        int c = ctx.color();
        ctx.sound(ModSounds.ABILITY_GROUND_CRACK, 1.2F, 0.7F);
        Fx.ring(ctx.level, ctx.pos(), c, 2.5F);
        ctx.fissure(ctx.pos().add(ctx.flatLook()), target.position().subtract(ctx.pos()).multiply(1, 0, 1).normalize(),
                (int) Math.max(2, ctx.pos().distanceTo(target.position()) - 1), 0.6, 0.0F, 0.0, c, null);
        ctx.later(6, () -> {
            if (!target.isAlive()) {
                return;
            }
            EarthChunkEntity.shell(ctx.level, target, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 60);
            ctx.freeze(target, 60);
            Fx.circle(ctx.level, target.position(), target, c, Fx.EARTH, 2.0F, 60);
            Shockwaves.dust(ctx.level, target.position(), 1.5F);
            ctx.soundAt(target.position(), ModSounds.ABILITY_GROUND_CRACK, 1.2F, 0.8F);
        });
        ctx.later(66, () -> {
            if (!target.isAlive()) {
                return;
            }
            Vec3 at = target.position();
            ctx.hit(target, 3.0F, c);
            ctx.launch(target, 0.5);
            for (LivingEntity e : ctx.around(at, 3.5)) {
                if (e != target) {
                    ctx.hit(e, 1.0F, c);
                }
            }
            Shockwaves.pop(ctx.level, net.minecraft.core.BlockPos.containing(at).below(), 1.2F, 16);
            Fx.halo(ctx.level, at, c, Fx.EARTH, 5.0F, 14);
            Fx.impact(ctx.level, at.add(0, 1, 0), c, 2.4F);
            ctx.soundAt(at, ModSounds.ABILITY_SHOCKWAVE, 1.3F, 0.9F);
            ctx.shakeNearby(at, 14, 0.8F, 8);
        });
        return true;
    }

    // ------------------------------------------------------------------ katana

    public static Ability windScar() {
        return Ability.of("katana_wind_scar", "Wind Scar",
                "Three cuts so fast the air splits: two crossing blades of wind and a third straight down the middle, all meeting "
                        + "where you aim.",
                25, 140, SignatureAbilities::windScarRun);
    }

    static boolean windScarRun(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 look = ctx.look();
        Vec3 start = ctx.eye().add(0, -0.4, 0);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            ctx.later(i * 3, () -> {
                float yaw = k == 2 ? 0 : (k == 0 ? -18 : 18);
                Vec3 dir = look.yRot(yaw * Mth.DEG_TO_RAD);
                Vec3 from = start.add(look.yRot((k == 0 ? 90 : -90) * Mth.DEG_TO_RAD).scale(k == 2 ? 0 : 1.6));
                SlashWaveEntity.fire(ctx.player, from, dir, 1.5F, 14, 1.6F, k == 0 ? 45 : k == 1 ? -45 : 90, ctx.dmg(0.9F), c, false,
                        e -> ctx.bleed(e, 60, 1));
                ctx.slash(k == 0 ? 45 : k == 1 ? -45 : 90, c, 1.2F, 1.5);
                ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 0.9F, 1.3F + k * 0.1F);
            });
        }
        Fx.sparkles(ctx.level, ctx.eye().add(look.scale(2)), Fx.PETAL, 14, 0.8);
        return true;
    }

    // ------------------------------------------------------------------ battleaxe

    public static Ability chainHook() {
        return Ability.of("battleaxe_chain_hook", "Chain Hook",
                "Hurl a hooked chain at the enemy you look at (up to 16 blocks) and drag it to your feet, staggered and with its "
                        + "armour broken.",
                25, 180, SignatureAbilities::chainHookRun).withCharges(2);
    }

    static boolean chainHookRun(AbilityContext ctx) {
        LivingEntity target = ctx.aimed(16);
        if (target == null) {
            return false;
        }
        int c = ctx.color();
        Vec3 from = ctx.eye().add(0, -0.3, 0);
        Vec3 to = target.getBoundingBox().getCenter();
        double length = from.distanceTo(to);
        for (double d = 0; d < length; d += 0.5) {
            Vec3 p = from.add(to.subtract(from).scale(d / length));
            ctx.level.sendParticles(ModParticles.SPARK.get().with(0xB8BFC8, 0.7F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 1.0F, 1.4F);
        ctx.soundAt(to, ModSounds.ABILITY_ZAP, 0.8F, 0.5F);
        ctx.later(3, () -> {
            if (!target.isAlive()) {
                return;
            }
            Vec3 pullTo = ctx.pos().add(ctx.flatLook().scale(1.5));
            Vec3 v = pullTo.subtract(target.position());
            target.setDeltaMovement(v.x * 0.32, 0.35 + Math.max(0, v.y) * 0.1, v.z * 0.32);
            target.hurtMarked = true;
            ctx.hit(target, 0.8F, c);
            ctx.stagger(target, 40);
            ctx.armorBreak(target, 120);
            Fx.sparkles(ctx.level, target.position().add(0, 1, 0), c, 12, 0.5);
        });
        ctx.later(10, () -> {
            if (target.isAlive() && target.distanceTo(ctx.player) < 4) {
                ctx.hit(target, 0.8F, c);
                Fx.impact(ctx.level, target.position().add(0, 1, 0), c, 1.6F);
                ctx.shake(0.5F, 5);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ scythe

    public static Ability deathMark() {
        return Ability.ultimate("scythe_death_mark", "Death Mark",
                "Doom every enemy within 12 blocks. In 3 seconds the doom falls: each one takes heavy damage, plus half of all the "
                        + "damage dealt to it while it was marked.",
                640, SignatureAbilities::deathMarkRun);
    }

    static boolean deathMarkRun(AbilityContext ctx) {
        int c = ctx.color();
        List<LivingEntity> marked = new ArrayList<>(ctx.around(12));
        if (marked.isEmpty()) {
            return false;
        }
        List<Float> health = new ArrayList<>();
        for (LivingEntity e : marked) {
            health.add(e.getHealth());
            e.addEffect(new MobEffectInstance(ModEffects.MARKED, 70, 0));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 70, 0, false, false));
            Fx.orbit(ctx.level, e.position(), e, Fx.VOID, 0xFF4D6D, 5, 0.6F, e.getBbHeight() + 0.3F, 60);
        }
        Fx.circle(ctx.level, ctx.pos(), ctx.player, Fx.VOID, c, 12.0F, 60);
        ctx.sound(ModSounds.ABILITY_CAST, 1.3F, 0.5F);
        ctx.later(60, () -> {
            for (int i = 0; i < marked.size(); i++) {
                LivingEntity e = marked.get(i);
                if (!e.isAlive()) {
                    continue;
                }
                float lost = Math.max(0, health.get(i) - e.getHealth());
                ctx.hit(e, 2.5F, c);
                if (lost > 0 && e.isAlive()) {
                    e.hurt(ctx.player.damageSources().magic(), lost * 0.5F);
                }
                Vec3 at = e.position();
                Fx.pillar(ctx.level, at, Fx.VOID, 0xFF4D6D, 0.7F, 6, 12);
                Fx.sunburst(ctx.level, at.add(0, 1, 0), Fx.VOID, 0xFF4D6D, 2.5F, 10, 10);
                ctx.soundAt(at, ModSounds.ABILITY_VOID, 1.0F, 1.2F);
            }
            ctx.shake(1.0F, 10);
        });
        return true;
    }

    // ------------------------------------------------------------------ legendaries

    public static Ability hemorrhage() {
        return Ability.ultimate("bloodfang_hemorrhage", "Hemorrhage",
                "Every Bleed on every enemy within 10 blocks bursts at once. Each stack deals heavy damage and heals you.",
                600, SignatureAbilities::hemorrhageRun);
    }

    static boolean hemorrhageRun(AbilityContext ctx) {
        int burst = 0;
        for (LivingEntity e : ctx.around(10)) {
            MobEffectInstance bleed = e.getEffect(ModEffects.BLEED);
            int stacks = bleed == null ? 1 : bleed.getAmplifier() + 2;
            e.removeEffect(ModEffects.BLEED);
            ctx.hit(e, 0.7F * stacks, Fx.BLOOD);
            ctx.player.heal(1.5F * stacks);
            Vec3 at = e.getBoundingBox().getCenter();
            Fx.sunburst(ctx.level, at, Fx.BLOOD, 0xFFB3B9, 1.5F + stacks * 0.4F, 10, 10);
            ctx.level.sendParticles(ModParticles.BLOOD.get().with(Fx.BLOOD, 1.4F), at.x, at.y, at.z, 10 + stacks * 6, 0.4, 0.5, 0.4, 0.25);
            burst++;
        }
        if (burst == 0) {
            return false;
        }
        Fx.halo(ctx.level, ctx.pos(), Fx.BLOOD, 0xFFB3B9, 10.0F, 18);
        Fx.orbit(ctx.level, ctx.pos(), ctx.player, Fx.BLOOD, 0xFFB3B9, 8, 1.4F, 1.0F, 30);
        ctx.sound(ModSounds.ABILITY_BLOOD, 1.2F, 0.8F);
        ctx.shake(0.8F, 8);
        return true;
    }

    public static Ability tectonicSpiral() {
        return Ability.ultimate("earthshaker_tectonic_spiral", "Tectonic Spiral",
                "Strike the ground and it splits open in eight fissures that spiral out one after another, throwing everything "
                        + "they touch into the air.",
                600, SignatureAbilities::tectonicSpiralRun);
    }

    static boolean tectonicSpiralRun(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 center = ctx.pos();
        float baseYaw = ctx.player.getYRot();
        ctx.sound(ModSounds.ABILITY_RUMBLE, 1.4F, 0.6F);
        Fx.circle(ctx.level, center, null, c, Fx.EARTH, 4.0F, 40);
        for (int i = 0; i < 8; i++) {
            final int k = i;
            ctx.later(4 + i * 3, () -> {
                Vec3 dir = Vec3.directionFromRotation(0, baseYaw + k * 45 + k * 6);
                ctx.fissure(center.add(dir.scale(1.2)), dir, 12, 1.0, 1.5F, 0.9, c, e -> ctx.stagger(e, 30));
                ctx.soundAt(center.add(dir.scale(6)), ModSounds.ABILITY_GROUND_CRACK, 1.0F, 0.8F + k * 0.04F);
            });
        }
        ctx.later(30, () -> {
            ctx.shockwave(center, 6.0F, 0.9F, 1.5F, 0.6, c, null);
            Fx.pillar(ctx.level, center, c, Fx.WHITE, 1.2F, 10, 16);
            ctx.shakeNearby(center, 30, 1.5F, 16);
        });
        return true;
    }

    public static Ability updraft() {
        return Ability.ultimate("skypiercer_updraft", "Updraft",
                "Spin up a roaring tornado where you aim. For 4 seconds it sucks enemies in and hurls them skyward, where Skypiercer "
                        + "hits them twice as hard.",
                600, SignatureAbilities::updraftRun);
    }

    static boolean updraftRun(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 at = ctx.aimPoint(18);
        Fx.gyro(ctx.level, at, null, c, Fx.WHITE, 3.0F, 2.0F, 84);
        Fx.circle(ctx.level, at, null, c, Fx.WHITE, 4.5F, 84);
        ctx.soundAt(at, ModSounds.ABILITY_DASH, 1.4F, 0.6F);
        for (int i = 0; i < 20; i++) {
            final int k = i;
            ctx.later(i * 4, () -> {
                for (LivingEntity e : ctx.around(at, 5.0)) {
                    Vec3 in = at.subtract(e.position()).multiply(1, 0, 1).scale(0.12);
                    e.setDeltaMovement(in.x, Math.max(e.getDeltaMovement().y, 0.42), in.z);
                    e.hurtMarked = true;
                    e.resetFallDistance();
                    if (k % 3 == 0) {
                        ctx.hit(e, 0.35F, c);
                    }
                }
                for (int s = 0; s < 6; s++) {
                    double a = (k * 0.9 + s * Math.PI / 3);
                    double h = s * 1.2;
                    double r = 0.8 + h * 0.35;
                    Fx.shoot(ctx.level, ModParticles.GLOW.get(), c, 1.4F, at.add(Math.cos(a) * r, h, Math.sin(a) * r),
                            new Vec3(-Math.sin(a) * 0.3, 0.2, Math.cos(a) * 0.3));
                }
                if (k % 5 == 0) {
                    ctx.soundAt(at, ModSounds.ABILITY_DASH, 0.8F, 0.8F + k * 0.02F);
                }
            });
        }
        return true;
    }
}

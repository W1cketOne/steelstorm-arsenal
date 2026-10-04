package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilityManager;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatData;
import com.steelstorm.arsenal.entity.EarthChunkEntity;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.entity.SpectralWeaponEntity;
import com.steelstorm.arsenal.entity.VortexEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem.Legendary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Each legendary weapon replaces its type's ultimate with one of its own. */
public final class LegendaryUltimates {
    public static Ability of(Legendary legendary) {
        return switch (legendary) {
            case TEMPEST_EDGE -> Ability.ultimate("tempest_edge_thunder_verdict", "Thunder Verdict",
                    "Call down eight bolts of lightning, one after another, on the enemies around you.",
                    600, LegendaryUltimates::thunderVerdict);
            case RIMECLEAVER -> Ability.ultimate("rimecleaver_absolute_zero", "Absolute Zero",
                    "Freeze every enemy within 9 blocks solid in a shell of ice. Two seconds later the ice shatters "
                            + "for heavy damage.",
                    600, LegendaryUltimates::absoluteZero);
            case VOIDREAVER -> Ability.ultimate("voidreaver_event_horizon", "Event Horizon",
                    "Tear open a black hole 10 blocks ahead. For 5 seconds it swallows everything nearby, then "
                            + "collapses in a devastating blast.",
                    700, LegendaryUltimates::eventHorizon);
            case EARTHSHAKER -> SignatureAbilities.tectonicSpiral();
            case BLOODFANG -> SignatureAbilities.hemorrhage();
            case SKYPIERCER -> SignatureAbilities.updraft();
            case MOONVEIL -> Ability.ultimate("moonveil_moonfall", "Moonfall",
                    "Moonlight pins every enemy within 12 blocks in place, then three great crescents of moonlight "
                            + "sweep out in front of you.",
                    600, LegendaryUltimates::moonfall);
            case KINGSBANE -> Ability.ultimate("kingsbane_regicide", "Regicide",
                    "Leap onto the enemy you look at and bring the axe down for 4x damage, or 8x if it is below 30% "
                            + "health. A kill refunds half of your ultimate.",
                    600, LegendaryUltimates::regicide);
        };
    }

    static boolean thunderVerdict(AbilityContext ctx) {
        List<LivingEntity> enemies = new ArrayList<>(ctx.around(12.0));
        for (int i = 0; i < 8; i++) {
            final int n = i;
            ctx.later(i * 4, () -> {
                LivingEntity target = enemies.isEmpty() ? null : enemies.get(n % enemies.size());
                Vec3 at;
                if (target != null && target.isAlive()) {
                    at = target.position();
                } else {
                    double a = ctx.level.random.nextDouble() * Math.PI * 2;
                    double r = 2 + ctx.level.random.nextDouble() * 6;
                    at = ctx.pos().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    BlockPos g = Shockwaves.ground(ctx.level, at.x, at.y, at.z);
                    if (g != null) {
                        at = new Vec3(at.x, g.getY() + 1, at.z);
                    }
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(ctx.level);
                if (bolt != null) {
                    bolt.moveTo(at.x, at.y, at.z);
                    bolt.setVisualOnly(true);
                    ctx.level.addFreshEntity(bolt);
                }
                for (LivingEntity e : ctx.around(at, 2.2)) {
                    ctx.hit(e, 2.5F, Fx.LIGHTNING);
                    ctx.stagger(e, 20);
                }
                Fx.ring(ctx.level, at, Fx.STORM, 3.0F);
                Fx.sparks(ctx.level, Fx.LIGHTNING, at.add(0, 0.5, 0), 16, 0.8);
                ctx.soundAt(at, ModSounds.ABILITY_THUNDER, 1.4F, 0.9F + n * 0.04F);
                ctx.shakeNearby(at, 16, 0.6F, 6);
            });
        }
        ctx.sound(ModSounds.ABILITY_ZAP, 1.2F, 0.8F);
        Fx.burst(ctx.level, ModParticles.GLOW.get(), Fx.STORM, 1.6F, ctx.player.getBoundingBox().getCenter(), 20, 0.6, 0.1);
        return true;
    }

    static boolean absoluteZero(AbilityContext ctx) {
        List<LivingEntity> enemies = ctx.around(9.0);
        Fx.ring(ctx.level, ctx.pos(), Fx.FROST, 9.0F);
        ctx.shockwave(ctx.pos(), 9.0F, 1.2F, 0, 0, Fx.FROST, null);
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI / 20;
            Fx.shoot(ctx.level, ModParticles.FROST.get(), Fx.FROST, 1.6F, ctx.pos().add(0, 0.4, 0),
                    new Vec3(Math.cos(a) * 0.7, 0.05, Math.sin(a) * 0.7));
        }
        ctx.sound(ModSounds.ABILITY_FROST, 1.4F, 0.8F);
        for (LivingEntity e : enemies) {
            ctx.hit(e, 1.5F, Fx.FROST);
            ctx.freeze(e, 50);
            ctx.pin(e, 50);
            EarthChunkEntity.shell(ctx.level, e, Blocks.PACKED_ICE.defaultBlockState(), 48);
        }
        ctx.later(46, () -> {
            for (LivingEntity e : enemies) {
                if (!e.isAlive()) {
                    continue;
                }
                ctx.hit(e, 2.0F, Fx.FROST);
                Fx.burst(ctx.level, ModParticles.FROST.get(), Fx.FROST, 1.6F, e.getBoundingBox().getCenter(), 24, 0.5, 0.25);
                ctx.soundAt(e.position(), ModSounds.ABILITY_FROST, 1.0F, 1.5F);
            }
            ctx.sound(net.minecraft.sounds.SoundEvents.GLASS_BREAK, 1.2F, 0.7F);
        });
        return true;
    }

    static boolean eventHorizon(AbilityContext ctx) {
        Vec3 at = ctx.eye().add(ctx.look().scale(10));
        Vec3 clipped = ctx.aimPoint(10);
        if (clipped.distanceTo(ctx.eye()) < 9) {
            at = clipped.add(0, 1.5, 0);
        }
        VortexEntity.spawn(ctx.player, at, false, 8.0F, 1.6F, 100, Fx.VOID, ctx.dmg(0.3F), ctx.dmg(3.0F), 0);
        ctx.soundAt(at, ModSounds.ABILITY_VOID, 1.6F, 0.5F);
        ctx.soundAt(at, ModSounds.ABILITY_VOID_HUM, 1.4F, 0.6F);
        return true;
    }




    static boolean moonfall(AbilityContext ctx) {
        int c = ctx.color();
        for (LivingEntity e : ctx.around(12.0)) {
            ctx.pin(e, 40);
            e.addEffect(new MobEffectInstance(ModEffects.FROZEN, 40, 0));
            Fx.burst(ctx.level, ModParticles.RUNE.get(), c, 1.2F, e.getBoundingBox().getCenter(), 6, 0.4, 0.02);
        }
        Fx.ring(ctx.level, ctx.pos(), c, 12.0F);
        Fx.burst(ctx.level, ModParticles.GLOW.get(), 0xFFFFFF, 2.0F, ctx.eye().add(0, 2, 0), 30, 1.5, 0.05);
        ctx.sound(ModSounds.ABILITY_FROST, 1.0F, 1.6F);
        float[] rolls = {0, 50, -50};
        float[] yaws = {0, -18, 18};
        for (int i = 0; i < 3; i++) {
            final int n = i;
            ctx.later(8 + i * 8, () -> {
                Vec3 dir = Vec3.directionFromRotation(ctx.player.getXRot() * 0.3F, ctx.player.getYRot() + yaws[n]);
                Vec3 start = ctx.eye().add(0, -0.4, 0).add(dir.scale(1.2));
                SlashWaveEntity.fire(ctx.player, start, dir, 1.4F, 18, 3.2F, rolls[n], ctx.dmg(1.6F), c, false, null);
                ctx.slash(rolls[n], c, 1.6F, 1.6);
                ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 1.2F, 0.8F + n * 0.15F);
            });
        }
        return true;
    }

    static boolean regicide(AbilityContext ctx) {
        LivingEntity target = ctx.aimed(14.0);
        if (target == null) {
            return false;
        }
        int c = ctx.color();
        Vec3 to = target.position().subtract(ctx.pos());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        double dist = flat.length();
        Vec3 dir = flat.lengthSqr() < 1e-4 ? ctx.flatLook() : flat.normalize();
        ctx.sound(ModSounds.ABILITY_DASH, 1.2F, 0.6F);
        ctx.leap(dir.scale(Math.min(1.6, 0.2 + dist * 0.105)).add(0, 0.95 + Math.max(0, to.y) * 0.06, 0), land -> {
            if (!target.isAlive()) {
                return;
            }
            boolean execute = target.getHealth() < target.getMaxHealth() * 0.3F;
            Vec3 at = target.getBoundingBox().getCenter();
            if (target.distanceToSqr(land.player) < 5 * 5) {
                land.hit(target, execute ? 8.0F : 4.0F, Fx.GOLD);
                land.stagger(target, 60);
            }
            for (LivingEntity e : land.around(target.position(), 3.0)) {
                if (e != target) {
                    land.hit(e, 1.5F, c);
                }
            }
            land.slashAt(at, land.player.getYRot(), 90, Fx.GOLD, 2.2F);
            land.slashAt(at, land.player.getYRot(), 45, c, 1.8F);
            Fx.impact(land.level, at, Fx.GOLD, 2.8F);
            for (int k = 0; k < 12; k++) {
                double a = k * Math.PI / 6;
                Fx.shoot(land.level, ModParticles.GLOW.get(), Fx.GOLD, 1.6F, at.add(Math.cos(a) * 0.5, 1.2, Math.sin(a) * 0.5),
                        new Vec3(0, 0.12, 0));
            }
            land.shockwave(land.pos(), 4.0F, 0.8F, 0, 0.4, Fx.GOLD, null);
            land.sound(ModSounds.ABILITY_SHOCKWAVE, 1.3F, 0.8F);
            land.sound(ModSounds.WEAPON_HIT_METAL, 1.2F, 0.5F);
            land.shakeNearby(land.pos(), 16, 1.0F, 10);
            if (!target.isAlive()) {
                land.data().ultimate = CombatData.ULTIMATE_MAX / 2;
                land.sound(ModSounds.ULTIMATE_READY, 1.0F, 1.3F);
            }
        });
        return true;
    }

    private LegendaryUltimates() {
    }
}

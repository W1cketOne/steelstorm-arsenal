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
            case SOLARIS -> Ability.ultimate("solaris_supernova", "Supernova",
                    "Become a falling star: three rings of solar fire roll out to 16 blocks, burning and hurling every "
                            + "enemy, and you are healed for half your health.",
                    500, LegendaryUltimates::supernova);
            case WORLDSPLITTER -> Ability.ultimate("worldsplitter_sunder", "Sunder the World",
                    "Rend the ground in front of you with a 24-block fissure, then loose a fan of seven giant slashes.",
                    500, LegendaryUltimates::sunder);
            case ECLIPSE -> Ability.ultimate("eclipse_total_eclipse", "Total Eclipse",
                    "Blot out the sun: every enemy within 16 blocks is blinded and cut down by a storm of twelve "
                            + "shadow strikes.",
                    500, LegendaryUltimates::totalEclipse);
            case STARFALL -> Ability.ultimate("starfall_meteor_storm", "Meteor Storm",
                    "Call twelve meteors down on the area you look at over three seconds.",
                    500, LegendaryUltimates::meteorStorm);
            case SOULREAPER -> Ability.ultimate("soulreaper_harvest", "Harvest of Souls",
                    "Open a soul well around you: it drags in everything within 14 blocks and drains it, healing you "
                            + "for every soul it touches.",
                    500, LegendaryUltimates::harvest);
            case VENOMFANG -> Ability.ultimate("venomfang_thousand_cuts", "Thousand Cuts",
                    "Twenty lightning-fast cuts on every enemy within 8 blocks, each one dripping with poison and decay.",
                    500, LegendaryUltimates::thousandCuts);
            case DRAGONSPINE -> Ability.ultimate("dragonspine_dragons_descent", "Dragon's Descent",
                    "Soar into the air and crash down in a storm of dragonfire, then breathe five waves of flame ahead.",
                    500, LegendaryUltimates::dragonsDescent);
            case TITANBREAKER -> Ability.ultimate("titanbreaker_titanfall", "Titanfall",
                    "Bring the sky down: a 14-block quake that deals 4x damage plus 20% of each enemy's max health "
                            + "and throws them skyward.",
                    500, LegendaryUltimates::titanfall);
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

    static boolean supernova(AbilityContext ctx) {
        int c = 0xFFB01F;
        ctx.player.heal(ctx.player.getMaxHealth() * 0.5F);
        Fx.impact(ctx.level, ctx.pos().add(0, 1, 0), 0xFFF2B0, 4.0F);
        for (int i = 0; i < 3; i++) {
            final int n = i;
            ctx.later(i * 7, () -> {
                float r = 7 + n * 4.5F;
                ctx.shockwave(ctx.pos(), r, 1.1F, 2.5F, 0.7, c, e -> e.igniteForSeconds(8));
                Fx.ring(ctx.level, ctx.pos(), n == 2 ? 0xFFF2B0 : c, r);
                Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 2.2F, ctx.player.getBoundingBox().getCenter(), 30, 1.2, 0.2);
                ctx.sound(ModSounds.ABILITY_INFERNO, 1.6F, 0.7F + n * 0.15F);
                ctx.shakeNearby(ctx.pos(), 24, 1.0F, 8);
            });
        }
        ctx.sound(ModSounds.ABILITY_EPIC_IMPACT, 1.6F, 1.2F);
        return true;
    }

    static boolean sunder(AbilityContext ctx) {
        int c = 0x22E0C8;
        ctx.fissure(ctx.pos(), ctx.flatLook(), 24, 2.2, 3.0F, 0.9, c, e -> ctx.armorBreak(e, 200));
        for (int i = 0; i < 7; i++) {
            final int n = i;
            ctx.later(6 + i * 2, () -> {
                float yaw = ctx.player.getYRot() - 45 + n * 15;
                Vec3 dir = Vec3.directionFromRotation(ctx.player.getXRot() * 0.2F, yaw);
                Vec3 start = ctx.eye().add(0, -0.5, 0).add(dir.scale(1.0));
                SlashWaveEntity.fire(ctx.player, start, dir, 1.5F, 22, 3.5F, n % 2 == 0 ? 0 : 35, ctx.dmg(2.2F), c, false, null);
                ctx.sound(ModSounds.ABILITY_SLASH_WAVE, 1.3F, 0.6F + n * 0.08F);
            });
        }
        ctx.shake(1.0F, 12);
        return true;
    }

    static boolean totalEclipse(AbilityContext ctx) {
        List<LivingEntity> enemies = new ArrayList<>(ctx.around(16.0));
        int c = 0x7A2CFF;
        Fx.burst(ctx.level, ModParticles.GLOW.get(), 0x1A0033, 3.0F, ctx.eye().add(0, 3, 0), 60, 3.0, 0.02);
        for (LivingEntity e : enemies) {
            e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 120, 0));
            e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 120, 0));
            ctx.pin(e, 50);
        }
        ctx.sound(ModSounds.ABILITY_VOID, 1.5F, 0.6F);
        for (int i = 0; i < 12; i++) {
            final int n = i;
            ctx.later(4 + i * 3, () -> {
                if (enemies.isEmpty()) {
                    return;
                }
                LivingEntity e = enemies.get(n % enemies.size());
                if (!e.isAlive()) {
                    return;
                }
                ctx.hit(e, 1.6F, c);
                ctx.slashAt(e.getBoundingBox().getCenter(), n * 47.0F, n * 61.0F % 180, n % 2 == 0 ? c : 0xFFFFFF, 1.6F);
                ctx.soundAt(e.position(), ModSounds.WEAPON_SWING, 1.0F, 1.3F + (n % 4) * 0.1F);
            });
        }
        return true;
    }

    static boolean meteorStorm(AbilityContext ctx) {
        Vec3 at = ctx.aimPoint(30);
        for (int i = 0; i < 12; i++) {
            final int n = i;
            ctx.later(i * 5, () -> {
                double a = ctx.level.random.nextDouble() * Math.PI * 2;
                double r = n == 0 ? 0 : 1.5 + ctx.level.random.nextDouble() * 6;
                Vec3 hit = at.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                com.steelstorm.arsenal.entity.MeteorEntity m = com.steelstorm.arsenal.registry.ModEntities.METEOR.get().create(ctx.level);
                if (m == null) {
                    return;
                }
                m.crater = false;
                m.owner = ctx.player;
                m.setSize(1.0F + ctx.level.random.nextFloat() * 0.8F);
                Vec3 vel = new Vec3(0.5, -1.8, 0.3);
                m.setPos(hit.subtract(vel.scale(22)));
                m.setDeltaMovement(vel);
                ctx.level.addFreshEntity(m);
            });
        }
        ctx.sound(ModSounds.ABILITY_EPIC_IMPACT, 1.4F, 0.6F);
        return true;
    }

    static boolean harvest(AbilityContext ctx) {
        VortexEntity.spawn(ctx.player, ctx.pos().add(0, 1, 0), true, 14.0F, 2.0F, 80, 0x5BFFB0, ctx.dmg(0.6F), ctx.dmg(3.0F), 2.0F);
        ctx.sound(ModSounds.ABILITY_VOID_HUM, 1.6F, 0.7F);
        ctx.sound(ModSounds.ABILITY_VOID, 1.4F, 0.5F);
        return true;
    }

    static boolean thousandCuts(AbilityContext ctx) {
        List<LivingEntity> enemies = new ArrayList<>(ctx.around(8.0));
        int c = 0x7CFF3A;
        for (int i = 0; i < 20; i++) {
            final int n = i;
            ctx.later(i * 2, () -> {
                for (LivingEntity e : enemies) {
                    if (!e.isAlive()) {
                        continue;
                    }
                    e.invulnerableTime = 0;
                    ctx.hit(e, 0.45F, c);
                    if (n % 5 == 0) {
                        e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 160, 2));
                        e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 160, 2));
                        ctx.bleed(e, 160, 3);
                        ctx.slashAt(e.getBoundingBox().getCenter(), n * 37.0F, n * 53.0F % 180, c, 1.3F);
                    }
                }
                if (n % 2 == 0) {
                    ctx.sound(ModSounds.WEAPON_SWING, 0.8F, 1.6F + (n % 3) * 0.1F);
                }
            });
        }
        return true;
    }

    static boolean dragonsDescent(AbilityContext ctx) {
        int c = 0xFF5A1F;
        ctx.sound(ModSounds.ABILITY_DASH, 1.3F, 0.5F);
        ctx.leap(ctx.flatLook().scale(0.4).add(0, 1.6, 0), land -> {
            land.shockwave(land.pos(), 10.0F, 1.0F, 3.0F, 0.9, c, e -> e.igniteForSeconds(10));
            Fx.impact(land.level, land.pos().add(0, 0.3, 0), c, 3.5F);
            Fx.cracks(land.level, land.pos(), c, 8.0F);
            land.sound(ModSounds.ABILITY_INFERNO, 1.6F, 0.7F);
            land.shakeNearby(land.pos(), 20, 1.2F, 12);
            for (int i = 0; i < 5; i++) {
                final int n = i;
                land.later(6 + i * 4, () -> {
                    Vec3 dir = Vec3.directionFromRotation(land.player.getXRot() * 0.3F, land.player.getYRot() - 20 + n * 10);
                    SlashWaveEntity.fire(land.player, land.eye().add(dir), dir, 1.3F, 18, 2.6F, 90, land.dmg(1.8F), c, false,
                            e -> e.igniteForSeconds(8));
                    land.sound(ModSounds.ABILITY_INFERNO, 1.0F, 1.0F + n * 0.1F);
                });
            }
        });
        return true;
    }

    static boolean titanfall(AbilityContext ctx) {
        int c = 0xFFD24A;
        ctx.shockwave(ctx.pos(), 14.0F, 1.0F, 4.0F, 1.4, c, e -> {
            e.invulnerableTime = 0;
            e.hurt(ctx.player.damageSources().playerAttack(ctx.player), e.getMaxHealth() * 0.2F);
            ctx.armorBreak(e, 200);
        });
        Fx.impact(ctx.level, ctx.pos().add(0, 0.3, 0), c, 4.0F);
        Fx.cracks(ctx.level, ctx.pos(), c, 12.0F);
        ctx.sound(ModSounds.ABILITY_EPIC_IMPACT, 1.8F, 0.6F);
        ctx.sound(ModSounds.ABILITY_SHOCKWAVE, 1.6F, 0.5F);
        ctx.shakeNearby(ctx.pos(), 28, 1.6F, 16);
        return true;
    }

    private LegendaryUltimates() {
    }
}

package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.entity.ThrownHammerEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Warhammer: every ability shakes the earth. */
public final class HammerAbilities {
    public static AbilitySet create() {
        return new AbilitySet("warhammer",
                Ability.of("warhammer_earthquake", "Earthquake",
                        "Smash the ground so hard it ripples outward in a 7-block shockwave that launches every enemy it "
                                + "reaches, followed by a wider aftershock.",
                        40, 180, HammerAbilities::earthquake),
                Ability.of("warhammer_hammer_throw", "Hammer Throw",
                        "Hurl your hammer up to 14 blocks. It smashes into the first enemy or wall with a shockwave, "
                                + "staggering and breaking armour, then flies back to your hand.",
                        30, 160, HammerAbilities::hammerThrow),
                SignatureAbilities.stonePrison(),
                Ability.ultimate("warhammer_cataclysm", "Cataclysm",
                        "Leap high into the air and come down like a meteor: three huge shockwaves roll out 12 blocks, "
                                + "launching and staggering everything they reach.",
                        700, HammerAbilities::cataclysm));
    }

    /** Where the hammer head hits the ground in front of the player. */
    static Vec3 strikePoint(AbilityContext ctx) {
        Vec3 p = ctx.pos().add(ctx.flatLook().scale(1.3));
        var ground = Shockwaves.ground(ctx.level, p.x, ctx.pos().y, p.z);
        return ground != null ? new Vec3(p.x, ground.getY() + 1, p.z) : p;
    }

    /** Sparks, dust, debris and a flash where the hammer lands. */
    static void smashFx(AbilityContext ctx, Vec3 at, float size) {
        int c = ctx.color();
        Fx.impact(ctx.level, at.add(0, 0.4, 0), c, 1.6F * size);
        Fx.sparks(ctx.level, 0xFFD166, at.add(0, 0.2, 0), (int) (14 * size), 0.8);
        Fx.burst(ctx.level, ModParticles.SMOKE.get(), 0x6B6158, 1.8F, at.add(0, 0.3, 0), (int) (10 * size), 0.8 * size, 0.2, 0.8 * size, 0.03);
        Shockwaves.crater(ctx.level, at, 0.9F * size, 0.7F);
    }

    static boolean earthquake(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 at = strikePoint(ctx);
        ctx.slash(95, c, 1.1F, 1.4);
        smashFx(ctx, at, 1.2F);
        ctx.shockwave(at, 7.0F, 1.0F, 1.2F, 0.8, c, null);
        ctx.later(7, () -> ctx.shockwave(at, 9.5F, 1.3F, 0.4F, 0.45, 0xFFD166, null));
        ctx.soundAt(at, ModSounds.ABILITY_SHOCKWAVE, 1.4F, 0.9F);
        ctx.soundAt(at, ModSounds.ABILITY_RUMBLE, 1.2F, 1.0F);
        ctx.soundAt(at, ModSounds.WEAPON_HIT_METAL, 1.0F, 0.5F);
        ctx.shakeNearby(at, 18, 1.1F, 14);
        return true;
    }

    static boolean hammerThrow(AbilityContext ctx) {
        int slot = ctx.player.getInventory().selected;
        ItemStack stack = ctx.player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            return false;
        }
        ctx.player.getInventory().setItem(slot, ItemStack.EMPTY);
        ThrownHammerEntity hammer = new ThrownHammerEntity(ctx.player, stack, slot, ctx.dmg(2.0F), ctx.dmg(1.0F), 14.0);
        ctx.level.addFreshEntity(hammer);
        ctx.sound(ModSounds.WEAPON_HAMMER_THROW, 1.1F, 1.0F);
        ctx.slash(30, ctx.color(), 1.0F, 1.2);
        return true;
    }


    static boolean cataclysm(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 look = ctx.flatLook();
        Shockwaves.dust(ctx.level, ctx.pos(), 1.5F);
        ctx.sound(ModSounds.ABILITY_DASH, 1.2F, 0.6F);
        ctx.leap(look.scale(0.35).add(0, 1.45, 0), null);
        for (int i = 1; i < 30; i++) {
            ctx.later(i, () -> {
                if (!ctx.player.onGround()) {
                    Vec3 p = ctx.player.getBoundingBox().getCenter();
                    Fx.burst(ctx.level, ModParticles.GLOW.get(), c, 1.6F, p, 3, 0.3, 0.4, 0.3, 0.02);
                    Fx.burst(ctx.level, ModParticles.SMOKE.get(), 0x3A302A, 1.2F, p, 1, 0.2, 0.2, 0.2, 0.01);
                }
            });
        }
        // Slam down hard once past the top of the jump.
        ctx.later(12, () -> {
            ctx.player.setDeltaMovement(look.scale(0.2).add(0, -2.4, 0));
            ctx.player.hurtMarked = true;
            com.steelstorm.arsenal.ability.AbilityManager.onLanding(ctx.player, 50, () -> impact(ctx));
        });
        return true;
    }

    /** Cataclysm (and Worldbreaker) landing: rings of earth, a crater, and the whole area shaking. */
    static void impact(AbilityContext ctx) {
        int c = ctx.color();
        Vec3 at = ctx.pos();
        smashFx(ctx, at, 2.2F);
        Shockwaves.crater(ctx.level, at, 3.5F, 1.6F);
        ctx.shockwave(at, 12.0F, 1.1F, 2.0F, 1.2, c, e -> ctx.stagger(e, 60));
        ctx.later(6, () -> ctx.shockwave(at, 12.0F, 1.35F, 1.0F, 0.6, 0xFFD166, null));
        ctx.later(12, () -> ctx.shockwave(at, 12.0F, 1.6F, 0.6F, 0.4, 0xFFF1C1, null));
        for (int k = 0; k < 40; k++) {
            double a = k * Math.PI / 20;
            Fx.shoot(ctx.level, ModParticles.GLOW.get(), k % 3 == 0 ? 0xFFF1C1 : c, 2.0F, at.add(0, 0.3, 0),
                    new Vec3(Math.cos(a) * 0.6, 0.3 + ctx.level.random.nextDouble() * 0.5, Math.sin(a) * 0.6));
        }
        ctx.sound(ModSounds.ABILITY_SHOCKWAVE, 1.6F, 0.6F);
        ctx.sound(ModSounds.ABILITY_RUMBLE, 1.5F, 0.7F);
        ctx.sound(ModSounds.ABILITY_GROUND_CRACK, 1.4F, 0.7F);
        ctx.shakeNearby(at, 36, 2.0F, 26);
        for (LivingEntity e : ctx.around(at, 2.5)) {
            ctx.hit(e, 1.0F, c);
        }
    }

    private HammerAbilities() {
    }
}

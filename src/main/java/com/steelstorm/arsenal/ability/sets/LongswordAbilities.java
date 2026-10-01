package com.steelstorm.arsenal.ability.sets;

import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilityContext;
import com.steelstorm.arsenal.ability.AbilityManager;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.entity.SpectralWeaponEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Longsword: a balanced duelist's kit built around timing. */
public final class LongswordAbilities {
    public static AbilitySet create() {
        return new AbilitySet("longsword",
                Ability.of("longsword_rising_slash", "Rising Slash",
                        "An upward cut that throws enemies in front of you into the air and carries you up with it.",
                        25, 120, LongswordAbilities::risingSlash),
                Ability.of("longsword_riposte", "Riposte",
                        "Take a parrying stance for 1.5 seconds. The next attack against you is turned aside and answered "
                                + "with a counter-strike for double damage that staggers the attacker.",
                        20, 160, LongswordAbilities::riposte),
                Ability.of("longsword_blade_dash", "Blade Dash",
                        "Dash 6 blocks forward, cutting and staggering every enemy you pass through.",
                        20, 100, LongswordAbilities::bladeDash),
                Ability.ultimate("longsword_judgment", "Judgment of Steel",
                        "Six giant spectral swords rain down around the spot you look at, then a seventh crashes into the "
                                + "centre with a shockwave.",
                        600, LongswordAbilities::judgment));
    }

    static boolean risingSlash(AbilityContext ctx) {
        int c = ctx.color();
        ctx.sound(ModSounds.WEAPON_SWING_HEAVY, 1.0F, 1.15F);
        ctx.slash(-80, c, 1.15F, 1.7);
        ctx.later(2, () -> ctx.slash(-100, 0xFFFFFF, 0.85F, 1.9));
        for (LivingEntity e : ctx.cone(4.2, 60)) {
            if (ctx.hit(e, 1.2F, c)) {
                ctx.launch(e, 1.0);
            }
        }
        Vec3 m = ctx.player.getDeltaMovement();
        ctx.player.setDeltaMovement(m.x, Math.max(m.y, 0.55), m.z);
        ctx.player.hurtMarked = true;
        ctx.data().noFallUntil = ctx.now() + 40;
        ctx.shake(0.3F, 5);
        return true;
    }

    static boolean riposte(AbilityContext ctx) {
        ctx.player.addEffect(new MobEffectInstance(ModEffects.RIPOSTE, 30, 0, false, true, true));
        ctx.sound(ModSounds.WEAPON_PARRY, 0.9F, 0.7F);
        Fx.ring(ctx.level, ctx.pos(), ctx.color(), 2.0F);
        Fx.burst(ctx.level, ModParticles.SPARK.get(), Fx.STEEL, 1.0F, ctx.player.getBoundingBox().getCenter(), 10, 0.4, 0.2);
        return true;
    }

    /** Called when a player in the Riposte stance is attacked: cancel the hit and strike back. */
    public static void counter(ServerPlayer player, LivingEntity attacker) {
        ServerLevel level = player.serverLevel();
        player.removeEffect(ModEffects.RIPOSTE);
        ItemStack stack = player.getMainHandItem();
        int c = WeaponLooks.abilityColor(stack);
        Vec3 at = attacker.getBoundingBox().getCenter();
        if (player.distanceToSqr(attacker) < 6 * 6) {
            float damage = (float) (AbilityManager.baseDamage(stack) * 2.0F
                    * com.steelstorm.arsenal.Config.SPECIAL_DAMAGE_MULTIPLIER.get());
            CombatUtil.specialHurt(player, attacker, damage);
            attacker.addEffect(new MobEffectInstance(ModEffects.STAGGER, 50, 0));
            attacker.knockback(0.9, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
            Fx.slash(level, at, player.getYRot(), 0, 40, c, 1.1F);
            Fx.slash(level, at, player.getYRot(), 0, -40, Fx.GOLD, 1.1F);
        }
        Fx.impact(level, at, Fx.GOLD, 1.6F);
        Fx.sparks(level, Fx.GOLD, player.getEyePosition().add(player.getLookAngle().scale(0.8)), 14, 0.6);
        Fx.sound(level, player.position(), ModSounds.WEAPON_PERFECT_PARRY, 1.2F, 1.1F);
        Stamina.shake(player, 0.7F, 7);
        Stamina.restore(player, 15);
        AbilityManager.addUltimate(player, 12);
        player.displayClientMessage(Component.translatable("message.steelstorm.riposte").withStyle(ChatFormatting.GOLD), true);
    }

    static boolean bladeDash(AbilityContext ctx) {
        int c = ctx.color();
        ctx.sound(ModSounds.ABILITY_DASH, 1.0F, 1.0F);
        for (LivingEntity e : ctx.dash(ctx.look(), 6.0)) {
            if (ctx.hit(e, 1.0F, c)) {
                ctx.stagger(e, 15);
            }
            Fx.slash(ctx.level, e.getBoundingBox().getCenter(), ctx.player.getYRot() + 90, 0, 25, c, 0.8F);
        }
        ctx.slash(0, c, 1.0F, 1.2);
        ctx.sound(ModSounds.WEAPON_SWING, 1.0F, 1.3F);
        return true;
    }

    static boolean judgment(AbilityContext ctx) {
        Vec3 center = ctx.aimPoint(20);
        ItemStack blade = ctx.weaponCopy();
        int c = ctx.color();
        Fx.ring(ctx.level, center, Fx.GOLD, 6.5F);
        Fx.burst(ctx.level, ModParticles.RUNE.get(), Fx.GOLD, 1.4F, center.add(0, 0.3, 0), 30, 3.0, 0.1, 3.0, 0.01);
        ctx.soundAt(center, ModSounds.ABILITY_CAST, 1.2F, 0.8F);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + ctx.level.random.nextDouble() * 0.3;
            Vec3 at = center.add(Math.cos(a) * 3.3, 0, Math.sin(a) * 3.3);
            BlockPos g = Shockwaves.ground(ctx.level, at.x, center.y, at.z);
            Vec3 impact = g != null ? new Vec3(at.x, g.getY() + 1, at.z) : at;
            final float pitch = 0.9F + i * 0.05F;
            SpectralWeaponEntity.drop(ctx.level, blade, impact, 3.0F, c, 10 + i * 4, 11.0F, 4, 34, w -> {
                if (!ctx.alive()) {
                    return;
                }
                for (LivingEntity e : ctx.around(impact, 2.3)) {
                    ctx.hit(e, 1.5F, c);
                    ctx.stagger(e, 20);
                }
                Shockwaves.ring(ctx.level, ctx.player, impact, 2.5F, 0.6F, 0, 0.3, c, null);
                Fx.impact(ctx.level, impact.add(0, 0.5, 0), c, 1.6F);
                Fx.sparks(ctx.level, c, impact.add(0, 0.2, 0), 10, 0.6);
                ctx.soundAt(impact, ModSounds.ABILITY_BLADE_FALL, 1.0F, pitch);
                ctx.shakeNearby(impact, 10, 0.35F, 5);
            });
        }
        SpectralWeaponEntity.drop(ctx.level, blade, center, 4.6F, Fx.GOLD, 40, 16.0F, 5, 44, w -> {
            if (!ctx.alive()) {
                return;
            }
            for (LivingEntity e : ctx.around(center, 5.0)) {
                ctx.hit(e, 2.5F, Fx.GOLD);
                ctx.stagger(e, 40);
            }
            ctx.shockwave(center, 7.0F, 0.9F, 0, 0.9, Fx.GOLD, null);
            Shockwaves.crater(ctx.level, center, 2.5F, 1.0F);
            ctx.slashAt(center.add(0, 1.3, 0), ctx.player.getYRot(), 45, Fx.GOLD, 2.6F);
            ctx.slashAt(center.add(0, 1.3, 0), ctx.player.getYRot(), -45, Fx.GOLD, 2.6F);
            Fx.impact(ctx.level, center.add(0, 0.6, 0), Fx.GOLD, 3.0F);
            ctx.soundAt(center, ModSounds.ABILITY_SHOCKWAVE, 1.4F, 0.8F);
            ctx.soundAt(center, ModSounds.ABILITY_BLADE_FALL, 1.4F, 0.6F);
            ctx.shakeNearby(center, 22, 1.2F, 14);
        });
        return true;
    }

    private LongswordAbilities() {
    }
}

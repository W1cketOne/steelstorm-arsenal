package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Weapon passives that trigger after a hit actually lands. */
public final class WeaponEffects {
    /** Adjusts the damage of a melee swing before it lands (combo finishers, backstabs...). */
    public static float modifyMeleeDamage(Player player, WeaponItem weapon, LivingEntity target, float amount, CombatData data) {
        switch (weapon.type()) {
            case LONGSWORD -> {
                if (data.finisherPending) {
                    amount *= 1.3F;
                }
            }
            case DUAL_DAGGERS -> {
                if (CombatUtil.isBehind(target, player)) {
                    amount *= 2.0F;
                    if (player.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.7), target.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
                    }
                }
            }
            default -> {
            }
        }
        return amount;
    }

    /** A normal melee swing landed for {@code damage} after armour. */
    public static void onMeleeHit(Player player, WeaponItem weapon, LivingEntity target, float damage, boolean fullStrength, CombatData data) {
        switch (weapon.type()) {
            case GREATSWORD -> {
                if (fullStrength) {
                    wideSweep(player, target, damage);
                }
            }
            default -> {
            }
        }
    }

    /** A special ability hit a target; lets passives like Bleed apply to specials too. */
    public static void onSpecialHit(Player player, WeaponItem weapon, LivingEntity target) {
    }

    /** Greatsword passive: the sweep reaches enemies further from the target than vanilla's. */
    private static void wideSweep(Player player, LivingEntity target, float damage) {
        float sweep = Math.max(1.0F, damage * 0.4F);
        for (LivingEntity other : CombatUtil.around(player, target.position(), 3.0)) {
            if (other != target && other.distanceToSqr(target) > 1.6 * 1.6 && player.distanceToSqr(other) < 25) {
                CombatUtil.specialHurt(player, other, sweep);
                other.knockback(0.4, player.getX() - other.getX(), player.getZ() - other.getZ());
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, other.getX(), other.getY(0.5), other.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private WeaponEffects() {
    }
}

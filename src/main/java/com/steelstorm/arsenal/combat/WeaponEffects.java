package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.registry.ModDamageTypes;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModEnchantments;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Weapon passives, Stormsteel and enchantment effects. Server side only. */
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
            case BATTLEAXE -> {
                if (target.isBlocking() || (target instanceof Player p && CombatEvents.isGuarding(p))) {
                    amount *= 1.5F;
                }
            }
            default -> {
            }
        }
        int executioner = ModEnchantments.level(player, player.getMainHandItem(), ModEnchantments.EXECUTIONER);
        if (executioner > 0 && target.getHealth() <= target.getMaxHealth() * 0.35F) {
            amount *= 1.0F + 0.2F * executioner;
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
            case KATANA -> {
                if (data.critPending) {
                    BleedEffect.apply(target, 120);
                }
            }
            case WARHAMMER -> target.addEffect(new MobEffectInstance(ModEffects.ARMOR_BREAK, 100, 0));
            case SCYTHE -> heal(player, damage * 0.10F);
            default -> {
            }
        }
        data.critPending = false;
        applySharedOnHit(player, weapon, target);
    }

    /** A special ability hit a target; weapon passives that make sense for specials apply too. */
    public static void onSpecialHit(Player player, WeaponItem weapon, LivingEntity target, float damage) {
        switch (weapon.type()) {
            case KATANA -> BleedEffect.apply(target, 120);
            case WARHAMMER -> target.addEffect(new MobEffectInstance(ModEffects.ARMOR_BREAK, 100, 0));
            case SCYTHE -> heal(player, damage * 0.10F);
            default -> {
            }
        }
    }

    /** Effects shared by every weapon: Stormsteel zaps and enchantments. */
    private static void applySharedOnHit(Player player, WeaponItem weapon, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        int lacerate = ModEnchantments.level(player, stack, ModEnchantments.LACERATE);
        if (lacerate > 0 && player.getRandom().nextFloat() < 0.15F * lacerate) {
            BleedEffect.apply(target, 100);
        }
        int momentum = ModEnchantments.level(player, stack, ModEnchantments.MOMENTUM);
        if (momentum > 0) {
            Stamina.restore(player, 4.0F * momentum);
        }
        if (weapon.isStormsteel() && player.getRandom().nextFloat() < 0.25F) {
            zap(player, target, 3.0F);
        }
    }

    /** Stormsteel passive: lightning arcs from the target to the nearest other enemy. */
    public static void zap(Player player, LivingEntity from, float damage) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity next = null;
        double best = 6.0 * 6.0;
        for (LivingEntity candidate : CombatUtil.around(player, from.position(), 6.0)) {
            double d = candidate.distanceToSqr(from);
            if (candidate != from && d < best) {
                best = d;
                next = candidate;
            }
        }
        if (next == null) {
            return;
        }
        next.invulnerableTime = 0;
        next.hurt(ModDamageTypes.zap(level, player), damage);
        Vec3 a = from.getBoundingBox().getCenter();
        Vec3 b = next.getBoundingBox().getCenter();
        int steps = (int) Math.max(4, a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, next.getX(), next.getY(), next.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.25F, 2.0F);
    }

    private static void heal(Player player, float amount) {
        if (amount > 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(amount);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.1), player.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
        }
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

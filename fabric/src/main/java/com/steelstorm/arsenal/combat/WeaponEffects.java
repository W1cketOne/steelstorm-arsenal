package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.ability.AbilityManager;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.registry.ModDamageTypes;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.weapon.Rune;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModEnchantments;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
        if (weapon instanceof LegendaryWeaponItem legendary) {
            switch (legendary.legendary()) {
                case SKYPIERCER -> {
                    if (!target.onGround() && !target.isInWater() && target.getDeltaMovement().y != 0) {
                        amount *= 2.0F;
                        if (player.level() instanceof ServerLevel level) {
                            Fx.sparks(level, 0x9AD8FF, target.getBoundingBox().getCenter(), 10, 0.6);
                        }
                    }
                }
                case KINGSBANE -> {
                    if (target.getHealth() > player.getHealth()) {
                        amount *= 1.5F;
                    }
                }
                case TITANBREAKER -> amount += target.getMaxHealth() * 0.06F;
                case DRAGONSPINE -> {
                    if (!target.onGround()) {
                        amount *= 2.0F;
                    }
                }
                case ECLIPSE -> {
                    if (CombatUtil.isBehind(target, player) || target.hasEffect(MobEffects.BLINDNESS)) {
                        amount *= 1.75F;
                    }
                }
                default -> {
                }
            }
        }
        // Whetstone: a sharpened edge hits harder.
        if (player.getMainHandItem().getOrDefault(ModDataComponents.SHARPENED.get(), 0) > 0) {
            amount += 2.0F;
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
        if (weapon instanceof LegendaryWeaponItem legendary) {
            legendaryOnHit(player, legendary, target, data);
        }
        data.critPending = false;
        applySharedOnHit(player, weapon, target);
    }

    private static void legendaryOnHit(Player player, LegendaryWeaponItem weapon, LivingEntity target, CombatData data) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        switch (weapon.legendary()) {
            case TEMPEST_EDGE -> {
                // Combo finishers call down lightning (visual bolt, controlled damage that can't hurt the wielder).
                if (data.finisherPending) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(target.getX(), target.getY(), target.getZ());
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                    target.invulnerableTime = 0;
                    target.hurt(ModDamageTypes.zap(level, player), 7.0F);
                    zap(player, target, 5.0F);
                }
            }
            case RIMECLEAVER -> {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 100));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                Fx.burst(level, ModParticles.FROST.get(), Fx.FROST, 1.3F, target.getBoundingBox().getCenter(), 12, 0.4, 0.04);
                Fx.sound(level, target.position(), ModSounds.ABILITY_FROST, 0.5F, 1.6F);
            }
            case VOIDREAVER -> {
                for (LivingEntity other : CombatUtil.around(player, target.position(), 4.5)) {
                    if (other != target) {
                        Vec3 to = target.position().subtract(other.position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 1) {
                            Vec3 pull = to.normalize().scale(0.45 * com.steelstorm.arsenal.ability.Shockwaves.resist(other));
                            other.setDeltaMovement(pull.x, Math.max(other.getDeltaMovement().y, 0.1), pull.z);
                            other.hurtMarked = true;
                        }
                    }
                }
                Fx.burst(level, ModParticles.GLOW.get(), Fx.VOID, 1.4F, target.getBoundingBox().getCenter(), 10, 0.5, 0.05);
            }
            case EARTHSHAKER -> {
                if (data.finisherPending) {
                    float damage = (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 0.5F);
                    Shockwaves.ring(level, player, target.position(), 3.5F, 0.8F, damage, 0.45, 0xFF9A3C, null);
                    Fx.sound(level, target.position(), ModSounds.ABILITY_SHOCKWAVE, 0.9F, 1.3F);
                    Stamina.shake(player, 0.5F, 6);
                }
            }
            case BLOODFANG -> {
                if (target.hasEffect(ModEffects.BLEED)) {
                    heal(player, 1.0F);
                }
                BleedEffect.apply(target, 100);
            }
            case SOLARIS -> {
                target.igniteForSeconds(6);
                float dmg = (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 0.4F);
                Shockwaves.ring(level, player, target.position(), 3.0F, 1.2F, dmg, 0.25, 0xFFB01F, e -> e.igniteForSeconds(4));
                Fx.burst(level, ModParticles.GLOW.get(), 0xFFD25A, 1.6F, target.getBoundingBox().getCenter(), 12, 0.4, 0.1);
                if (data.finisherPending) {
                    Fx.impact(level, target.position(), 0xFFF2B0, 2.5F);
                    Shockwaves.ring(level, player, target.position(), 6.0F, 1.0F, dmg * 3, 0.6, 0xFFB01F, e -> e.igniteForSeconds(8));
                    Fx.sound(level, target.position(), ModSounds.ABILITY_INFERNO, 1.2F, 0.9F);
                }
            }
            case WORLDSPLITTER -> {
                Vec3 start = player.getEyePosition().add(0, -0.5, 0).add(player.getLookAngle().scale(1.0));
                SlashWaveEntity.fire(player, start, player.getLookAngle(), 1.4F, 12, 2.2F, data.finisherPending ? 90 : 0,
                        (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 0.8F), 0x22E0C8, false, null);
                Fx.sound(level, player.position(), ModSounds.ABILITY_SLASH_WAVE, 0.8F, 1.2F);
            }
            case ECLIPSE -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                if (data.critPending) {
                    for (int k = -1; k <= 1; k++) {
                        Vec3 dir = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + k * 15);
                        SlashWaveEntity.fire(player, player.getEyePosition().add(0, -0.4, 0).add(dir), dir, 1.4F, 12, 1.6F, k * 30,
                                (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 0.7F), 0x7A2CFF, false, null);
                    }
                    Fx.sound(level, player.position(), ModSounds.ABILITY_SLASH_WAVE, 0.8F, 1.4F);
                }
            }
            case STARFALL -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                if (data.finisherPending) {
                    com.steelstorm.arsenal.entity.MeteorEntity m = com.steelstorm.arsenal.registry.ModEntities.METEOR.get().create(level);
                    if (m != null) {
                        m.crater = false;
                        m.owner = player;
                        m.setSize(1.0F);
                        Vec3 vel = new Vec3(0.3, -1.9, 0.2);
                        m.setPos(target.position().subtract(vel.scale(16)));
                        m.setDeltaMovement(vel);
                        level.addFreshEntity(m);
                    }
                }
            }
            case SOULREAPER -> {
                heal(player, (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 0.25F));
                for (LivingEntity other : CombatUtil.around(player, target.position(), 6)) {
                    if (other != target) {
                        Vec3 to = target.position().subtract(other.position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 1) {
                            Vec3 pull = to.normalize().scale(0.55 * com.steelstorm.arsenal.ability.Shockwaves.resist(other));
                            other.setDeltaMovement(pull.x, Math.max(other.getDeltaMovement().y, 0.1), pull.z);
                            other.hurtMarked = true;
                        }
                    }
                }
                Fx.burst(level, ModParticles.GLOW.get(), 0x5BFFB0, 1.4F, target.getBoundingBox().getCenter(), 10, 0.5, 0.05);
            }
            case VENOMFANG -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                BleedEffect.apply(target, 120);
                heal(player, 1.5F);
            }
            case DRAGONSPINE -> {
                target.igniteForSeconds(6);
                if (target.onGround()) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.75 * com.steelstorm.arsenal.ability.Shockwaves.resist(target), 0));
                    target.hurtMarked = true;
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(),
                        16, 0.3, 0.4, 0.3, 0.05);
            }
            case TITANBREAKER -> {
                target.addEffect(new MobEffectInstance(ModEffects.ARMOR_BREAK, 160, 1));
                if (data.finisherPending) {
                    Shockwaves.ring(level, player, target.position(), 5.0F, 1.0F,
                            (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 1.2F), 0.9, 0xFFD24A, null);
                    Fx.sound(level, target.position(), ModSounds.ABILITY_SHOCKWAVE, 1.2F, 0.7F);
                    Stamina.shake(player, 0.7F, 8);
                }
            }
            case MOONVEIL -> {
                if (data.critPending) {
                    Vec3 start = player.getEyePosition().add(0, -0.4, 0).add(player.getLookAngle().scale(1.2));
                    SlashWaveEntity.fire(player, start, player.getLookAngle(), 1.3F, 9, 1.3F, 0,
                            (float) (AbilityManager.baseDamage(player.getMainHandItem()) * 0.6F), 0xD6DEFF, false, null);
                    Fx.sound(level, player.position(), ModSounds.ABILITY_SLASH_WAVE, 0.7F, 1.5F);
                }
            }
            default -> {
            }
        }
    }

    /** A special ability hit a target; weapon passives that make sense for specials apply too. */
    public static void onSpecialHit(Player player, WeaponItem weapon, LivingEntity target, float damage) {
        Rune rune = player.getMainHandItem().get(ModDataComponents.RUNE.get());
        if (rune != null && player.getRandom().nextFloat() < 0.35F) {
            rune.onHit(player, target, damage);
        }
        switch (weapon.type()) {
            case KATANA -> BleedEffect.apply(target, 120);
            case WARHAMMER -> target.addEffect(new MobEffectInstance(ModEffects.ARMOR_BREAK, 100, 0));
            case SCYTHE -> heal(player, damage * 0.10F);
            default -> {
            }
        }
        if (weapon instanceof LegendaryWeaponItem legendary && legendary.legendary() == LegendaryWeaponItem.Legendary.RIMECLEAVER) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 100));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        }
    }

    /** Effects shared by every weapon: sharpening, runes, Stormsteel zaps and enchantments. */
    private static void applySharedOnHit(Player player, WeaponItem weapon, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        int sharp = stack.getOrDefault(ModDataComponents.SHARPENED.get(), 0);
        if (sharp > 0) {
            if (sharp <= 1) {
                stack.remove(ModDataComponents.SHARPENED.get());
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.steelstorm.dull")
                        .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            } else {
                stack.set(ModDataComponents.SHARPENED.get(), sharp - 1);
            }
            if (player.level() instanceof ServerLevel level) {
                Fx.sparks(level, 0xFFE9A8, target.getBoundingBox().getCenter(), 3, 0.3);
            }
        }
        Rune rune = stack.get(ModDataComponents.RUNE.get());
        if (rune != null) {
            rune.onHit(player, target, weapon.attackDamage());
        }
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

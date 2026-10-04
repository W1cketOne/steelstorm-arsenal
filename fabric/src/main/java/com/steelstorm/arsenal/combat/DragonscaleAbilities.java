package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.DragonscaleArmorItem;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingFallEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;

/**
 * Dragonscale armour. Helmet: Dragon Eyes (night vision, no blindness or darkness). Chestplate:
 * Dragon Heart (Strength II). Leggings: Scaled Hide (Resistance I; melee attackers are burned for 4).
 * Boots: Dragon Stride (Speed I, no fall damage). Full set: Dragon's Fury (immune to fire and lava;
 * every fourth blow erupts in a burst of dragonfire, and at under 40% health a Dragon Roar blasts and
 * burns everything within 10 blocks, every 45 seconds).
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class DragonscaleAbilities {
    public static final int COLOR = 0xFF4A1A;
    private static final Map<UUID, Integer> HITS = new HashMap<>();
    private static final Map<UUID, Long> ROAR = new HashMap<>();

    public static boolean wearing(LivingEntity e, ArmorItem.Type type) {
        return e.getItemBySlot(type.getSlot()).getItem() instanceof DragonscaleArmorItem;
    }

    public static boolean fullSet(LivingEntity e) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof DragonscaleArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (wearing(player, ArmorItem.Type.HELMET)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (wearing(player, ArmorItem.Type.CHESTPLATE)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false, true));
        }
        if (wearing(player, ArmorItem.Type.LEGGINGS)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }
        if (wearing(player, ArmorItem.Type.BOOTS)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        }
        if (fullSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            player.serverLevel().sendParticles(ParticleTypes.SMALL_FLAME, player.getX(), player.getY() + 1.4, player.getZ() + 0.0,
                    3, 0.35, 0.4, 0.35, 0.01);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && wearing(p, ArmorItem.Type.BOOTS)) {
            event.setDamageMultiplier(0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof Player p && event.getSource().is(DamageTypeTags.IS_FIRE) && fullSet(p)) {
            event.setCanceled(true);
            return;
        }
        // Scaled Hide.
        if (target instanceof ServerPlayer p && wearing(p, ArmorItem.Type.LEGGINGS) && CombatUtil.isDirectMelee(event.getSource())
                && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != p) {
            attacker.igniteForSeconds(5);
            attacker.invulnerableTime = 0;
            attacker.hurt(p.damageSources().onFire(), 4.0F);
        }
        // Dragon Roar.
        if (target instanceof ServerPlayer p && fullSet(p) && p.getHealth() - event.getAmount() < p.getMaxHealth() * 0.4F) {
            long now = p.serverLevel().getGameTime();
            if (now >= ROAR.getOrDefault(p.getUUID(), 0L)) {
                ROAR.put(p.getUUID(), now + 900);
                ServerLevel level = p.serverLevel();
                Shockwaves.ring(level, p, p.position(), 10.0F, 1.0F, 18.0F, 1.1, COLOR, e -> e.igniteForSeconds(10));
                Fx.impact(level, p.position().add(0, 1, 0), COLOR, 3.0F);
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 2.0, 1.0, 2.0, 0.2);
                Fx.sound(level, p.position(), ModSounds.ABILITY_INFERNO, 1.6F, 0.6F);
                Fx.sound(level, p.position(), ModSounds.ABILITY_EPIC_IMPACT, 1.2F, 0.7F);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.steelstorm.dragon_roar")
                        .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD), true);
            }
        }
        // Dragon's Fury: every fourth blow erupts in dragonfire.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != target && fullSet(attacker)
                && CombatUtil.isDirectMelee(event.getSource())) {
            target.igniteForSeconds(5);
            int hits = HITS.merge(attacker.getUUID(), 1, Integer::sum);
            if (hits % 4 == 0) {
                ServerLevel level = attacker.serverLevel();
                Shockwaves.ring(level, attacker, target.position(), 5.0F, 1.2F, event.getAmount() * 1.5F, 0.6, COLOR,
                        e -> e.igniteForSeconds(8));
                Fx.burst(level, ModParticles.GLOW.get(), COLOR, 2.0F, target.getBoundingBox().getCenter(), 20, 0.6, 0.15);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 30, 0.8, 0.6, 0.8, 0.1);
                Fx.sound(level, target.position(), ModSounds.ABILITY_INFERNO, 1.2F, 1.0F);
            }
        }
    }

    private DragonscaleAbilities() {
    }
}

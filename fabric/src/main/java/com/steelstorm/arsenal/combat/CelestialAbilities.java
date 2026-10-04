package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.CelestialArmorItem;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.compat.neo.bus.api.EventPriority;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;

/**
 * Celestial armour. Helmet: Halo (night vision; blindness, darkness, poison and wither can't touch
 * you). Chestplate: Seraph Wings (while falling, glide forward instead; sneak to drop). Leggings:
 * Sanctified (constant Regeneration II). Boots: Heavenstep (Speed II, no fall damage). Full set:
 * Divine Aegis (take 30% less damage; at under a quarter health you are restored in a burst of holy
 * light that hurls back every enemy nearby, once a minute) and your blows smite undead for double.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class CelestialAbilities {
    public static final int COLOR = 0xFFE9A8;
    private static final Map<UUID, Long> AEGIS = new HashMap<>();

    public static boolean wearing(LivingEntity e, ArmorItem.Type type) {
        return e.getItemBySlot(type.getSlot()).getItem() instanceof CelestialArmorItem;
    }

    public static boolean fullSet(LivingEntity e) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof CelestialArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (wearing(player, ArmorItem.Type.HELMET)) {
            if (player.tickCount % 80 == 0 || !player.hasEffect(MobEffects.NIGHT_VISION)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            }
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
        }
        if (wearing(player, ArmorItem.Type.LEGGINGS) && player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false, true));
        }
        if (wearing(player, ArmorItem.Type.BOOTS) && player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false, true));
        }
        // Seraph Wings: falling turns into a forward glide.
        if (wearing(player, ArmorItem.Type.CHESTPLATE) && !player.onGround() && !player.isInWater() && !player.isShiftKeyDown()
                && !player.getAbilities().flying && !player.isFallFlying() && player.getDeltaMovement().y < -0.25) {
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.85 + look.x * 0.12, Math.max(v.y, -0.18), v.z * 0.85 + look.z * 0.12);
            player.hurtMarked = true;
            player.fallDistance = 0;
            if (player.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.5, 0.2, 0.5, 0.01);
            }
        }
        if (fullSet(player) && player.tickCount % 10 == 0) {
            Fx.burst(level, ModParticles.SPARKLE.get(), COLOR, 0.8F, player.position().add(0, 2.3, 0), 2, 0.25, 0.01);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && wearing(p, ArmorItem.Type.BOOTS)) {
            event.setDamageMultiplier(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // Smite: blows against the undead strike twice as hard.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != target && fullSet(attacker)
                && CombatUtil.isDirectMelee(event.getSource())) {
            if (target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) {
                event.setAmount(event.getAmount() * 2.0F);
            }
            Fx.burst(attacker.serverLevel(), ModParticles.SPARKLE.get(), COLOR, 1.2F, target.getBoundingBox().getCenter(), 6, 0.3, 0.05);
        }
        if (!(target instanceof ServerPlayer player) || !fullSet(player)) {
            return;
        }
        event.setAmount(event.getAmount() * 0.7F);
        // Divine Aegis: a blow that would leave you near death restores you instead.
        long now = player.serverLevel().getGameTime();
        if (player.getHealth() - event.getAmount() < player.getMaxHealth() * 0.25F && now >= AEGIS.getOrDefault(player.getUUID(), 0L)) {
            AEGIS.put(player.getUUID(), now + 1200);
            event.setAmount(0);
            player.setHealth(player.getMaxHealth());
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 3));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
            ServerLevel level = player.serverLevel();
            Shockwaves.ring(level, player, player.position(), 9.0F, 1.0F, 20.0F, 1.0, COLOR, null);
            Fx.impact(level, player.position().add(0, 1, 0), 0xFFFFFF, 3.0F);
            Fx.ring(level, player.position(), COLOR, 9.0F);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 60, 1.2, 1.5, 1.2, 0.15);
            Fx.sound(level, player.position(), ModSounds.ULTIMATE_READY, 1.5F, 0.8F);
            Fx.sound(level, player.position(), ModSounds.ABILITY_EPIC_IMPACT, 1.2F, 1.4F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.steelstorm.divine_aegis")
                    .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD), true);
        }
    }

    private CelestialAbilities() {
    }
}

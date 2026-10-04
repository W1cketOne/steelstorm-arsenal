package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.WarlordArmorItem;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingFallEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.player.PlayerEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.PlayerTickEvent;

/**
 * Ember Warlord armour. Helmet: Infernal Eyes (night vision, no blindness or darkness). Chestplate:
 * Magma Heart (melee attackers are scorched and ignited). Leggings: Scorched Path (sprinting into
 * enemies bowls them aside in flames). Boots: Meteor Fall (sneak in mid-air to slam down in a fiery
 * shockwave). Full set: immune to fire and lava, and your attacks set enemies ablaze.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class WarlordAbilities {
    private static final Set<UUID> SLAMMING = new HashSet<>();
    private static final Map<UUID, Long> BOWLED = new HashMap<>();

    public static boolean wearing(LivingEntity e, ArmorItem.Type type) {
        return e.getItemBySlot(type.getSlot()).getItem() instanceof WarlordArmorItem;
    }

    public static boolean fullSet(LivingEntity e) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof WarlordArmorItem)) {
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
        }
        boolean full = fullSet(player);
        if (full) {
            if (player.tickCount % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            }
            if (player.tickCount % 12 == 0) {
                level.sendParticles(ParticleTypes.SMALL_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.3, 0.1, 0.3, 0.01);
            }
        }
        // Scorched Path.
        if (player.isSprinting() && wearing(player, ArmorItem.Type.LEGGINGS)) {
            if (player.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.02, 0.2, 0.01);
            }
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            AABB box = player.getBoundingBox().inflate(0.6).move(look.scale(0.6));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> Shockwaves.canHit(player, e))) {
                long now = level.getGameTime();
                if (now < BOWLED.getOrDefault(e.getUUID(), 0L)) {
                    continue;
                }
                if (BOWLED.size() > 256) {
                    BOWLED.values().removeIf(t -> t < now);
                }
                BOWLED.put(e.getUUID(), now + 15);
                Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1).normalize();
                e.setDeltaMovement(away.x * 1.1 + look.x * 0.4, 0.45, away.z * 1.1 + look.z * 0.4);
                e.hurtMarked = true;
                e.hurt(player.damageSources().playerAttack(player), 3.0F);
                e.igniteForSeconds(3);
                Fx.burst(level, ModParticles.GLOW.get(), 0xFF7A14, 1.3F, e.getBoundingBox().getCenter(), 8, 0.3, 0.08);
                Fx.sound(level, e.position(), ModSounds.ABILITY_INFERNO, 0.6F, 1.4F);
            }
        }
        // Meteor Fall: sneaking in mid-air drives you down.
        if (wearing(player, ArmorItem.Type.BOOTS) && player.isShiftKeyDown() && !player.onGround() && !player.isInWater()
                && !player.getAbilities().flying && player.fallDistance > 0.6F && !SLAMMING.contains(player.getUUID())) {
            SLAMMING.add(player.getUUID());
            player.setDeltaMovement(player.getDeltaMovement().x * 0.3, -2.2, player.getDeltaMovement().z * 0.3);
            player.hurtMarked = true;
            Fx.sound(level, player.position(), ModSounds.ABILITY_DASH, 1.0F, 0.6F);
        }
        if (SLAMMING.contains(player.getUUID())) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 4, 0.25, 0.4, 0.25, 0.02);
            if (player.onGround() || player.isInWater()) {
                land(player, Math.max(4.0F, player.fallDistance));
            }
        }
    }

    private static void land(ServerPlayer player, float height) {
        SLAMMING.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        float radius = Math.min(7.0F, 3.0F + height * 0.25F);
        float damage = Math.min(14.0F, 4.0F + height * 0.6F);
        Shockwaves.ring(level, player, player.position(), radius, 0.9F, damage, 0.5, 0xFF7A14, e -> e.igniteForSeconds(4));
        Fx.impact(level, player.position().add(0, 0.2, 0), 0xFF7A14, 2.2F);
        Fx.cracks(level, player.position(), 0xFF5A10, radius * 0.8F);
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.2, player.getZ(), 14, radius * 0.3, 0.1, radius * 0.3, 0.1);
        Fx.sound(level, player.position(), ModSounds.ABILITY_EPIC_IMPACT, 1.3F, 0.9F);
        Fx.sound(level, player.position(), ModSounds.ABILITY_INFERNO, 1.0F, 0.8F);
        Stamina.shake(player, 0.8F, 10);
        player.fallDistance = 0;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SLAMMING.contains(player.getUUID())) {
            land(player, event.getDistance());
            event.setDamageMultiplier(0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // Full set: fire and lava can't hurt you.
        if (target instanceof Player p && event.getSource().is(DamageTypeTags.IS_FIRE) && fullSet(p)) {
            event.setCanceled(true);
            return;
        }
        // Magma Heart.
        if (target instanceof ServerPlayer p && wearing(p, ArmorItem.Type.CHESTPLATE) && CombatUtil.isDirectMelee(event.getSource())
                && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != p) {
            attacker.igniteForSeconds(4);
            attacker.invulnerableTime = 0;
            attacker.hurt(p.damageSources().onFire(), 2.0F);
            Fx.burst(p.serverLevel(), ModParticles.GLOW.get(), 0xFF7A14, 1.2F, attacker.getBoundingBox().getCenter(), 10, 0.3, 0.06);
        }
        // Full set: your blows set enemies alight.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != target && fullSet(attacker)
                && CombatUtil.isDirectMelee(event.getSource())) {
            target.igniteForSeconds(3);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SLAMMING.remove(event.getEntity().getUUID());
    }

    private WarlordAbilities() {
    }
}

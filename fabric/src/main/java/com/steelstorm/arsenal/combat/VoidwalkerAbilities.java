package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.VoidwalkerArmorItem;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
 * Voidwalker armour. Helmet: Void Sight (client-side outlines on invisible things; endermen ignore
 * your gaze). Chestplate: Phase Shift (a chance to blink out of a hit). Leggings: Shadow Step
 * (invisible while sneaking). Boots: Void Blink (teleport forward from mid-air). Full set: no fall
 * damage.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class VoidwalkerAbilities {
    private static final int VOID = 0xB15CFF;
    private static final Map<UUID, Long> PHASE_READY = new HashMap<>();
    private static final Map<UUID, Boolean> BLINKED = new HashMap<>();

    public static boolean wearing(LivingEntity e, ArmorItem.Type type) {
        return e.getItemBySlot(type.getSlot()).getItem() instanceof VoidwalkerArmorItem;
    }

    public static boolean fullSet(LivingEntity e) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof VoidwalkerArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /** Teleports up to 7 blocks along the player's look, stopping short of walls. */
    public static void blink(ServerPlayer player) {
        if (!wearing(player, ArmorItem.Type.BOOTS) || player.onGround() || player.isSpectator()
                || Boolean.TRUE.equals(BLINKED.get(player.getUUID()))) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle().multiply(1, 0.35, 1).normalize();
        Vec3 from = player.position();
        Vec3 target = null;
        for (double d = 7.0; d >= 1.5; d -= 0.5) {
            Vec3 at = from.add(dir.scale(d));
            AABB box = player.getDimensions(player.getPose()).makeBoundingBox(at);
            if (level.noCollision(player, box) && level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                    at.add(0, player.getEyeHeight(), 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                target = at;
                break;
            }
        }
        if (target == null) {
            return;
        }
        BLINKED.put(player.getUUID(), true);
        trail(level, from, target);
        player.teleportTo(target.x, target.y, target.z);
        player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1).add(0, 0.25, 0));
        player.hurtMarked = true;
        player.fallDistance = 0;
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
        Fx.sound(level, target, ModSounds.ABILITY_VOID, 0.6F, 1.6F);
    }

    private static void trail(ServerLevel level, Vec3 a, Vec3 b) {
        for (int i = 0; i <= 12; i++) {
            Vec3 p = a.lerp(b, i / 12.0).add(0, 1, 0);
            Fx.burst(level, ModParticles.GLOW.get(), VOID, 1.2F, p, 2, 0.15, 0.02);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, b.x, b.y + 1, b.z, 30, 0.3, 0.6, 0.3, 0.05);
        level.sendParticles(ParticleTypes.PORTAL, a.x, a.y + 1, a.z, 30, 0.3, 0.6, 0.3, 0.3);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.onGround() || player.isInWater()) {
            BLINKED.remove(player.getUUID());
        }
        if (player.isCrouching() && wearing(player, ArmorItem.Type.LEGGINGS)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, true, false, true));
            if (player.tickCount % 6 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.3, player.getZ(), 1, 0.3, 0.2,
                        0.3, 0.01);
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !wearing(player, ArmorItem.Type.CHESTPLATE)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || event.getAmount() < 2.0F) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < PHASE_READY.getOrDefault(player.getUUID(), 0L) || player.getRandom().nextFloat() > 0.3F) {
            return;
        }
        ServerLevel level = player.serverLevel();
        for (int tries = 0; tries < 8; tries++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            Vec3 at = player.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
            if (level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(at))
                    && !level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(at.add(0, -0.6, 0)))) {
                event.setCanceled(true);
                PHASE_READY.put(player.getUUID(), now + 160);
                trail(level, player.position(), at);
                player.teleportTo(at.x, at.y, at.z);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && fullSet(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BLINKED.remove(event.getEntity().getUUID());
        PHASE_READY.remove(event.getEntity().getUUID());
    }

    private VoidwalkerAbilities() {
    }
}

package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.compat.neo.bus.api.EventPriority;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.player.PlayerEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;

/**
 * Players are untouchable while they charge and unleash an ultimate (weapon or suit): no damage
 * and no knockback, from anything short of /kill or the void.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class UltGuard {
    private static final Map<UUID, Long> UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CHARGING = new ConcurrentHashMap<>();

    /** Protects the player for the next `ticks` ticks (extends, never shortens). */
    public static void protect(ServerPlayer player, int ticks) {
        long until = player.level().getGameTime() + ticks;
        UNTIL.merge(player.getUUID(), until, Math::max);
    }

    public static void setCharging(ServerPlayer player, boolean charging) {
        if (charging) {
            CHARGING.put(player.getUUID(), Boolean.TRUE);
        } else {
            CHARGING.remove(player.getUUID());
        }
    }

    public static boolean isProtected(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        if (CHARGING.containsKey(player.getUUID())) {
            return true;
        }
        Long until = UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (isProtected(event.getEntity()) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer p) {
                p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY(1.0), p.getZ(), 6, 0.4, 0.5, 0.4, 0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UNTIL.remove(event.getEntity().getUUID());
        CHARGING.remove(event.getEntity().getUUID());
    }

    private UltGuard() {
    }
}

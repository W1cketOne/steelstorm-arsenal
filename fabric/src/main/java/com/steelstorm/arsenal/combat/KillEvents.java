package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.network.KillStreakPayload;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.Mastery;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingDeathEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.steelstorm.compat.neo.neoforge.network.PacketDistributor;

/** What happens when you kill something: a burst of light, kill streaks, and weapon mastery. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class KillEvents {
    private static final int STREAK_WINDOW = 80;
    private record Streak(int count, long last) {
    }

    private static final Map<UUID, Streak> STREAKS = new HashMap<>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || dead == player || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean weapon = stack.getItem() instanceof WeaponItem;
        int color = weapon ? WeaponLooks.trailColor(stack) : Fx.STEEL;
        Vec3 c = dead.getBoundingBox().getCenter();
        // The body bursts into light and its soul drifts up.
        Fx.sparkles(level, c, color, 14 + (int) Math.min(30, dead.getMaxHealth() / 2), dead.getBbWidth() * 0.6);
        Fx.orbs(level, c, color, 4, 0.4);
        level.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 6, 0.3, 0.4, 0.3, 0.04);
        if (dead.getMaxHealth() >= 40) {
            Fx.sunburst(level, c, color, Fx.WHITE, 4.0F, 12, 14);
            Fx.halo(level, dead.position(), color, Fx.WHITE, 3.5F, 14);
        }
        // Kill streaks.
        long now = level.getGameTime();
        Streak s = STREAKS.get(player.getUUID());
        int count = s != null && now - s.last() <= STREAK_WINDOW ? s.count() + 1 : 1;
        STREAKS.put(player.getUUID(), new Streak(count, now));
        if (count >= 2) {
            PacketDistributor.sendToPlayer(player, new KillStreakPayload(count));
            if (count >= 4) {
                Fx.halo(level, player.position(), Fx.GOLD, color, 2.5F + count * 0.3F, 12);
            }
        }
        // Weapon mastery.
        if (weapon) {
            int before = Mastery.kills(stack);
            int after = before + 1;
            stack.set(ModDataComponents.KILLS.get(), after);
            int rank = Mastery.rank(after);
            if (rank > Mastery.rank(before)) {
                PacketDistributor.sendToPlayer(player, new KillStreakPayload(-rank));
                Fx.pillar(level, player.position(), color, Fx.GOLD, 1.0F, 10.0F, 30);
                Fx.halo(level, player.position(), Fx.GOLD, color, 6.0F, 20);
                Fx.sparkles(level, player.position().add(0, 1, 0), Fx.GOLD, 40, 1.0);
                Fx.sound(level, player.position(), ModSounds.ULTIMATE_READY, 1.0F, 0.8F);
            }
        }
    }

    /** Mastery adds 3% damage per rank to hits made with the weapon. */
    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player && player.getMainHandItem().getItem() instanceof WeaponItem) {
            float mult = Mastery.damageMultiplier(player.getMainHandItem());
            if (mult > 1) {
                event.setAmount(event.getAmount() * mult);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(com.steelstorm.compat.neo.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        STREAKS.remove(event.getEntity().getUUID());
    }

    private KillEvents() {
    }
}

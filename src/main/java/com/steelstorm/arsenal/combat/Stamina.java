package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.network.CombatSyncPayload;
import com.steelstorm.arsenal.network.FeedbackPayload;
import com.steelstorm.arsenal.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side stamina rules. The client only ever displays what this class syncs to it. */
public final class Stamina {
    public static CombatData data(Player player) {
        return player.getData(ModAttachments.COMBAT);
    }

    public static float max() {
        return Config.MAX_STAMINA.get().floatValue();
    }

    public static boolean has(Player player, float amount) {
        return player.getAbilities().instabuild || data(player).stamina >= amount;
    }

    /** Spends stamina if the player has enough. Creative players never run out. */
    public static boolean tryConsume(Player player, float amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        CombatData data = data(player);
        if (data.stamina < amount) {
            return false;
        }
        data.stamina -= amount;
        data.lastStaminaUse = player.level().getGameTime();
        return true;
    }

    /** Spends as much as possible; returns how much was actually spent. */
    public static float drain(Player player, float amount) {
        if (player.getAbilities().instabuild) {
            return amount;
        }
        CombatData data = data(player);
        float spent = Math.min(amount, data.stamina);
        data.stamina -= spent;
        data.lastStaminaUse = player.level().getGameTime();
        return spent;
    }

    public static void restore(Player player, float amount) {
        CombatData data = data(player);
        data.stamina = Math.min(max(), data.stamina + amount);
    }

    public static void tick(ServerPlayer player) {
        CombatData data = data(player);
        long now = player.level().getGameTime();
        float max = max();
        if (data.stamina > max) {
            data.stamina = max;
        }
        if (data.stamina < max && now - data.lastStaminaUse >= Config.STAMINA_REGEN_DELAY.get()) {
            data.stamina = Math.min(max, data.stamina + Config.STAMINA_REGEN_PER_TICK.get().floatValue());
        }
        if (data.combo > 0 && now - data.lastHitTime > CombatData.COMBO_TIMEOUT) {
            data.combo = 0;
        }
        sync(player, false);
    }

    /** Sends HUD state when something visible changed (or always when {@code force}). */
    public static void sync(ServerPlayer player, boolean force) {
        CombatData data = data(player);
        int shown = (int) Math.floor(data.stamina);
        if (!force && shown == data.syncedStamina && data.combo == data.syncedCombo
                && data.specialCooldownEnd == data.syncedSpecialEnd && data.dodgeCooldownEnd == data.syncedDodgeEnd) {
            return;
        }
        data.syncedStamina = shown;
        data.syncedCombo = data.combo;
        data.syncedSpecialEnd = data.specialCooldownEnd;
        data.syncedDodgeEnd = data.dodgeCooldownEnd;
        long now = player.level().getGameTime();
        PacketDistributor.sendToPlayer(player, new CombatSyncPayload(data.stamina, max(),
                (int) Math.max(0, data.specialCooldownEnd - now), data.specialCooldownTotal,
                (int) Math.max(0, data.dodgeCooldownEnd - now), data.combo));
    }

    public static void shake(Player player, float strength, int ticks) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FeedbackPayload(strength, ticks));
        }
    }

    private Stamina() {
    }
}

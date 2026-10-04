package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.ability.Abilities;
import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.network.CombatSyncPayload;
import com.steelstorm.arsenal.network.FeedbackPayload;
import com.steelstorm.arsenal.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.steelstorm.compat.neo.neoforge.network.PacketDistributor;

/** Server-side stamina rules. The client only ever displays what this class syncs to it. */
public final class Stamina {
    public static CombatData data(Player player) {
        return player.getAttachedOrCreate(ModAttachments.COMBAT);
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
        long now = player.level().getGameTime();
        AbilitySet set = Abilities.forStack(player.getMainHandItem());
        String setId = set == null ? "" : set.id();
        int shown = (int) Math.floor(data.stamina);
        int ultimate = (int) Math.floor(data.ultimate);
        long cooldownHash = 17;
        if (set != null) {
            for (int slot = 0; slot < AbilitySet.SLOTS; slot++) {
                Ability a = set.get(slot);
                if (a.charges() > 1) {
                    data.chargesLeft(a, now);
                }
                cooldownHash = cooldownHash * 31 + data.cooldownEnd.getOrDefault(a.id(), 0L);
                cooldownHash = cooldownHash * 31 + data.chargesUsed.getOrDefault(a.id(), 0);
            }
        }
        if (!force && shown == data.syncedStamina && data.combo == data.syncedCombo && ultimate == data.syncedUltimate
                && data.dodgeCooldownEnd == data.syncedDodgeEnd && setId.equals(data.syncedSet) && cooldownHash == data.syncedCooldownHash) {
            return;
        }
        data.syncedStamina = shown;
        data.syncedCombo = data.combo;
        data.syncedUltimate = ultimate;
        data.syncedDodgeEnd = data.dodgeCooldownEnd;
        data.syncedSet = setId;
        data.syncedCooldownHash = cooldownHash;
        List<Integer> left = new ArrayList<>(AbilitySet.SLOTS);
        List<Integer> total = new ArrayList<>(AbilitySet.SLOTS);
        List<Integer> charges = new ArrayList<>(AbilitySet.SLOTS);
        if (set != null) {
            for (int slot = 0; slot < AbilitySet.SLOTS; slot++) {
                Ability ability = set.get(slot);
                left.add((int) data.cooldownLeft(ability.id(), now));
                total.add(data.cooldownTotal.getOrDefault(ability.id(), ability.cooldown()));
                charges.add(ability.charges() - data.chargesUsed.getOrDefault(ability.id(), 0));
            }
        }
        PacketDistributor.sendToPlayer(player, new CombatSyncPayload(data.stamina, max(), data.ultimate, data.combo,
                (int) Math.max(0, data.dodgeCooldownEnd - now), setId, left, total, charges));
    }

    public static void shake(Player player, float strength, int ticks) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new FeedbackPayload(strength, ticks));
        }
    }

    private Stamina() {
    }
}

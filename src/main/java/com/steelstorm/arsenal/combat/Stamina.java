package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormConfig;
import com.steelstorm.arsenal.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/** Stamina rules. Creative and spectator players never run out. */
public final class Stamina {
    private Stamina() {
    }

    public static float max() {
        return SteelstormConfig.MAX_STAMINA.get().floatValue();
    }

    public static float get(Player player) {
        return Math.min(player.getData(ModAttachments.STAMINA), max());
    }

    public static void set(Player player, float value) {
        float clamped = Math.max(0.0f, Math.min(max(), value));
        if (clamped != player.getData(ModAttachments.STAMINA)) {
            player.setData(ModAttachments.STAMINA, clamped);
            CombatState.of(player).dirty = true;
        }
    }

    public static boolean has(Player player, float cost) {
        return player.getAbilities().instabuild || get(player) >= cost;
    }

    /** Spends stamina if there is enough. Spending always pauses regeneration. */
    public static boolean tryConsume(Player player, float cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        float current = get(player);
        if (current < cost) {
            return false;
        }
        set(player, current - cost);
        CombatState.of(player).regenDelay = SteelstormConfig.REGEN_DELAY.get();
        return true;
    }

    /** Spends up to {@code cost}; returns how much was actually available. */
    public static float drain(Player player, float cost) {
        if (player.getAbilities().instabuild) {
            return cost;
        }
        float current = get(player);
        float spent = Math.min(current, cost);
        set(player, current - spent);
        CombatState.of(player).regenDelay = SteelstormConfig.REGEN_DELAY.get();
        return spent;
    }

    public static void tick(Player player, CombatState state) {
        if (state.regenDelay > 0) {
            state.regenDelay--;
            return;
        }
        float current = get(player);
        if (current < max()) {
            set(player, current + SteelstormConfig.REGEN_PER_TICK.get().floatValue());
        }
    }
}

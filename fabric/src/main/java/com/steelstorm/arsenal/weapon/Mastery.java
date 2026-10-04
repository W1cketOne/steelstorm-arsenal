package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

/** Weapon mastery: kills made with a weapon raise it through five ranks, each adding 3% damage. */
public final class Mastery {
    public static final int[] THRESHOLDS = {10, 40, 100, 250, 500};
    public static final String[] NAMES = {"Unproven", "Blooded", "Veteran", "Champion", "Legend", "Mythic"};
    public static final ChatFormatting[] COLORS = {ChatFormatting.GRAY, ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.AQUA,
            ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD};

    public static int kills(ItemStack stack) {
        Integer k = stack.get(ModDataComponents.KILLS.get());
        return k == null ? 0 : k;
    }

    public static int rank(int kills) {
        int r = 0;
        while (r < THRESHOLDS.length && kills >= THRESHOLDS[r]) {
            r++;
        }
        return r;
    }

    public static float damageMultiplier(ItemStack stack) {
        return 1.0F + 0.03F * rank(kills(stack));
    }

    private Mastery() {
    }
}

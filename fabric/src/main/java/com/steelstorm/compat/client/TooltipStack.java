package com.steelstorm.compat.client;

import net.minecraft.world.item.ItemStack;

/** The stack whose tooltip is being drawn, for RenderTooltipEvent.Color. */
public final class TooltipStack {
    private static ItemStack current = ItemStack.EMPTY;

    public static void set(ItemStack stack) {
        current = stack;
    }

    public static ItemStack get() {
        return current;
    }

    private TooltipStack() {
    }
}

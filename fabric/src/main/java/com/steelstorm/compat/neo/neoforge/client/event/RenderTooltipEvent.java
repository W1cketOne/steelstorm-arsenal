package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.item.ItemStack;

public abstract class RenderTooltipEvent extends Event {
    private final ItemStack stack;
    protected RenderTooltipEvent(ItemStack stack) { this.stack = stack; }
    public ItemStack getItemStack() { return stack; }
    public static class Color extends RenderTooltipEvent {
        private int backgroundStart, backgroundEnd, borderStart, borderEnd;
        public Color(ItemStack stack, int background, int borderStart, int borderEnd) {
            super(stack); this.backgroundStart = background; this.backgroundEnd = background; this.borderStart = borderStart; this.borderEnd = borderEnd;
        }
        public int getBackgroundStart() { return backgroundStart; }
        public int getBackgroundEnd() { return backgroundEnd; }
        public int getBorderStart() { return borderStart; }
        public int getBorderEnd() { return borderEnd; }
        public void setBackgroundStart(int c) { backgroundStart = c; }
        public void setBackgroundEnd(int c) { backgroundEnd = c; }
        public void setBorderStart(int c) { borderStart = c; }
        public void setBorderEnd(int c) { borderEnd = c; }
    }
}

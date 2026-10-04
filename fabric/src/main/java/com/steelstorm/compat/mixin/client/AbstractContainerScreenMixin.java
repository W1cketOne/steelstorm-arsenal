package com.steelstorm.compat.mixin.client;

import com.steelstorm.compat.client.TooltipStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void steelstorm$tooltipStart(GuiGraphics g, int x, int y, CallbackInfo ci) {
        TooltipStack.set(hoveredSlot != null ? hoveredSlot.getItem() : ItemStack.EMPTY);
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void steelstorm$tooltipEnd(GuiGraphics g, int x, int y, CallbackInfo ci) {
        TooltipStack.set(ItemStack.EMPTY);
    }
}

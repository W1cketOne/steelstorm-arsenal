package com.steelstorm.compat.mixin.client;

import com.steelstorm.compat.client.TooltipStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void steelstorm$tooltipStart(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStack.set(stack);
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
    private void steelstorm$tooltipEnd(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStack.set(ItemStack.EMPTY);
    }

}

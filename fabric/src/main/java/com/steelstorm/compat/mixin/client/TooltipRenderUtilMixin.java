package com.steelstorm.compat.mixin.client;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.client.TooltipStack;
import com.steelstorm.compat.neo.neoforge.client.event.RenderTooltipEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets RenderTooltipEvent.Color recolour item tooltips. */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipRenderUtilMixin {
    @Inject(method = "renderTooltipBackground", at = @At("HEAD"), cancellable = true)
    private static void steelstorm$tooltipBackground(GuiGraphics g, int x, int y, int width, int height, int z, CallbackInfo ci) {
        ItemStack stack = TooltipStack.get();
        if (stack.isEmpty()) {
            return;
        }
        ci.cancel();
        RenderTooltipEvent.Color c = NeoBus.post(new RenderTooltipEvent.Color(stack, 0xF0100010, 0x505000FF, 0x5028007F));
        int i = x - 3;
        int j = y - 3;
        int k = width + 6;
        int l = height + 6;
        g.fillGradient(i, j - 1, i + k, j, z, c.getBackgroundStart(), c.getBackgroundStart());
        g.fillGradient(i, j + l, i + k, j + l + 1, z, c.getBackgroundEnd(), c.getBackgroundEnd());
        g.fillGradient(i, j, i + k, j + l, z, c.getBackgroundStart(), c.getBackgroundEnd());
        g.fillGradient(i - 1, j, i, j + l, z, c.getBackgroundStart(), c.getBackgroundEnd());
        g.fillGradient(i + k, j, i + k + 1, j + l, z, c.getBackgroundStart(), c.getBackgroundEnd());
        g.fillGradient(i, j + 1, i + 1, j + l - 1, z, c.getBorderStart(), c.getBorderEnd());
        g.fillGradient(i + k - 1, j + 1, i + k, j + l - 1, z, c.getBorderStart(), c.getBorderEnd());
        g.fillGradient(i, j, i + k, j + 1, z, c.getBorderStart(), c.getBorderStart());
        g.fillGradient(i, j + l - 1, i + k, j + l, z, c.getBorderEnd(), c.getBorderEnd());
    }
}

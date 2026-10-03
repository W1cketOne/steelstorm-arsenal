package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import com.steelstorm.arsenal.item.WarlordArmorItem;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** Weapon and armour tooltips take on the item's colour: a tinted dark panel with a softly pulsing border. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class TooltipTheme {
    @SubscribeEvent
    public static void onColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        int color;
        if (stack.getItem() instanceof WeaponItem) {
            color = WeaponLooks.trailColor(stack);
        } else if (stack.getItem() instanceof StormsteelArmorItem) {
            color = 0x5FB8FF;
        } else if (stack.getItem() instanceof WarlordArmorItem) {
            color = 0xFF6A1A;
        } else {
            return;
        }
        boolean legendary = stack.getItem() instanceof LegendaryWeaponItem;
        float time = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
        float pulse = 0.75F + 0.25F * Mth.sin(time * (legendary ? 0.25F : 0.12F));
        int bright = scale(color, pulse);
        int dim = scale(color, 0.35F * pulse);
        event.setBorderStart(0xFF000000 | bright);
        event.setBorderEnd(0xFF000000 | dim);
        event.setBackgroundStart(0xF0000000 | scale(color, 0.10F));
        event.setBackgroundEnd(0xF0000000 | scale(color, 0.04F));
    }

    private static int scale(int rgb, float f) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * f));
        int g = Math.min(255, (int) ((rgb >> 8 & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return r << 16 | g << 8 | b;
    }

    private TooltipTheme() {
    }
}

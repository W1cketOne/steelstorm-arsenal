package com.steelstorm.arsenal.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Stormsteel armour. Wearing all four pieces gives the Stormcaller set bonus (see ArmorEffects). */
public class StormsteelArmorItem extends ArmorItem {
    public StormsteelArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.steelstorm.stormsteel_set").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.steelstorm.stormsteel_set.desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}

package com.steelstorm.arsenal.item;

import com.steelstorm.arsenal.SteelstormArsenal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Stormsteel armour. Wearing all four pieces gives the Stormcaller set bonus (see ArmorEffects). */
public class StormsteelArmorItem extends ArmorItem {
    public StormsteelArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    /** Textures for the 3D model (see client.StormsteelArmorRendering): one for the legs, one for the rest. */
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return SteelstormArsenal.id("textures/models/armor/stormsteel_3d_" + (innerModel ? "inner" : "outer") + ".png");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String piece = getType().getName();
        tooltip.add(Component.translatable("tooltip.steelstorm.armor." + piece).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.steelstorm.armor." + piece + ".desc").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.steelstorm.stormsteel_set").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.steelstorm.stormsteel_set.desc").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.steelstorm.suit_ult.stormsteel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }
}

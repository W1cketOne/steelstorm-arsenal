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

/** Dragonscale armour: crimson scale plate with horns and wings. See DragonscaleAbilities. */
public class DragonscaleArmorItem extends ArmorItem {
    public DragonscaleArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return SteelstormArsenal.id("textures/models/armor/dragonscale_3d_" + (innerModel ? "inner" : "outer") + ".png");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String piece = getType().getName();
        tooltip.add(Component.translatable("tooltip.steelstorm.dragonscale." + piece).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.steelstorm.dragonscale." + piece + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.steelstorm.dragonscale_set").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.steelstorm.dragonscale_set.desc").withStyle(ChatFormatting.GRAY));
    }
}

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

/** Celestial armour: blessed white-and-gold plate with a halo and seraph wings. See CelestialAbilities. */
public class CelestialArmorItem extends ArmorItem {
    public CelestialArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return SteelstormArsenal.id("textures/models/armor/celestial_3d_" + (innerModel ? "inner" : "outer") + ".png");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String piece = getType().getName();
        tooltip.add(Component.translatable("tooltip.steelstorm.celestial." + piece).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.steelstorm.celestial." + piece + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.steelstorm.celestial_set").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.steelstorm.celestial_set.desc").withStyle(ChatFormatting.GRAY));
    }
}

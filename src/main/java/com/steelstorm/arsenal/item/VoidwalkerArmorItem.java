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

/** Voidwalker armour: dark plate grown through with amethyst. See VoidwalkerAbilities for what each piece does. */
public class VoidwalkerArmorItem extends ArmorItem {
    public VoidwalkerArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return SteelstormArsenal.id("textures/models/armor/voidwalker_3d_" + (innerModel ? "inner" : "outer") + ".png");
    }

    /** Endermen don't mind being looked at by someone wearing the Voidwalker helm. */
    @Override
    public boolean isEnderMask(ItemStack stack, net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.monster.EnderMan enderMan) {
        return getType() == Type.HELMET;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String piece = getType().getName();
        tooltip.add(Component.translatable("tooltip.steelstorm.voidwalker." + piece).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.steelstorm.voidwalker." + piece + ".desc").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.steelstorm.voidwalker_set").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.steelstorm.voidwalker_set.desc").withStyle(ChatFormatting.DARK_PURPLE));
    }
}

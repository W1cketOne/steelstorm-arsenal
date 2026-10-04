package com.steelstorm.arsenal.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A plain item with a one-line hint in its tooltip (the Vault Key, for example). */
public class HintItem extends Item {
    private final String hintKey;

    public HintItem(String hintKey, Properties properties) {
        super(properties);
        this.hintKey = hintKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey).withStyle(ChatFormatting.GRAY));
    }
}

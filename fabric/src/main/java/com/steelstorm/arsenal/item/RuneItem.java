package com.steelstorm.arsenal.item;

import com.steelstorm.arsenal.weapon.Rune;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A rune waiting to be inscribed on a weapon at a Rune Forge. */
public class RuneItem extends Item {
    private final Rune rune;

    public RuneItem(Rune rune, Properties properties) {
        super(properties);
        this.rune = rune;
    }

    public Rune rune() {
        return rune;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(rune.effectKey()).withStyle(s -> s.withColor(TextColor.fromRgb(rune.color()))));
        tooltip.add(Component.translatable("tooltip.steelstorm.rune_use").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}

package com.steelstorm.arsenal.ability;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Lists a weapon's abilities and their keys in its tooltip. Hold Shift for the descriptions. */
public final class AbilityTooltips {
    /** Key mapping names for the four ability slots, in slot order. */
    public static final String[] KEYS = {"key.steelstorm.ability_1", "key.steelstorm.ability_2", "key.steelstorm.ability_3",
            "key.steelstorm.ultimate"};

    public static void append(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        AbilitySet set = Abilities.forStack(stack);
        if (set == null) {
            if (stack.getItem() instanceof com.steelstorm.arsenal.weapon.WeaponItem) {
                tooltip.add(Component.translatable("tooltip.steelstorm.no_abilities").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            return;
        }
        boolean details = com.steelstorm.compat.FabricHooks.shiftDown.getAsBoolean();
        tooltip.add(Component.translatable("tooltip.steelstorm.abilities").withStyle(ChatFormatting.AQUA));
        for (int slot = 0; slot < AbilitySet.SLOTS; slot++) {
            Ability ability = set.get(slot);
            ChatFormatting color = ability.isUltimate() ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            MutableComponent line = Component.literal(" [").append(Component.keybind(KEYS[slot])).append("] ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(ability.nameKey()).withStyle(color));
            String seconds = format(ability.cooldown() / 20.0F);
            if (ability.charges() > 1) {
                line.append(Component.translatable("tooltip.steelstorm.charges", ability.charges()).withStyle(ChatFormatting.BLUE));
            }
            if (ability.isUltimate()) {
                line.append(Component.translatable("tooltip.steelstorm.ultimate_cost", seconds).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                line.append(Component.translatable("tooltip.steelstorm.ability_cost", ability.staminaCost(), seconds)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(line);
            if (details) {
                tooltip.add(Component.literal("    ").append(Component.translatable(ability.descKey())).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        tooltip.add(Component.translatable("tooltip.steelstorm.hold_ultimate", Component.keybind(KEYS[3]))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        if (!details) {
            tooltip.add(Component.translatable("tooltip.steelstorm.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static String format(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private AbilityTooltips() {
    }
}

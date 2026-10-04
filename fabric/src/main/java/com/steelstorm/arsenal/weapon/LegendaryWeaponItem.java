package com.steelstorm.arsenal.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import com.steelstorm.compat.neo.neoforge.common.SimpleTier;

/** Uncraftable legendary weapons found in Ruined Colosseums and dropped by the Fallen Warlord. */
public class LegendaryWeaponItem extends WeaponItem {
    public static final Tier LEGENDARY_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 9.0F, 4.0F, 20,
            () -> Ingredient.of(Items.NETHERITE_INGOT));

    public enum Legendary {
        TEMPEST_EDGE("tempest_edge", "Tempest Edge", WeaponType.LONGSWORD),
        RIMECLEAVER("rimecleaver", "Rimecleaver", WeaponType.GREATSWORD),
        VOIDREAVER("voidreaver", "Voidreaver", WeaponType.SCYTHE),
        EARTHSHAKER("earthshaker", "Earthshaker", WeaponType.WARHAMMER),
        BLOODFANG("bloodfang", "Bloodfang", WeaponType.DUAL_DAGGERS),
        SKYPIERCER("skypiercer", "Skypiercer", WeaponType.SPEAR),
        MOONVEIL("moonveil", "Moonveil", WeaponType.KATANA),
        KINGSBANE("kingsbane", "Kingsbane", WeaponType.BATTLEAXE);

        private final String id;
        private final String displayName;
        private final WeaponType type;

        Legendary(String id, String displayName, WeaponType type) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public WeaponType type() {
            return type;
        }

        public String descKey() {
            return "weapon.steelstorm." + id + ".legendary";
        }
    }

    private final Legendary legendary;

    public LegendaryWeaponItem(Legendary legendary, Properties properties) {
        super(legendary.type(), LEGENDARY_TIER, null, 1.0F, properties.rarity(Rarity.EPIC).fireResistant());
        this.legendary = legendary;
    }

    public Legendary legendary() {
        return legendary;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    protected void appendExtraTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.steelstorm.legendary").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.translatable(legendary.descKey()).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}

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

/**
 * Uncraftable legendary weapons found in Ruined Colosseums and dropped by the Fallen Warlord, and
 * the Mythic weapons forged from them: absurdly strong, each with its own on-hit power and ultimate.
 */
public class LegendaryWeaponItem extends WeaponItem {
    public static final Tier LEGENDARY_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 9.0F, 4.0F, 20,
            () -> Ingredient.of(Items.NETHERITE_INGOT));
    public static final Tier MYTHIC_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 5000, 12.0F, 10.0F, 25,
            () -> Ingredient.of(Items.NETHER_STAR));

    public enum Legendary {
        TEMPEST_EDGE("tempest_edge", "Tempest Edge", WeaponType.LONGSWORD),
        RIMECLEAVER("rimecleaver", "Rimecleaver", WeaponType.GREATSWORD),
        VOIDREAVER("voidreaver", "Voidreaver", WeaponType.SCYTHE),
        EARTHSHAKER("earthshaker", "Earthshaker", WeaponType.WARHAMMER),
        BLOODFANG("bloodfang", "Bloodfang", WeaponType.DUAL_DAGGERS),
        SKYPIERCER("skypiercer", "Skypiercer", WeaponType.SPEAR),
        MOONVEIL("moonveil", "Moonveil", WeaponType.KATANA),
        KINGSBANE("kingsbane", "Kingsbane", WeaponType.BATTLEAXE),
        // Mythic: forged from a legendary of the same kind, a nether star and a fortune in netherite.
        SOLARIS("solaris", "Solaris", WeaponType.LONGSWORD, true),
        WORLDSPLITTER("worldsplitter", "Worldsplitter", WeaponType.GREATSWORD, true),
        ECLIPSE("eclipse", "Eclipse", WeaponType.KATANA, true),
        STARFALL("starfall", "Starfall", WeaponType.WARHAMMER, true),
        SOULREAPER("soulreaper", "Soulreaper", WeaponType.SCYTHE, true),
        VENOMFANG("venomfang", "Venomfang", WeaponType.DUAL_DAGGERS, true),
        DRAGONSPINE("dragonspine", "Dragonspine", WeaponType.SPEAR, true),
        TITANBREAKER("titanbreaker", "Titanbreaker", WeaponType.BATTLEAXE, true);

        private final String id;
        private final String displayName;
        private final WeaponType type;
        private final boolean mythic;

        Legendary(String id, String displayName, WeaponType type) {
            this(id, displayName, type, false);
        }

        Legendary(String id, String displayName, WeaponType type, boolean mythic) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
            this.mythic = mythic;
        }

        public boolean mythic() {
            return mythic;
        }

        /** The legendary a mythic weapon is forged from (itself for legendaries). */
        public Legendary base() {
            if (!mythic) {
                return this;
            }
            for (Legendary l : values()) {
                if (!l.mythic && l.type == type) {
                    return l;
                }
            }
            return this;
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
        super(legendary.type(), legendary.mythic() ? MYTHIC_TIER : LEGENDARY_TIER, null, legendary.mythic() ? 1.25F : 1.0F,
                properties.rarity(Rarity.EPIC).fireResistant());
        this.legendary = legendary;
    }

    public Legendary legendary() {
        return legendary;
    }

    /** No enchantment glint: it would wash a purple sheen over the glowing 3D models. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    protected void appendExtraTooltip(ItemStack stack, List<Component> tooltip) {
        if (legendary.mythic()) {
            tooltip.add(Component.translatable("tooltip.steelstorm.mythic").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.translatable(legendary.descKey()).withStyle(ChatFormatting.YELLOW)));
            return;
        }
        tooltip.add(Component.translatable("tooltip.steelstorm.legendary").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.translatable(legendary.descKey()).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}

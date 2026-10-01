package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Material tiers. Stormsteel sits between diamond and netherite. Enum order is the
 * progression order used by the creative tab.
 */
public enum WeaponTier {
    STONE("stone", Tiers.STONE, ChatFormatting.GRAY, Rarity.COMMON, false),
    IRON("iron", Tiers.IRON, ChatFormatting.WHITE, Rarity.COMMON, false),
    GOLD("golden", Tiers.GOLD, ChatFormatting.GOLD, Rarity.COMMON, false),
    DIAMOND("diamond", Tiers.DIAMOND, ChatFormatting.AQUA, Rarity.UNCOMMON, false),
    STORMSTEEL("stormsteel", Materials.STORMSTEEL, ChatFormatting.BLUE, Rarity.RARE, false),
    NETHERITE("netherite", Tiers.NETHERITE, ChatFormatting.DARK_PURPLE, Rarity.RARE, true);

    private final String id;
    private final Tier tier;
    private final ChatFormatting color;
    private final Rarity rarity;
    private final boolean fireResistant;

    WeaponTier(String id, Tier tier, ChatFormatting color, Rarity rarity, boolean fireResistant) {
        this.id = id;
        this.tier = tier;
        this.color = color;
        this.rarity = rarity;
        this.fireResistant = fireResistant;
    }

    public String id() {
        return id;
    }

    public Tier tier() {
        return tier;
    }

    public ChatFormatting color() {
        return color;
    }

    public Rarity rarity() {
        return rarity;
    }

    public boolean fireResistant() {
        return fireResistant;
    }

    public String translationKey() {
        return "weapon_tier.steelstorm." + id;
    }

    /** Custom tool tiers. Kept in a holder class so they exist before the enum constants. */
    public static final class Materials {
        /** Between diamond (3 dmg, 1561 uses, enchant 10) and netherite (4 dmg, 2031 uses, enchant 15). */
        public static final Tier STORMSTEEL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
                1800, 8.5f, 3.5f, 18, () -> Ingredient.of(ModItems.STORMSTEEL_INGOT.get()));

        private Materials() {
        }
    }
}

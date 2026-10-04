package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import com.steelstorm.compat.neo.neoforge.common.SimpleTier;

/**
 * The six material tiers every tiered weapon type is registered in.
 * Weapon stats are expressed relative to a vanilla sword of the same tier, so each tier
 * simply wraps the vanilla (or Stormsteel) {@link Tier} that supplies durability and damage bonus.
 */
public enum WeaponTier {
    STONE("stone", "Stone", Tiers.STONE, false),
    IRON("iron", "Iron", Tiers.IRON, false),
    GOLD("golden", "Golden", Tiers.GOLD, false),
    DIAMOND("diamond", "Diamond", Tiers.DIAMOND, false),
    NETHERITE("netherite", "Netherite", Tiers.NETHERITE, true),
    STORMSTEEL("stormsteel", "Stormsteel", ModTiers.STORMSTEEL, false);

    private final String prefix;
    private final String displayName;
    private final Tier tier;
    private final boolean fireResistant;

    WeaponTier(String prefix, String displayName, Tier tier, boolean fireResistant) {
        this.prefix = prefix;
        this.displayName = displayName;
        this.tier = tier;
        this.fireResistant = fireResistant;
    }

    public String prefix() {
        return prefix;
    }

    public String displayName() {
        return displayName;
    }

    public Tier tier() {
        return tier;
    }

    public boolean fireResistant() {
        return fireResistant;
    }

    public static final class ModTiers {
        /** Sits between diamond (3.0 bonus, 1561 uses) and netherite (4.0 bonus, 2031 uses). */
        public static final Tier STORMSTEEL = new SimpleTier(
                BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5F, 3.5F, 16,
                () -> Ingredient.of(ModItems.STORMSTEEL_INGOT.get()));

        private ModTiers() {
        }
    }
}

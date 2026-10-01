package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    /** Crafting grid patterns: M = tier material, S = stick. */
    static String[] pattern(WeaponType type) {
        return switch (type) {
            case LONGSWORD -> new String[]{"  M", " M ", "S  "};
            case GREATSWORD -> new String[]{" MM", "MMM", "SM "};
            case KATANA -> new String[]{"  M", " M ", "SS "};
            case DUAL_DAGGERS -> new String[]{"M M", "S S"};
            case SPEAR -> new String[]{"  M", " S ", "S  "};
        };
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        for (WeaponType type : WeaponType.values()) {
            for (WeaponTier tier : WeaponTier.values()) {
                Item result = ModItems.weapon(type, tier).get();
                if (tier == WeaponTier.NETHERITE) {
                    // Netherite gear is upgraded from diamond at a smithing table, like vanilla.
                    netheriteSmithing(output, ModItems.weapon(type, WeaponTier.DIAMOND).get(), RecipeCategory.COMBAT, result);
                    continue;
                }
                ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result);
                for (String row : pattern(type)) {
                    builder.pattern(row);
                }
                builder.define('S', Items.STICK);
                switch (tier) {
                    case STONE -> builder.define('M', Ingredient.of(ItemTags.STONE_TOOL_MATERIALS)).unlockedBy("has_stone", has(ItemTags.STONE_TOOL_MATERIALS));
                    case IRON -> builder.define('M', Items.IRON_INGOT).unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT));
                    case GOLD -> builder.define('M', Items.GOLD_INGOT).unlockedBy(getHasName(Items.GOLD_INGOT), has(Items.GOLD_INGOT));
                    case DIAMOND -> builder.define('M', Items.DIAMOND).unlockedBy(getHasName(Items.DIAMOND), has(Items.DIAMOND));
                    case STORMSTEEL -> builder.define('M', ModItems.STORMSTEEL_INGOT.get())
                            .unlockedBy(getHasName(ModItems.STORMSTEEL_INGOT.get()), has(ModItems.STORMSTEEL_INGOT.get()));
                    default -> throw new IllegalStateException("No crafting material for " + tier);
                }
                builder.save(output);
            }
        }
    }
}

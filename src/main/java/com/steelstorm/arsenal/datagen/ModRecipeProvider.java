package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        for (WeaponType type : WeaponType.values()) {
            for (WeaponTier tier : WeaponTier.values()) {
                WeaponItem weapon = ModItems.weapon(type, tier).get();
                if (tier == WeaponTier.NETHERITE) {
                    // Like vanilla gear: upgrade the diamond version at a smithing table.
                    SmithingTransformRecipeBuilder.smithing(
                                    Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                                    Ingredient.of(ModItems.weapon(type, WeaponTier.DIAMOND).get()),
                                    Ingredient.of(Items.NETHERITE_INGOT),
                                    RecipeCategory.COMBAT, weapon)
                            .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                            .save(output, SteelstormArsenal.id(tier.id() + "_" + type.id() + "_smithing"));
                    continue;
                }
                ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, weapon)
                        .define('S', Items.STICK)
                        .define('M', material(tier));
                for (String row : type.pattern()) {
                    builder.pattern(row);
                }
                switch (tier) {
                    case STONE -> builder.unlockedBy("has_stone", has(ItemTags.STONE_TOOL_MATERIALS));
                    case IRON -> builder.unlockedBy("has_iron", has(Items.IRON_INGOT));
                    case GOLD -> builder.unlockedBy("has_gold", has(Items.GOLD_INGOT));
                    case DIAMOND -> builder.unlockedBy("has_diamond", has(Items.DIAMOND));
                    default -> builder.unlockedBy("has_stormsteel", has(ModItems.STORMSTEEL_INGOT.get()));
                }
                builder.save(output);
            }
        }
    }

    private static Ingredient material(WeaponTier tier) {
        return switch (tier) {
            case STONE -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS);
            case IRON -> Ingredient.of(Items.IRON_INGOT);
            case GOLD -> Ingredient.of(Items.GOLD_INGOT);
            case DIAMOND -> Ingredient.of(Items.DIAMOND);
            case STORMSTEEL -> Ingredient.of(ModItems.STORMSTEEL_INGOT.get());
            case NETHERITE -> Ingredient.of(Items.NETHERITE_INGOT);
        };
    }
}

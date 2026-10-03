package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.Rune;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import java.util.List;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    /** Crafting grid patterns: M = tier material, S = stick. */
    private static void storage(RecipeOutput output, ItemLike item, ItemLike block) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                .pattern("###").pattern("###").pattern("###").define('#', item)
                .unlockedBy(getHasName(item), has(item)).save(output, SteelstormArsenal.id(getItemName(block)));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item, 9).requires(block)
                .unlockedBy(getHasName(block), has(block)).save(output, SteelstormArsenal.id(getItemName(item) + "_from_" + getItemName(block)));
    }

    static String[] pattern(WeaponType type) {
        return switch (type) {
            case LONGSWORD -> new String[]{"  M", " M ", "S  "};
            case GREATSWORD -> new String[]{" MM", "MMM", "SM "};
            case KATANA -> new String[]{"  M", " M ", "SS "};
            case DUAL_DAGGERS -> new String[]{"M M", "S S"};
            case SPEAR -> new String[]{"  M", " S ", "S  "};
            case WARHAMMER -> new String[]{"MMM", "MMM", " S "};
            case SCYTHE -> new String[]{"MMM", "  S", "  S"};
            case BATTLEAXE -> new String[]{"MSM", "MSM", " S "};
        };
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        for (WeaponType type : WeaponType.values()) {
            for (WeaponTier tier : WeaponTier.values()) {
                Item result = ModItems.weapon(type, tier).get();
                if (tier == WeaponTier.NETHERITE) {
                    // Netherite gear is upgraded from diamond at a smithing table, like vanilla.
                    SmithingTransformRecipeBuilder.smithing(
                                    Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                                    Ingredient.of(ModItems.weapon(type, WeaponTier.DIAMOND).get()),
                                    Ingredient.of(Items.NETHERITE_INGOT), RecipeCategory.COMBAT, result)
                            .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                            .save(output, SteelstormArsenal.id(type.itemId(tier) + "_smithing"));
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

        // Stormsteel processing.
        for (ItemLike input : List.<ItemLike>of(ModItems.STORMSTEEL_ORE.get(), ModItems.DEEPSLATE_STORMSTEEL_ORE.get(), ModItems.RAW_STORMSTEEL.get())) {
            String name = getItemName(input);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, ModItems.STORMSTEEL_INGOT.get(), 1.0F, 200)
                    .group("stormsteel_ingot").unlockedBy(getHasName(input), has(input))
                    .save(output, SteelstormArsenal.id("stormsteel_ingot_from_smelting_" + name));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, ModItems.STORMSTEEL_INGOT.get(), 1.0F, 100)
                    .group("stormsteel_ingot").unlockedBy(getHasName(input), has(input))
                    .save(output, SteelstormArsenal.id("stormsteel_ingot_from_blasting_" + name));
        }
        storage(output, ModItems.STORMSTEEL_INGOT.get(), ModItems.STORMSTEEL_BLOCK.get());
        storage(output, ModItems.RAW_STORMSTEEL.get(), ModItems.RAW_STORMSTEEL_BLOCK.get());

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CHAKRAM.get())
                .pattern(" I ").pattern("IGI").pattern(" I ")
                .define('I', Items.IRON_INGOT).define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.THROWING_KNIFE.get(), 4)
                .pattern(" I").pattern("S ")
                .define('I', Items.IRON_INGOT).define('S', Items.STICK)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.WEAPON_RACK.get())
                .pattern("SSS").pattern("S S").pattern("PPP")
                .define('S', Items.STICK).define('P', ItemTags.PLANKS)
                .unlockedBy("has_planks", has(ItemTags.PLANKS)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TARGET_DUMMY.get())
                .pattern(" C ").pattern("SHS").pattern(" S ")
                .define('C', Items.CARVED_PUMPKIN).define('H', Items.HAY_BLOCK).define('S', Items.STICK)
                .unlockedBy(getHasName(Items.HAY_BLOCK), has(Items.HAY_BLOCK)).save(output);

        // Interactive blocks.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.WHETSTONE.get())
                .pattern(" F ").pattern("PGP").pattern("P P")
                .define('F', Items.FLINT).define('G', Items.GRINDSTONE).define('P', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.GRINDSTONE), has(Items.GRINDSTONE)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.RUNE_FORGE.get())
                .pattern("AAA").pattern("ISI").pattern("OOO")
                .define('A', Items.AMETHYST_SHARD).define('I', Items.IRON_INGOT).define('S', Items.SMITHING_TABLE)
                .define('O', Items.POLISHED_BLACKSTONE)
                .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.SIGNAL_BRAZIER.get())
                .pattern("ICI").pattern("IBI").pattern(" B ")
                .define('I', Items.IRON_INGOT).define('C', ItemTags.COALS).define('B', Items.IRON_BARS)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.STORM_ALTAR.get())
                .pattern("DSD").pattern("SBS").pattern("DDD")
                .define('D', Items.POLISHED_DEEPSLATE).define('S', ModItems.STORMSTEEL_INGOT.get()).define('B', ModItems.STORMSTEEL_BLOCK.get())
                .unlockedBy(getHasName(ModItems.STORMSTEEL_INGOT.get()), has(ModItems.STORMSTEEL_INGOT.get())).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.LEGENDARY_PEDESTAL.get())
                .pattern("GGG").pattern(" Q ").pattern("QQQ")
                .define('G', Items.GOLD_INGOT).define('Q', Items.QUARTZ_BLOCK)
                .unlockedBy(getHasName(Items.QUARTZ_BLOCK), has(Items.QUARTZ_BLOCK)).save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.EXPLORERS_COMPASS.get())
                .pattern(" G ").pattern("GCG").pattern(" A ")
                .define('G', Items.GOLD_INGOT).define('C', Items.COMPASS).define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS)).save(output);

        // Runes: two amethyst shards, lapis, and the element.
        rune(output, Rune.EMBER, Items.BLAZE_POWDER);
        rune(output, Rune.FROST, Items.PACKED_ICE);
        rune(output, Rune.STORM, ModItems.STORMSTEEL_INGOT.get());
        rune(output, Rune.VENOM, Items.FERMENTED_SPIDER_EYE);
        rune(output, Rune.VAMPIRIC, Items.GHAST_TEAR);
        rune(output, Rune.GALE, Items.FEATHER);

        // Stormsteel armour.
        armor(output, ModItems.STORMSTEEL_HELMET.get(), "MMM", "M M");
        armor(output, ModItems.STORMSTEEL_CHESTPLATE.get(), "M M", "MMM", "MMM");
        armor(output, ModItems.STORMSTEEL_LEGGINGS.get(), "MMM", "M M", "M M");
        armor(output, ModItems.STORMSTEEL_BOOTS.get(), "M M", "M M");

        // Ember Warlord armour: a Stormsteel piece forged with netherite, blaze and magma.
        warlord(output, ModItems.WARLORD_HELMET.get(), ModItems.STORMSTEEL_HELMET.get());
        warlord(output, ModItems.WARLORD_CHESTPLATE.get(), ModItems.STORMSTEEL_CHESTPLATE.get());
        warlord(output, ModItems.WARLORD_LEGGINGS.get(), ModItems.STORMSTEEL_LEGGINGS.get());
        warlord(output, ModItems.WARLORD_BOOTS.get(), ModItems.STORMSTEEL_BOOTS.get());

        // Voidwalker armour: a diamond piece steeped in the void.
        voidwalker(output, ModItems.VOIDWALKER_HELMET.get(), Items.DIAMOND_HELMET);
        voidwalker(output, ModItems.VOIDWALKER_CHESTPLATE.get(), Items.DIAMOND_CHESTPLATE);
        voidwalker(output, ModItems.VOIDWALKER_LEGGINGS.get(), Items.DIAMOND_LEGGINGS);
        voidwalker(output, ModItems.VOIDWALKER_BOOTS.get(), Items.DIAMOND_BOOTS);
    }

    private static void voidwalker(RecipeOutput output, ItemLike result, ItemLike base) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("SPS").pattern("CAC").pattern("SPS")
                .define('A', base).define('P', Items.ENDER_PEARL).define('S', Items.AMETHYST_SHARD).define('C', Items.CRYING_OBSIDIAN)
                .unlockedBy(getHasName(Items.ENDER_PEARL), has(Items.ENDER_PEARL)).save(output);
    }

    private static void warlord(RecipeOutput output, ItemLike result, ItemLike base) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("BMB").pattern("NAN").pattern("BMB")
                .define('A', base).define('N', Items.NETHERITE_INGOT).define('B', Items.BLAZE_ROD).define('M', Items.MAGMA_CREAM)
                .unlockedBy(getHasName(Items.NETHERITE_INGOT), has(Items.NETHERITE_INGOT)).save(output);
    }

    private static void rune(RecipeOutput output, Rune rune, ItemLike element) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.rune(rune).get())
                .requires(Items.AMETHYST_SHARD, 2).requires(Items.LAPIS_LAZULI).requires(element)
                .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD)).save(output);
    }

    private static void armor(RecipeOutput output, ItemLike result, String... rows) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result);
        for (String row : rows) {
            builder.pattern(row);
        }
        builder.define('M', ModItems.STORMSTEEL_INGOT.get())
                .unlockedBy(getHasName(ModItems.STORMSTEEL_INGOT.get()), has(ModItems.STORMSTEEL_INGOT.get())).save(output);
    }
}

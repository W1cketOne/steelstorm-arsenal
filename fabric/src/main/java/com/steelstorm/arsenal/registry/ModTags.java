package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Items {
        public static final TagKey<Item> MELEE_WEAPONS = TagKey.create(Registries.ITEM, SteelstormArsenal.id("melee_weapons"));
        public static final TagKey<Item> STORMSTEEL_INGOTS = common("ingots/stormsteel");
        public static final TagKey<Item> RAW_STORMSTEEL = common("raw_materials/stormsteel");
        public static final TagKey<Item> STORMSTEEL_ORES = common("ores/stormsteel");

        private static TagKey<Item> common(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        }

        private Items() {
        }
    }

    public static final class Blocks {
        public static final TagKey<Block> STORMSTEEL_ORES = common("ores/stormsteel");
        public static final TagKey<Block> STORMSTEEL_STORAGE_BLOCKS = common("storage_blocks/stormsteel");

        private static TagKey<Block> common(String path) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
        }

        private Blocks() {
        }
    }

    private ModTags() {
    }
}

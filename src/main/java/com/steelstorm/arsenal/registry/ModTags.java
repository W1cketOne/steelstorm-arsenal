package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final class Items {
        public static final TagKey<Item> MELEE_WEAPONS = TagKey.create(Registries.ITEM, SteelstormArsenal.id("melee_weapons"));
        public static final TagKey<Item> STORMSTEEL_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/stormsteel"));

        private Items() {
        }
    }

    private ModTags() {
    }
}

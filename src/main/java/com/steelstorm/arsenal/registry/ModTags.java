package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final class Items {
        /** Every Steelstorm melee weapon (all types and tiers, legendaries included). */
        public static final TagKey<Item> MELEE_WEAPONS = TagKey.create(Registries.ITEM, SteelstormArsenal.id("melee_weapons"));

        private Items() {
        }
    }

    private ModTags() {
    }
}

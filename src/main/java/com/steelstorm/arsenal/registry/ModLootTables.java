package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public final class ModLootTables {
    public static final ResourceKey<LootTable> ABANDONED_ARMORY = chest("abandoned_armory");
    public static final ResourceKey<LootTable> BANDIT_CAMP = chest("bandit_camp");
    public static final ResourceKey<LootTable> RUINED_COLOSSEUM = chest("ruined_colosseum");

    private static ResourceKey<LootTable> chest(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, SteelstormArsenal.id("chests/" + name));
    }

    private ModLootTables() {
    }
}

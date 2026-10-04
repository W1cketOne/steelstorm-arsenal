package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public final class ModLootTables {
    public static final ResourceKey<LootTable> ABANDONED_ARMORY = chest("abandoned_armory");
    public static final ResourceKey<LootTable> BANDIT_CAMP = chest("bandit_camp");
    public static final ResourceKey<LootTable> RUINED_COLOSSEUM = chest("ruined_colosseum");
    public static final ResourceKey<LootTable> CHAMPIONS_COFFER = chest("champions_coffer");
    public static final ResourceKey<LootTable> BANDIT_VAULT = chest("bandit_vault");
    public static final ResourceKey<LootTable> SARCOPHAGUS = chest("sarcophagus");
    public static final ResourceKey<LootTable> KNIGHTS_CRYPT = chest("knights_crypt");
    public static final ResourceKey<LootTable> BLACKSMITH = chest("blacksmith");
    public static final ResourceKey<LootTable> WATCHTOWER = chest("watchtower");
    public static final ResourceKey<LootTable> STORM_SHRINE = chest("storm_shrine");
    public static final ResourceKey<LootTable> STORMSTEEL_MINE = chest("stormsteel_mine");

    private static ResourceKey<LootTable> chest(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, SteelstormArsenal.id("chests/" + name));
    }

    private ModLootTables() {
    }
}

package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.steelstorm.arsenal.entity.TargetDummyItem;
import com.steelstorm.arsenal.weapon.ChakramItem;
import com.steelstorm.arsenal.weapon.ThrowingKnifeItem;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SteelstormArsenal.MODID);

    private static final Map<WeaponType, Map<WeaponTier, DeferredItem<WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);
    private static final List<DeferredItem<WeaponItem>> ALL_TIERED_WEAPONS = new ArrayList<>();

    public static final DeferredItem<Item> STORMSTEEL_INGOT = ITEMS.registerSimpleItem("stormsteel_ingot");

    static {
        // Every weapon type x every tier, registered in one loop.
        for (WeaponType type : WeaponType.values()) {
            Map<WeaponTier, DeferredItem<WeaponItem>> byTier = new EnumMap<>(WeaponTier.class);
            for (WeaponTier tier : WeaponTier.values()) {
                DeferredItem<WeaponItem> item = ITEMS.register(type.itemId(tier), () -> {
                    Item.Properties props = new Item.Properties();
                    if (tier.fireResistant()) {
                        props.fireResistant();
                    }
                    return new WeaponItem(type, tier.tier(), tier, 0.0F, props);
                });
                byTier.put(tier, item);
                ALL_TIERED_WEAPONS.add(item);
            }
            WEAPONS.put(type, byTier);
        }
    }

    public static final DeferredItem<ChakramItem> CHAKRAM = ITEMS.register("chakram",
            () -> new ChakramItem(new Item.Properties().durability(250)));
    public static final DeferredItem<ThrowingKnifeItem> THROWING_KNIFE = ITEMS.register("throwing_knife",
            () -> new ThrowingKnifeItem(new Item.Properties().stacksTo(16)));
    private static final Map<LegendaryWeaponItem.Legendary, DeferredItem<LegendaryWeaponItem>> LEGENDARIES =
            new EnumMap<>(LegendaryWeaponItem.Legendary.class);

    static {
        for (LegendaryWeaponItem.Legendary legendary : LegendaryWeaponItem.Legendary.values()) {
            LEGENDARIES.put(legendary, ITEMS.register(legendary.id(), () -> new LegendaryWeaponItem(legendary, new Item.Properties())));
        }
    }

    public static final DeferredItem<Item> RAW_STORMSTEEL = ITEMS.registerSimpleItem("raw_stormsteel");
    public static final DeferredItem<BlockItem> STORMSTEEL_ORE = ITEMS.registerSimpleBlockItem("stormsteel_ore", ModBlocks.STORMSTEEL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_STORMSTEEL_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_stormsteel_ore", ModBlocks.DEEPSLATE_STORMSTEEL_ORE);
    public static final DeferredItem<BlockItem> STORMSTEEL_BLOCK = ITEMS.registerSimpleBlockItem("stormsteel_block", ModBlocks.STORMSTEEL_BLOCK);
    public static final DeferredItem<BlockItem> RAW_STORMSTEEL_BLOCK =
            ITEMS.registerSimpleBlockItem("raw_stormsteel_block", ModBlocks.RAW_STORMSTEEL_BLOCK);
    public static final DeferredItem<BlockItem> WEAPON_RACK = ITEMS.registerSimpleBlockItem("weapon_rack", ModBlocks.WEAPON_RACK);
    public static final DeferredItem<TargetDummyItem> TARGET_DUMMY = ITEMS.register("target_dummy",
            () -> new TargetDummyItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_DUELIST_SPAWN_EGG = ITEMS.register("bandit_duelist_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BANDIT_DUELIST, 0x5A3A1E, 0xA31F1F, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_ARCHER_SPAWN_EGG = ITEMS.register("bandit_archer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BANDIT_ARCHER, 0x35522A, 0x6B4A2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> IRON_REVENANT_SPAWN_EGG = ITEMS.register("iron_revenant_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.IRON_REVENANT, 0x8A8A8A, 0xFF3A2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FALLEN_WARLORD_SPAWN_EGG = ITEMS.register("fallen_warlord_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FALLEN_WARLORD, 0x2A1416, 0xC9971A, new Item.Properties()));

    public static DeferredItem<LegendaryWeaponItem> legendary(LegendaryWeaponItem.Legendary legendary) {
        return LEGENDARIES.get(legendary);
    }

    public static DeferredItem<WeaponItem> weapon(WeaponType type, WeaponTier tier) {
        return WEAPONS.get(type).get(tier);
    }

    public static List<DeferredItem<WeaponItem>> tieredWeapons() {
        return Collections.unmodifiableList(ALL_TIERED_WEAPONS);
    }

    private ModItems() {
    }
}

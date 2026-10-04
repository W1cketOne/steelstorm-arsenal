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
import com.steelstorm.arsenal.item.HintItem;
import com.steelstorm.arsenal.item.RuneItem;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import com.steelstorm.arsenal.weapon.Rune;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import com.steelstorm.compat.neo.neoforge.common.DeferredSpawnEggItem;
import net.minecraft.world.item.Item;
import com.steelstorm.compat.neo.neoforge.registries.DeferredItem;
import com.steelstorm.compat.neo.neoforge.registries.DeferredRegister;

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

    // Runes, keys and armour.
    private static final Map<Rune, DeferredItem<RuneItem>> RUNES = new EnumMap<>(Rune.class);

    static {
        for (Rune rune : Rune.values()) {
            RUNES.put(rune, ITEMS.register(rune.id() + "_rune",
                    () -> new RuneItem(rune, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))));
        }
    }

    public static final DeferredItem<com.steelstorm.arsenal.item.ExplorersCompassItem> EXPLORERS_COMPASS = ITEMS.register("explorers_compass",
            () -> new com.steelstorm.arsenal.item.ExplorersCompassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HintItem> VAULT_KEY = ITEMS.register("vault_key",
            () -> new HintItem("tooltip.steelstorm.vault_key", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<StormsteelArmorItem> STORMSTEEL_HELMET = armor("stormsteel_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<StormsteelArmorItem> STORMSTEEL_CHESTPLATE = armor("stormsteel_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<StormsteelArmorItem> STORMSTEEL_LEGGINGS = armor("stormsteel_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<StormsteelArmorItem> STORMSTEEL_BOOTS = armor("stormsteel_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<com.steelstorm.arsenal.item.WarlordArmorItem> WARLORD_HELMET = warlord("warlord_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<com.steelstorm.arsenal.item.WarlordArmorItem> WARLORD_CHESTPLATE = warlord("warlord_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<com.steelstorm.arsenal.item.WarlordArmorItem> WARLORD_LEGGINGS = warlord("warlord_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<com.steelstorm.arsenal.item.WarlordArmorItem> WARLORD_BOOTS = warlord("warlord_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<com.steelstorm.arsenal.item.VoidwalkerArmorItem> VOIDWALKER_HELMET = voidwalker("voidwalker_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<com.steelstorm.arsenal.item.VoidwalkerArmorItem> VOIDWALKER_CHESTPLATE = voidwalker("voidwalker_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<com.steelstorm.arsenal.item.VoidwalkerArmorItem> VOIDWALKER_LEGGINGS = voidwalker("voidwalker_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<com.steelstorm.arsenal.item.VoidwalkerArmorItem> VOIDWALKER_BOOTS = voidwalker("voidwalker_boots", ArmorItem.Type.BOOTS);

    // Interactive blocks.
    public static final DeferredItem<BlockItem> WHETSTONE = ITEMS.registerSimpleBlockItem("whetstone", ModBlocks.WHETSTONE);
    public static final DeferredItem<BlockItem> RUNE_FORGE = ITEMS.registerSimpleBlockItem("rune_forge", ModBlocks.RUNE_FORGE,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SIGNAL_BRAZIER = ITEMS.registerSimpleBlockItem("signal_brazier", ModBlocks.SIGNAL_BRAZIER);
    public static final DeferredItem<BlockItem> STORM_ALTAR = ITEMS.registerSimpleBlockItem("storm_altar", ModBlocks.STORM_ALTAR,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> ARENA_GONG = ITEMS.registerSimpleBlockItem("arena_gong", ModBlocks.ARENA_GONG,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CHAMPIONS_COFFER = ITEMS.registerSimpleBlockItem("champions_coffer", ModBlocks.CHAMPIONS_COFFER,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> BANDIT_VAULT = ITEMS.registerSimpleBlockItem("bandit_vault", ModBlocks.BANDIT_VAULT);
    public static final DeferredItem<BlockItem> SARCOPHAGUS = ITEMS.registerSimpleBlockItem("sarcophagus", ModBlocks.SARCOPHAGUS);
    public static final DeferredItem<BlockItem> LEGENDARY_PEDESTAL = ITEMS.registerSimpleBlockItem("legendary_pedestal",
            ModBlocks.LEGENDARY_PEDESTAL, new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_CAPTAIN_SPAWN_EGG = ITEMS.register("bandit_captain_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BANDIT_CAPTAIN, 0x3A2412, 0xC9A227, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CRYPT_KNIGHT_SPAWN_EGG = ITEMS.register("crypt_knight_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CRYPT_KNIGHT, 0x2A2E3A, 0x7FA7FF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> STORM_HERALD_SPAWN_EGG = ITEMS.register("storm_herald_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STORM_HERALD, 0x1B2A4A, 0x7FD8FF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FORGE_COLOSSUS_SPAWN_EGG = ITEMS.register("forge_colossus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FORGE_COLOSSUS, 0x3A3633, 0xFF7A1A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MOONBLADE_REVENANT_SPAWN_EGG = ITEMS.register("moonblade_revenant_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MOONBLADE_REVENANT, 0x1D1A4D, 0x9CC4FF, new Item.Properties()));

    private static DeferredItem<StormsteelArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new StormsteelArmorItem(ModArmorMaterials.STORMSTEEL, type,
                new Item.Properties().durability(type.getDurability(35)).fireResistant()));
    }

    private static DeferredItem<com.steelstorm.arsenal.item.WarlordArmorItem> warlord(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new com.steelstorm.arsenal.item.WarlordArmorItem(ModArmorMaterials.WARLORD, type,
                new Item.Properties().durability(type.getDurability(40)).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
    }

    private static DeferredItem<com.steelstorm.arsenal.item.VoidwalkerArmorItem> voidwalker(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new com.steelstorm.arsenal.item.VoidwalkerArmorItem(ModArmorMaterials.VOIDWALKER, type,
                new Item.Properties().durability(type.getDurability(36)).rarity(net.minecraft.world.item.Rarity.RARE)));
    }

    public static DeferredItem<RuneItem> rune(Rune rune) {
        return RUNES.get(rune);
    }

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

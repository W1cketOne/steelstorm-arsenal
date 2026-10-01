package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SteelstormArsenal.MODID);

    // ---- Materials ----
    public static final DeferredItem<Item> STORMSTEEL_INGOT = ITEMS.registerSimpleItem("stormsteel_ingot",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    // ---- Weapons: every type x tier, registered in one loop ----
    private static final Map<WeaponType, Map<WeaponTier, DeferredItem<WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);

    static {
        for (WeaponType type : WeaponType.values()) {
            Map<WeaponTier, DeferredItem<WeaponItem>> byTier = new EnumMap<>(WeaponTier.class);
            for (WeaponTier tier : WeaponTier.values()) {
                String name = tier.id() + "_" + type.id();
                byTier.put(tier, ITEMS.registerItem(name, props -> new WeaponItem(type, tier, props), weaponProperties(tier)));
            }
            WEAPONS.put(type, Collections.unmodifiableMap(byTier));
        }
    }

    private static Item.Properties weaponProperties(WeaponTier tier) {
        Item.Properties props = new Item.Properties().rarity(tier.rarity());
        if (tier.fireResistant()) {
            props.fireResistant();
        }
        return props;
    }

    public static DeferredItem<WeaponItem> weapon(WeaponType type, WeaponTier tier) {
        return WEAPONS.get(type).get(tier);
    }

    public static Map<WeaponType, Map<WeaponTier, DeferredItem<WeaponItem>>> weapons() {
        return Collections.unmodifiableMap(WEAPONS);
    }

    private ModItems() {
    }
}

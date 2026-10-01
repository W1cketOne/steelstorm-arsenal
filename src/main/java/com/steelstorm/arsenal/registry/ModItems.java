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

    public static DeferredItem<WeaponItem> weapon(WeaponType type, WeaponTier tier) {
        return WEAPONS.get(type).get(tier);
    }

    public static List<DeferredItem<WeaponItem>> tieredWeapons() {
        return Collections.unmodifiableList(ALL_TIERED_WEAPONS);
    }

    private ModItems() {
    }
}

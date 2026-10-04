package com.steelstorm.compat.neo.neoforge.client.extensions.common;

import com.steelstorm.compat.neo.bus.api.Event;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;

public class RegisterClientExtensionsEvent extends Event {
    public static final Map<Item, IClientItemExtensions> ITEMS = new HashMap<>();
    public void registerItem(IClientItemExtensions ext, Item... items) {
        for (Item i : items) { ITEMS.put(i, ext); }
    }
    public static IClientItemExtensions of(Item item) { return ITEMS.get(item); }
}

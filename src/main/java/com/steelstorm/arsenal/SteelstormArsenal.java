package com.steelstorm.arsenal;

import com.mojang.logging.LogUtils;
import com.steelstorm.arsenal.registry.ModCreativeTabs;
import com.steelstorm.arsenal.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SteelstormArsenal.MODID)
public class SteelstormArsenal {
    public static final String MODID = "steelstorm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SteelstormArsenal(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

package com.steelstorm.arsenal;

import com.mojang.logging.LogUtils;
import com.steelstorm.arsenal.registry.ModCreativeTabs;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SteelstormArsenal.MODID)
public class SteelstormArsenal {
    public static final String MODID = "steelstorm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SteelstormArsenal(IEventBus modBus, ModContainer container) {
        ModDataComponents.REGISTER.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, SteelstormConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

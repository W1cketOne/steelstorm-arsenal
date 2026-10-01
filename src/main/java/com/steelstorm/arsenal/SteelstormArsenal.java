package com.steelstorm.arsenal;

import com.mojang.logging.LogUtils;
import com.steelstorm.arsenal.registry.ModAttachments;
import com.steelstorm.arsenal.registry.ModCreativeTabs;
import com.steelstorm.arsenal.registry.ModEffects;
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

    public SteelstormArsenal(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

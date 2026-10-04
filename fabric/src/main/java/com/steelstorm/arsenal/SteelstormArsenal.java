package com.steelstorm.arsenal;

import com.mojang.logging.LogUtils;
import com.steelstorm.arsenal.registry.ModArmorMaterials;
import com.steelstorm.arsenal.registry.ModAttachments;
import com.steelstorm.arsenal.registry.ModBlockEntities;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModCreativeTabs;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.registry.ModStructures;
import net.minecraft.resources.ResourceLocation;
import com.steelstorm.compat.neo.bus.api.IEventBus;
import com.steelstorm.compat.neo.fml.ModContainer;
import com.steelstorm.compat.neo.fml.config.ModConfig;
import org.slf4j.Logger;

public class SteelstormArsenal implements net.fabricmc.api.ModInitializer {
    public static final String MODID = "steelstorm";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        IEventBus modEventBus = null;
        // Fabric registers in call order, so dependencies come first: entities before spawn eggs, items before tabs.
        ModArmorMaterials.MATERIALS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.PIECES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModAttachments.init();

        ModContainer modContainer = new ModContainer();
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        com.steelstorm.compat.FabricHooks.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

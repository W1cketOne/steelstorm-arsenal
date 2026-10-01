package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.client.render.ThrownSpearRenderer;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point. Nothing in this package is loaded on a dedicated server. */
@Mod(value = SteelstormArsenal.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public class SteelstormClient {
    public SteelstormClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_SPEAR.get(), ThrownSpearRenderer::new);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Item[] weapons = ModItems.weapons().values().stream()
                .flatMap(byTier -> byTier.values().stream())
                .map(holder -> (Item) holder.get())
                .toArray(Item[]::new);
        event.registerItem(new WeaponClientExtensions(), weapons);
    }
}

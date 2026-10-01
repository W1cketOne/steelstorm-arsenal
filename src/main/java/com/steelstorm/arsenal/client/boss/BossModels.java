package com.steelstorm.arsenal.client.boss;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Model layer registration for the animated bosses. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class BossModels {
    public static final ModelLayerLocation COLOSSUS = new ModelLayerLocation(SteelstormArsenal.id("forge_colossus"), "main");
    public static final ModelLayerLocation MOONBLADE = new ModelLayerLocation(SteelstormArsenal.id("moonblade_revenant"), "main");

    private BossModels() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(COLOSSUS, BossLayers::forgeColossus);
        event.registerLayerDefinition(MOONBLADE, BossLayers::moonbladeRevenant);
    }
}

package com.steelstorm.arsenal.client.boss;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.client.model.geom.ModelLayerLocation;
import com.steelstorm.compat.neo.neoforge.client.event.EntityRenderersEvent;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;

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

package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModBlockEntities;
import com.steelstorm.arsenal.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class ClientRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_SPEAR.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWING_KNIFE.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAKRAM.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.TARGET_DUMMY.get(),
                ctx -> new SkinnedHumanoidRenderer<>(ctx, SteelstormArsenal.id("textures/entity/target_dummy.png"), 1.0F));
        event.registerBlockEntityRenderer(ModBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
    }

    private ClientRenderers() {
    }
}

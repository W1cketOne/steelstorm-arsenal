package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.client.render.EarthChunkRenderer;
import com.steelstorm.arsenal.client.render.GroundWaveRenderer;
import com.steelstorm.arsenal.client.render.OrbitBladesRenderer;
import com.steelstorm.arsenal.client.render.SlashWaveRenderer;
import com.steelstorm.arsenal.client.render.SpectralWeaponRenderer;
import com.steelstorm.arsenal.client.render.VortexRenderer;
import com.steelstorm.arsenal.registry.ModBlockEntities;
import com.steelstorm.arsenal.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayers;
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
        event.registerEntityRenderer(ModEntities.THROWN_HAMMER.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.GROUND_WAVE.get(), GroundWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_CHUNK.get(), EarthChunkRenderer::new);
        event.registerEntityRenderer(ModEntities.SLASH_WAVE.get(), SlashWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.SPECTRAL_WEAPON.get(), SpectralWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.VORTEX.get(), VortexRenderer::new);
        event.registerEntityRenderer(ModEntities.ORBIT_BLADES.get(), OrbitBladesRenderer::new);
        event.registerEntityRenderer(ModEntities.TARGET_DUMMY.get(),
                ctx -> new SkinnedHumanoidRenderer<>(ctx, SteelstormArsenal.id("textures/entity/target_dummy.png"), 1.0F));
        event.registerEntityRenderer(ModEntities.BANDIT_DUELIST.get(), ctx -> new SkinnedHumanoidRenderer<>(ctx,
                new SteelstormMobModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), SteelstormArsenal.id("textures/entity/bandit_duelist.png"), 1.0F));
        event.registerEntityRenderer(ModEntities.BANDIT_ARCHER.get(), ctx -> new SkinnedHumanoidRenderer<>(ctx,
                new SteelstormMobModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), SteelstormArsenal.id("textures/entity/bandit_archer.png"), 1.0F));
        event.registerEntityRenderer(ModEntities.IRON_REVENANT.get(), ctx -> new SkinnedHumanoidRenderer<>(ctx,
                new SteelstormMobModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), SteelstormArsenal.id("textures/entity/iron_revenant.png"), 1.05F));
        event.registerEntityRenderer(ModEntities.FALLEN_WARLORD.get(), ctx -> new SkinnedHumanoidRenderer<>(ctx,
                new SteelstormMobModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), SteelstormArsenal.id("textures/entity/fallen_warlord.png"), 1.4F));
        event.registerBlockEntityRenderer(ModBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
    }

    private ClientRenderers() {
    }
}

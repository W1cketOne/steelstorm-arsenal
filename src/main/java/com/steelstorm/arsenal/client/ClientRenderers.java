package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.client.render.EarthChunkRenderer;
import com.steelstorm.arsenal.client.render.GroundWaveRenderer;
import com.steelstorm.arsenal.client.render.ItemHolderRenderer;
import com.steelstorm.arsenal.client.render.OrbitBladesRenderer;
import com.steelstorm.arsenal.client.render.SlashWaveRenderer;
import com.steelstorm.arsenal.client.render.SpectralWeaponRenderer;
import com.steelstorm.arsenal.client.render.VortexRenderer;
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
        event.registerEntityRenderer(ModEntities.THROWN_HAMMER.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.GROUND_WAVE.get(), GroundWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_CHUNK.get(), EarthChunkRenderer::new);
        event.registerEntityRenderer(ModEntities.SLASH_WAVE.get(), SlashWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.SPECTRAL_WEAPON.get(), SpectralWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.VORTEX.get(), VortexRenderer::new);
        event.registerEntityRenderer(ModEntities.ORBIT_BLADES.get(), OrbitBladesRenderer::new);
        event.registerEntityRenderer(ModEntities.AURA_FX.get(), com.steelstorm.arsenal.client.render.AuraFxRenderer::new);
        event.registerEntityRenderer(ModEntities.TARGET_DUMMY.get(),
                ctx -> new SkinnedHumanoidRenderer<>(ctx, SteelstormArsenal.id("textures/entity/target_dummy.png"), 1.0F));
        event.registerEntityRenderer(ModEntities.BANDIT_DUELIST.get(), ctx -> new SteelstormMobRenderer<>(ctx, "bandit_duelist", 1.0F, false));
        event.registerEntityRenderer(ModEntities.BANDIT_ARCHER.get(), ctx -> new SteelstormMobRenderer<>(ctx, "bandit_archer", 1.0F, false));
        event.registerEntityRenderer(ModEntities.BANDIT_CAPTAIN.get(), ctx -> new SteelstormMobRenderer<>(ctx, "bandit_captain", 1.05F, false));
        event.registerEntityRenderer(ModEntities.IRON_REVENANT.get(), ctx -> new SteelstormMobRenderer<>(ctx, "iron_revenant", 1.05F, true));
        event.registerEntityRenderer(ModEntities.CRYPT_KNIGHT.get(), ctx -> new SteelstormMobRenderer<>(ctx, "crypt_knight", 1.15F, true));
        event.registerEntityRenderer(ModEntities.FALLEN_WARLORD.get(), ctx -> new SteelstormMobRenderer<>(ctx, "fallen_warlord", 1.4F, true));
        event.registerEntityRenderer(ModEntities.FORGE_COLOSSUS.get(), ctx -> new com.steelstorm.arsenal.client.boss.BossRenderer<>(ctx,
                new com.steelstorm.arsenal.client.boss.ForgeColossusModel(ctx.bakeLayer(com.steelstorm.arsenal.client.boss.BossModels.COLOSSUS)),
                "forge_colossus", 1.6F));
        event.registerEntityRenderer(ModEntities.MOONBLADE_REVENANT.get(), ctx -> new com.steelstorm.arsenal.client.boss.BossRenderer<>(ctx,
                new com.steelstorm.arsenal.client.boss.MoonbladeModel(ctx.bakeLayer(com.steelstorm.arsenal.client.boss.BossModels.MOONBLADE)),
                "moonblade_revenant", 0.6F));
        event.registerEntityRenderer(ModEntities.STORM_HERALD.get(), ctx -> new SteelstormMobRenderer<>(ctx, "storm_herald", 1.2F, true));
        event.registerBlockEntityRenderer(ModBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ITEM_HOLDER.get(), ItemHolderRenderer::new);
    }

    private ClientRenderers() {
    }
}

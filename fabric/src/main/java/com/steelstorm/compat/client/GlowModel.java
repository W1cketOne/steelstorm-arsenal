package com.steelstorm.compat.client;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the faces a weapon model marks with tint index 15 (inlays, glowing gems) at full
 * brightness, so they glow in the dark the way NeoForge's per-element light emission did.
 */
public final class GlowModel extends ForwardingBakedModel {
    public static final int GLOW_TINT = 15;
    private static RenderMaterial emissive;

    public GlowModel(BakedModel model) {
        this.wrapped = model;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> random, RenderContext context) {
        RenderMaterial glow = material();
        context.pushTransform(quad -> {
            if (quad.colorIndex() == GLOW_TINT) {
                quad.colorIndex(-1);
                if (glow != null) {
                    quad.material(glow);
                }
                for (int i = 0; i < 4; i++) {
                    quad.lightmap(i, 0xF000F0);
                }
            }
            return true;
        });
        super.emitItemQuads(stack, random, context);
        context.popTransform();
    }

    private static RenderMaterial material() {
        if (emissive == null) {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            if (renderer != null) {
                emissive = renderer.materialFinder().emissive(true).disableDiffuse(true).find();
            }
        }
        return emissive;
    }
}

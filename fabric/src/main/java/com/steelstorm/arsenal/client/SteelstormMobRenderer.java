package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.SteelstormMonster;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the mod's humanoid enemies with the player model and its skin overlay layer, worn
 * armour, and an optional glow texture (eyes, runes) that stays bright in the dark.
 */
public class SteelstormMobRenderer<T extends SteelstormMonster> extends HumanoidMobRenderer<T, SteelstormMobModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public SteelstormMobRenderer(EntityRendererProvider.Context context, String name, float scale, boolean glows) {
        super(context, new SteelstormMobModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F * scale);
        this.texture = SteelstormArsenal.id("textures/entity/" + name + ".png");
        this.scale = scale;
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        if (glows) {
            addLayer(new GlowLayer<>(this, SteelstormArsenal.id("textures/entity/" + name + "_glow.png")));
        }
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        if (scale != 1.0F) {
            pose.scale(scale, scale, scale);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    /** Draws the glow texture fully lit on top of the skin. */
    static class GlowLayer<T extends SteelstormMonster> extends EyesLayer<T, SteelstormMobModel<T>> {
        private final RenderType type;

        GlowLayer(RenderLayerParent<T, SteelstormMobModel<T>> parent, ResourceLocation texture) {
            super(parent);
            this.type = RenderType.eyes(texture);
        }

        @Override
        public RenderType renderType() {
            return type;
        }
    }
}

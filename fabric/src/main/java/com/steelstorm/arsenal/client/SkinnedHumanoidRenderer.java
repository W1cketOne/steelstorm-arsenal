package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Vanilla humanoid model with one of our own skins, optionally scaled up (for the boss). */
public class SkinnedHumanoidRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public SkinnedHumanoidRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        this(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), texture, scale);
    }

    public SkinnedHumanoidRenderer(EntityRendererProvider.Context context, HumanoidModel<T> model, ResourceLocation texture, float scale) {
        super(context, model, 0.5F * scale);
        this.texture = texture;
        this.scale = scale;
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
}

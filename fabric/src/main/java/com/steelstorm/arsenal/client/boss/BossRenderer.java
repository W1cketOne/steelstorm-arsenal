package com.steelstorm.arsenal.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.boss.AnimatedBoss;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders an animated boss with a glowing layer (eyes, molten cracks, moonlight) that ignores darkness. */
public class BossRenderer<T extends AnimatedBoss, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;

    public BossRenderer(EntityRendererProvider.Context context, M model, String name, float shadow) {
        super(context, model, shadow);
        this.texture = SteelstormArsenal.id("textures/entity/" + name + ".png");
        ResourceLocation glow = SteelstormArsenal.id("textures/entity/" + name + "_glow.png");
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                               float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                float pulse = entity.enraged() ? 0.85F + 0.15F * (float) Math.sin(ageInTicks * 0.4F) : 0.75F;
                int c = (int) (255 * pulse);
                getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(glow)), 0xF000F0, OverlayTexture.NO_OVERLAY,
                        0xFF000000 | (c << 16) | (c << 8) | c);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}

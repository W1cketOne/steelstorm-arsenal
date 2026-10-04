package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.MeteorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** A tumbling chunk of molten rock wrapped in a fiery glow. */
public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(MeteorEntity meteor, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = meteor.tickCount + partialTick;
        float s = meteor.size();
        pose.pushPose();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(time * 9));
        pose.mulPose(Axis.ZP.rotationDegrees(time * 6));
        pose.scale(s, s, s);
        pose.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.MAGMA_BLOCK.defaultBlockState(), pose, buffers, 0xF000F0,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), s * 2.4F, 0xFF7A1A, 1.0F);
        FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), s * 1.3F, 0xFFE0A0, 1.0F);
        pose.popPose();
        super.render(meteor, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return FxRender.GLOW;
    }
}

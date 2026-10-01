package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.EarthChunkEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Draws a popping ground block, or a block shell stretched around a frozen creature. */
public class EarthChunkRenderer extends EntityRenderer<EarthChunkEntity> {
    private final BlockRenderDispatcher blocks;

    public EarthChunkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0;
    }

    @Override
    public void render(EarthChunkEntity chunk, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        BlockState state = chunk.state();
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        pose.pushPose();
        BlockPos lightPos;
        if (chunk.isShell()) {
            float g = chunk.growth(partialTick);
            float w = chunk.shellWidth() * g;
            float h = chunk.shellHeight() * g;
            pose.translate(-w / 2, 0, -w / 2);
            pose.scale(w, h, w);
            lightPos = BlockPos.containing(chunk.getX(), chunk.getY() + 0.5, chunk.getZ());
        } else {
            float offset = chunk.offset(partialTick);
            float k = Mth.clamp(offset * 2, 0, 1);
            pose.translate(-0.5, offset, -0.5);
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(chunk.tiltX() * k));
            pose.mulPose(Axis.ZP.rotationDegrees(chunk.tiltZ() * k));
            pose.translate(-0.5, -0.5, -0.5);
            lightPos = BlockPos.containing(chunk.getX(), chunk.getY() + 1.0, chunk.getZ());
        }
        blocks.renderSingleBlock(state, pose, buffers, LevelRenderer.getLightColor(chunk.level(), lightPos), OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
        pose.popPose();
        super.render(chunk, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EarthChunkEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

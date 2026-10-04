package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.GroundWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the ground blocks a {@link GroundWaveEntity} is lifting, tilted a little so the ripple looks rough. */
public class GroundWaveRenderer extends EntityRenderer<GroundWaveEntity> {
    private final BlockRenderDispatcher blocks;
    private final RandomSource random = RandomSource.create();

    public GroundWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0;
    }

    @Override
    public void render(GroundWaveEntity wave, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Level level = wave.level();
        float time = wave.tickCount + partialTick;
        float maxHeight = Math.max(0.1F, wave.height());
        for (GroundWaveEntity.Column column : wave.columns()) {
            float raise = wave.raise(column.distance(), time);
            if (raise <= 0.02F) {
                continue;
            }
            BlockPos pos = column.pos();
            if (!GroundWaveEntity.isSolid(level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            float k = raise / maxHeight;
            int seed = column.seed();
            float tiltX = (((seed) & 0xFF) / 255.0F - 0.5F) * 32.0F * k;
            float tiltZ = (((seed >> 8) & 0xFF) / 255.0F - 0.5F) * 32.0F * k;
            pose.pushPose();
            pose.translate(pos.getX() - wave.getX(), pos.getY() - wave.getY() + raise, pos.getZ() - wave.getZ());
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(tiltX));
            pose.mulPose(Axis.ZP.rotationDegrees(tiltZ));
            pose.translate(-0.5, -0.5, -0.5);
            BakedModel model = blocks.getBlockModel(state);
            long blockSeed = state.getSeed(pos);
            blocks.getModelRenderer().tesselateBlock(level, model, state, pos.above(), pose,
                    buffers.getBuffer(net.minecraft.client.renderer.ItemBlockRenderTypes.getMovingBlockRenderType(state)), false, random, blockSeed,
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(wave, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GroundWaveEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

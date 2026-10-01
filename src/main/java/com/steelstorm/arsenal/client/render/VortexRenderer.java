package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.VortexEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A starless hole in the world (the End portal shader on a sphere) circled by glowing accretion rings. */
public class VortexRenderer extends EntityRenderer<VortexEntity> {
    public VortexRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(VortexEntity v, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float size = v.size(partialTick);
        if (size <= 0.01F) {
            return;
        }
        pose.pushPose();
        Entity owner = v.owner();
        if (v.follows() && owner != null) {
            // Follow the owner's smooth (interpolated) position instead of last tick's.
            Vec3 target = owner.getPosition(partialTick).add(0, v.lift(), 0);
            Vec3 offset = target.subtract(v.getPosition(partialTick));
            pose.translate(offset.x, offset.y, offset.z);
        }
        float time = v.tickCount + partialTick;
        float core = v.core() * size * (1 + Mth.sin(time * 0.3F) * 0.04F);
        FxRender.sphere(buffers.getBuffer(RenderType.endPortal()), pose.last(), core, 12, 20);

        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), core * 2.6F, v.color(), 0.6F);
        pose.popPose();

        VertexConsumer rings = buffers.getBuffer(RenderType.eyes(FxRender.RING));
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(16 + i * 13 + Mth.sin(time * 0.05F + i) * 6));
            pose.mulPose(Axis.ZP.rotationDegrees(i * 33 - 20));
            pose.mulPose(Axis.YP.rotationDegrees(time * (7 + i * 3) * (i % 2 == 0 ? 1 : -1)));
            float inner = core * (1.12F + i * 0.3F);
            float outer = core * (1.85F + i * 0.6F);
            FxRender.ring(rings, pose.last(), inner, outer, 40, time * 0.015F * (i + 1), i == 0 ? 0xFFFFFF : v.color(), i == 0 ? 0.45F : 0.85F);
            pose.popPose();
        }
        pose.popPose();
        super.render(v, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(VortexEntity entity) {
        return FxRender.RING;
    }
}

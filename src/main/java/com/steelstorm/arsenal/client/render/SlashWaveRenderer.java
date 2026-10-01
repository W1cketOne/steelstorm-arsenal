package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** A glowing crescent facing the way the wave flies. */
public class SlashWaveRenderer extends EntityRenderer<SlashWaveEntity> {
    public SlashWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(SlashWaveEntity wave, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 m = wave.getDeltaMovement();
        Vec3 dir = m.lengthSqr() > 1e-6 ? m : Vec3.directionFromRotation(wave.getXRot(), wave.getYRot());
        float scale = wave.scaleAt(partialTick);
        if (scale > 0.01F) {
            var buffer = buffers.getBuffer(RenderType.eyes(FxRender.SLASH));
            Vec3 back = dir.normalize();
            // The wave itself plus two fading afterimages trailing behind it.
            for (int i = 2; i >= 0; i--) {
                pose.pushPose();
                pose.translate(-back.x * i * 0.7, -back.y * i * 0.7, -back.z * i * 0.7);
                FxRender.crescent(buffer, pose.last(), dir, wave.roll(), wave.halfWidth() * scale * (1 - i * 0.12F), wave.color(),
                        i == 0 ? 1.0F : 0.45F / i);
                pose.popPose();
            }
        }
        super.render(wave, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SlashWaveEntity entity) {
        return FxRender.SLASH;
    }
}

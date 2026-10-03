package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.AuraFxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Draws {@link AuraFxEntity} effects with additive glow. */
public class AuraFxRenderer extends EntityRenderer<AuraFxEntity> {
    private static final ResourceLocation BAND = SteelstormArsenal.id("textures/entity/halo_band.png");
    private static final ResourceLocation ORB = SteelstormArsenal.id("textures/entity/orb.png");
    private static final ResourceLocation SPARKLE = SteelstormArsenal.id("textures/entity/sparkle.png");
    private static final ResourceLocation RAY = SteelstormArsenal.id("textures/entity/ray.png");
    private static final ResourceLocation CRACKS = SteelstormArsenal.id("textures/entity/cracks.png");
    private static final ResourceLocation CRACKS_GLOW = SteelstormArsenal.id("textures/entity/cracks_glow.png");

    public AuraFxRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public boolean shouldRender(AuraFxEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(AuraFxEntity fx, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float p = fx.progress(partialTick);
        if (p >= 1) {
            return;
        }
        float time = fx.tickCount + partialTick;
        pose.pushPose();
        Vec3 offset = fx.center(partialTick).subtract(fx.getPosition(partialTick));
        pose.translate(offset.x, offset.y, offset.z);
        switch (fx.style()) {
            case HALO -> halo(fx, p, pose, buffers);
            case GYRO -> gyro(fx, p, time, pose, buffers);
            case ORBIT -> orbit(fx, p, time, pose, buffers);
            case PILLAR -> pillar(fx, p, time, pose, buffers);
            case SUNBURST -> sunburst(fx, p, time, pose, buffers);
            case CIRCLE -> circle(fx, p, time, pose, buffers);
            case CRACKS -> cracks(fx, p, pose, buffers, light);
        }
        pose.popPose();
    }

    /** An expanding ground shockwave: a hot band with a soft wake and a second ring trailing it. */
    private void halo(AuraFxEntity fx, float p, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(BAND));
        float r = fx.radius() * AuraFxEntity.easeOut(p);
        float fade = (1 - p) * (1 - p);
        float width = 0.6F + fx.radius() * 0.12F;
        pose.translate(0, 0.12, 0);
        FxRender.ring(vc, pose.last(), Math.max(0, r - width), r + width * 0.35F, 64, 0, fx.color(), fade * 1.2F);
        float r2 = fx.radius() * AuraFxEntity.easeOut(Math.max(0, p - 0.12F));
        FxRender.ring(vc, pose.last(), Math.max(0, r2 - width * 0.6F), r2 + width * 0.2F, 64, 0.3F, fx.color2(), fade * 0.8F);
        // A low, faint dome so it reads from the side too.
        pose.pushPose();
        pose.scale(1, 0.35F, 1);
        for (int i = 1; i <= 3; i++) {
            pose.pushPose();
            pose.translate(0, i * width * 0.9F, 0);
            float shrink = 1 - i * 0.12F;
            FxRender.ring(vc, pose.last(), Math.max(0, r * shrink - width * 0.5F), r * shrink + width * 0.1F, 64, i * 0.2F, fx.color(),
                    fade * 0.35F / i);
            pose.popPose();
        }
        pose.popPose();
    }

    /** Three tilted rings spinning around each other as they grow, like a gyroscope. */
    private void gyro(AuraFxEntity fx, float p, float time, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(BAND));
        float r = fx.radius() * AuraFxEntity.easeOut(Math.min(1, p * 1.6F));
        float fade = p < 0.7F ? 1 : (1 - p) / 0.3F;
        pose.translate(0, fx.height(), 0);
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * (9 + i * 4) + i * 60));
            pose.mulPose(Axis.XP.rotationDegrees(60 + i * 35));
            FxRender.ring(vc, pose.last(), r * 0.86F, r, 48, time * 0.02F, i == 1 ? fx.color2() : fx.color(), fade);
            pose.popPose();
        }
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), r * 0.7F, fx.color2(), fade * 0.5F);
    }

    /** Glowing orbs circling the centre, bobbing as they go, each with a sparkle core. */
    private void orbit(AuraFxEntity fx, float p, float time, PoseStack pose, MultiBufferSource buffers) {
        float grow = Mth.clamp(p * 6, 0, 1);
        float fade = p < 0.85F ? 1 : (1 - p) / 0.15F;
        float r = fx.radius() * (0.4F + 0.6F * grow);
        // One buffer at a time: asking for another finishes the first.
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(pass == 0 ? ORB : SPARKLE));
            for (int i = 0; i < fx.count(); i++) {
                float a = time * 0.35F + Mth.TWO_PI * i / fx.count();
                float y = fx.height() + Mth.sin(a * 2 + time * 0.1F) * 0.25F;
                pose.pushPose();
                pose.translate(Mth.cos(a) * r, y, Mth.sin(a) * r);
                pose.mulPose(entityRenderDispatcher.cameraOrientation());
                float size = 0.22F * fade * (1 + 0.15F * Mth.sin(time * 0.6F + i));
                if (pass == 0) {
                    FxRender.halo(vc, pose.last(), size, fx.color(), 1.0F);
                } else {
                    FxRender.halo(vc, pose.last(), size * 0.8F, fx.color2(), 0.9F);
                }
                pose.popPose();
            }
        }
    }

    /** A column of light with rays sweeping round its base. */
    private void pillar(AuraFxEntity fx, float p, float time, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(FxRender.GLOW));
        float grow = AuraFxEntity.easeOut(Math.min(1, p * 4));
        float fade = p < 0.6F ? 1 : (1 - p) / 0.4F;
        float w = fx.radius() * (0.6F + 0.4F * grow) * (1 + 0.08F * Mth.sin(time * 0.9F));
        float h = fx.height() * grow;
        float camYaw = -entityRenderDispatcher.camera.getYRot();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(camYaw));
        FxRender.beam(glow, pose.last(), 0, h, w, fx.color(), fade);
        FxRender.beam(glow, pose.last(), 0, h, w * 0.35F, fx.color2(), fade);
        pose.popPose();
        VertexConsumer rays = buffers.getBuffer(RenderType.eyes(RAY));
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 6 + i * 60));
            FxRender.quad(rays, pose.last(), new Vector3f(-0.25F, 0, 0), new Vector3f(0.25F, 0, 0),
                    new Vector3f(0.25F, 0, w * 5), new Vector3f(-0.25F, 0, w * 5), 0, 1, 1, 0, fx.color(), fade * 0.7F);
            pose.popPose();
        }
    }

    /** Spinning rays bursting out from the centre. */
    private void sunburst(AuraFxEntity fx, float p, float time, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer rays = buffers.getBuffer(RenderType.eyes(RAY));
        float len = fx.radius() * AuraFxEntity.easeOut(p);
        float fade = (1 - p);
        pose.translate(0, fx.height(), 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees(time * 4));
        for (int i = 0; i < fx.count(); i++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(360F * i / fx.count()));
            float w = (i % 2 == 0 ? 0.35F : 0.2F) * (0.5F + fx.radius() * 0.08F);
            float l = len * (i % 2 == 0 ? 1 : 0.65F);
            FxRender.quad(rays, pose.last(), new Vector3f(-w, 0, 0), new Vector3f(w, 0, 0), new Vector3f(w, l, 0), new Vector3f(-w, l, 0),
                    0, 1, 1, 0, i % 2 == 0 ? fx.color() : fx.color2(), fade);
            pose.popPose();
        }
        FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), len * 0.35F, fx.color2(), fade);
    }

    /** A magic circle on the ground: two counter-rotating patterned rings and a star in the middle. */
    private void circle(AuraFxEntity fx, float p, float time, PoseStack pose, MultiBufferSource buffers) {
        float grow = AuraFxEntity.easeOut(Math.min(1, p * 5));
        float fade = p < 0.8F ? 1 : (1 - p) / 0.2F;
        float r = fx.radius() * grow;
        pose.translate(0, 0.08, 0);
        VertexConsumer rings = buffers.getBuffer(RenderType.eyes(FxRender.RING));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(time * 3));
        FxRender.ring(rings, pose.last(), r * 0.82F, r, 64, 0, fx.color(), fade);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-time * 5));
        FxRender.ring(rings, pose.last(), r * 0.48F, r * 0.62F, 48, 0.5F, fx.color2(), fade);
        pose.popPose();
        VertexConsumer band = buffers.getBuffer(RenderType.eyes(BAND));
        FxRender.ring(band, pose.last(), r * 0.95F, r * 1.08F, 64, 0, fx.color(), fade * 0.6F);
        VertexConsumer star = buffers.getBuffer(RenderType.eyes(SPARKLE));
        pose.mulPose(Axis.YP.rotationDegrees(time * 2));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        FxRender.halo(star, pose.last(), r * 0.45F, fx.color2(), fade);
    }

    /** Scorched cracks split the ground, glowing hot at first, then cool and fade away. */
    private void cracks(AuraFxEntity fx, float p, PoseStack pose, MultiBufferSource buffers, int light) {
        float open = AuraFxEntity.easeOut(Math.min(1, p * 8));
        float fade = p < 0.7F ? 1 : (1 - p) / 0.3F;
        float r = fx.radius() * (0.6F + 0.4F * open);
        pose.translate(0, 0.03, 0);
        pose.mulPose(Axis.YP.rotationDegrees(fx.getId() * 47 % 360));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        FxRender.halo(buffers.getBuffer(RenderType.entityTranslucent(CRACKS)), pose.last(), r, 0xFFFFFF, fade);
        float hot = Math.max(0, 1 - p * 2.2F);
        if (hot > 0) {
            pose.translate(0, 0, -0.01);
            FxRender.halo(buffers.getBuffer(RenderType.eyes(CRACKS_GLOW)), pose.last(), r, fx.color(), hot);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AuraFxEntity entity) {
        return BAND;
    }
}

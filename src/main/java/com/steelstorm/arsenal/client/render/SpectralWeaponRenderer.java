package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.SpectralWeaponEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** A giant weapon standing on its tip, fully lit, wrapped in a column of glow. */
public class SpectralWeaponRenderer extends EntityRenderer<SpectralWeaponEntity> {
    private final ItemRenderer items;

    public SpectralWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0;
    }

    @Override
    public void render(SpectralWeaponEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = e.stack();
        if (stack.isEmpty()) {
            return;
        }
        float scale = e.scale() * e.appear(partialTick);
        float tip = WeaponModels.tip(stack);
        float length = (tip + WeaponModels.tail(stack)) * scale;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.scale(scale, scale, scale);
        pose.translate(0, tip, 0);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        items.renderStatic(stack, ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
        pose.popPose();

        Vec3 cam = entityRenderDispatcher.camera.getPosition();
        Vec3 at = e.getPosition(partialTick);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(cam.x - at.x, cam.z - at.z)));
        float glow = e.hasLanded() ? 0.45F : 0.8F;
        FxRender.beam(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), -0.3F * scale, length + 0.3F * scale, 0.55F * scale,
                e.color(), glow);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SpectralWeaponEntity entity) {
        return FxRender.GLOW;
    }
}

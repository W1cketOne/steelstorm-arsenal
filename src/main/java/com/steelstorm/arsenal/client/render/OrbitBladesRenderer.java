package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.OrbitBladesEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Chakrams spinning flat around their owner, each with a soft glow. */
public class OrbitBladesRenderer extends EntityRenderer<OrbitBladesEntity> {
    private final ItemRenderer items;

    public OrbitBladesRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0;
    }

    @Override
    public void render(OrbitBladesEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity owner = e.owner();
        Vec3 base = owner != null ? owner.getPosition(partialTick).add(0, 1.0, 0) : e.getPosition(partialTick);
        Vec3 offset = base.subtract(e.getPosition(partialTick));
        float age = e.tickCount + partialTick;
        ItemStack stack = e.stack();
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        for (int i = 0; i < e.count(); i++) {
            Vec3 p = e.bladeOffset(i, age);
            pose.pushPose();
            pose.translate(p.x, p.y, p.z);
            pose.pushPose();
            // Face along the orbit, tilt so the blade shows its face, then spin it like a saw.
            pose.mulPose(Axis.YP.rotation((float) Math.atan2(p.x, p.z)));
            pose.mulPose(Axis.XP.rotationDegrees(55));
            pose.mulPose(Axis.ZP.rotationDegrees(age * 47 + i * 30));
            pose.scale(0.75F, 0.75F, 0.75F);
            items.renderStatic(stack, ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId() + i);
            pose.popPose();
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), 0.6F, e.color(), 0.5F);
            pose.popPose();
        }
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(OrbitBladesEntity entity) {
        return FxRender.GLOW;
    }
}

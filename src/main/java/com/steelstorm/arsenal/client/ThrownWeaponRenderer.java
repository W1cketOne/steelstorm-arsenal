package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.ThrownHammerEntity;
import com.steelstorm.arsenal.entity.ThrownWeaponEntity;
import com.steelstorm.arsenal.weapon.ThrowingKnifeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a thrown weapon with its 3D model: spears and knives fly point first, the chakram spins
 * flat and the warhammer tumbles end over end.
 */
public class ThrownWeaponRenderer<T extends Entity & ThrownWeaponEntity> extends EntityRenderer<T> {
    private final ItemRenderer itemRenderer;

    public ThrownWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = entity.getRenderStack();
        float age = entity.tickCount + partialTick;
        pose.pushPose();
        if (entity.spins()) {
            pose.mulPose(Axis.YP.rotationDegrees(age * 40.0F));
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.75F, 0.75F, 0.75F);
        } else if (entity instanceof ThrownHammerEntity) {
            Vec3 m = entity.getDeltaMovement();
            float heading = m.horizontalDistanceSqr() > 1e-6 ? (float) Math.toDegrees(Math.atan2(m.x, m.z)) : -entity.getYRot();
            pose.mulPose(Axis.YP.rotationDegrees(heading));
            pose.mulPose(Axis.XP.rotationDegrees(age * 48.0F));
            pose.scale(0.7F, 0.7F, 0.7F);
            // Spin around the hammer's balance point, near the head.
            pose.translate(0, -0.4, 0);
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 90.0F));
            float scale = stack.getItem() instanceof ThrowingKnifeItem ? 0.85F : 0.62F;
            pose.scale(scale, scale, scale);
        }
        itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, entity.glows() ? 0xF000F0 : light, OverlayTexture.NO_OVERLAY, pose, buffers,
                entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.entity.ThrownSpear;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the thrown spear's actual item model, pointed along its flight path with a little quiver on impact. */
public class ThrownSpearRenderer extends EntityRenderer<ThrownSpear> {
    private final ItemRenderer itemRenderer;

    public ThrownSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSpear entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        float shake = entity.shakeTime - partialTicks;
        if (shake > 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0f) * shake));
        }
        // The sprite is drawn diagonally (tip top-right); turn it so the tip leads (+X), then push it
        // back so the point sits at the entity position like a real spear stuck in a block.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));
        poseStack.translate(-0.8f, -0.8f, 0.0f);
        poseStack.scale(2.0f, 2.0f, 2.0f);
        itemRenderer.renderStatic(entity.getDisplayStack(), ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownSpear entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

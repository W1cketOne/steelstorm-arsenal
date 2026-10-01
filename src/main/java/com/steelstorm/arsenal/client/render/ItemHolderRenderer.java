package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.block.ItemHolderBlockEntity;
import com.steelstorm.arsenal.block.LegendaryPedestalBlock;
import com.steelstorm.arsenal.item.RuneItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The item floating over a Rune Forge (a slowly turning rune) or a Legendary Pedestal (the
 * weapon standing point-down in a column of light).
 */
public class ItemHolderRenderer implements BlockEntityRenderer<ItemHolderBlockEntity> {
    private final ItemRenderer items;

    public ItemHolderRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(ItemHolderBlockEntity holder, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = holder.getItem();
        if (stack.isEmpty() || holder.getLevel() == null) {
            return;
        }
        float time = holder.getLevel().getGameTime() + partialTick;
        boolean pedestal = holder.getBlockState().getBlock() instanceof LegendaryPedestalBlock;
        pose.pushPose();
        if (pedestal) {
            float tip = WeaponModels.tip(stack);
            float scale = 0.75F;
            pose.translate(0.5, 1.05 + Math.sin(time * 0.06) * 0.05, 0.5);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 1.2F));
            pose.scale(scale, scale, scale);
            pose.translate(0, tip, 0);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            items.renderStatic(stack, ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, holder.getLevel(), 0);
            pose.popPose();
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 at = Vec3.atCenterOf(holder.getBlockPos());
            pose.mulPose(Axis.YP.rotation((float) Math.atan2(cam.x - at.x, cam.z - at.z)));
            float length = (tip + WeaponModels.tail(stack)) * scale;
            FxRender.beam(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), -0.4F, length + 0.6F, 0.5F,
                    WeaponLooks.trailColor(stack), 0.35F + 0.1F * (float) Math.sin(time * 0.1));
        } else {
            pose.translate(0.5, 1.15 + Math.sin(time * 0.08) * 0.06, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
            pose.scale(0.55F, 0.55F, 0.55F);
            items.renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, holder.getLevel(), 0);
            if (stack.getItem() instanceof RuneItem rune) {
                pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                FxRender.halo(buffers.getBuffer(RenderType.eyes(FxRender.GLOW)), pose.last(), 0.9F, rune.rune().color(), 0.6F);
            }
        }
        pose.popPose();
    }
}

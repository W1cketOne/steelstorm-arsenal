package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.block.WeaponRackBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Stands up to three weapons upright on the rack, blades pointing up. */
public class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity> {
    private final ItemRenderer itemRenderer;

    public WeaponRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(WeaponRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = rack.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        for (int slot = 0; slot < WeaponRackBlockEntity.SLOTS; slot++) {
            ItemStack stack = rack.getWeapon(slot);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // Slot 0 is on the left when looking at the front of the rack.
            pose.translate((slot - 1) * 0.3125, 0.55, 0.0);
            // Item sprites run diagonally; rotate so the blade points straight up.
            pose.mulPose(Axis.ZP.rotationDegrees(-45));
            pose.scale(0.62F, 0.62F, 0.62F);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    rack.getLevel(), (int) rack.getBlockPos().asLong() + slot);
            pose.popPose();
        }
    }
}

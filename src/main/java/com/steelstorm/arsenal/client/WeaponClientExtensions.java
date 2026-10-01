package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Custom first- and third-person poses for weapons (spear wind-up). */
public class WeaponClientExtensions implements IClientItemExtensions {

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (entity.isUsingItem() && entity.getUsedItemHand() == hand && WeaponItem.isThrowReady(stack)) {
            return HumanoidModel.ArmPose.THROW_SPEAR;
        }
        return null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (!(player.isUsingItem() && player.getUseItem() == stack && WeaponItem.isThrowReady(stack))) {
            return false;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float used = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0f);
        float charge = Mth.clamp(used / WeaponItem.SPEAR_CHARGE_TICKS, 0.0f, 1.0f);
        // Base arm placement (same as vanilla's applyItemArmTransform).
        poseStack.translate(side * 0.56f, -0.52f + equipProcess * -0.6f, -0.72f);
        // Draw the spear back over the shoulder, tip forward, trembling once fully charged.
        float tremble = charge >= 1.0f ? Mth.sin(used * 1.3f) * 0.006f : 0.0f;
        poseStack.translate(side * -0.1f * charge, 0.18f * charge + tremble, 0.35f * charge);
        poseStack.mulPose(Axis.XP.rotationDegrees(-35.0f * charge));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 10.0f * charge));
        return true;
    }
}

package com.steelstorm.compat.mixin.client;

import com.steelstorm.arsenal.client.anim.WeaponPoses;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Steelstorm weapons pose the arms themselves, in place of vanilla's ITEM pose. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow
    public HumanoidModel.ArmPose rightArmPose;
    @Shadow
    public HumanoidModel.ArmPose leftArmPose;

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true)
    private void steelstorm$poseRight(LivingEntity entity, CallbackInfo ci) {
        if (steelstorm$pose(entity, HumanoidArm.RIGHT, rightArmPose)) {
            ci.cancel();
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true)
    private void steelstorm$poseLeft(LivingEntity entity, CallbackInfo ci) {
        if (steelstorm$pose(entity, HumanoidArm.LEFT, leftArmPose)) {
            ci.cancel();
        }
    }

    private boolean steelstorm$pose(LivingEntity entity, HumanoidArm arm, HumanoidModel.ArmPose pose) {
        if (pose != HumanoidModel.ArmPose.ITEM || arm != entity.getMainArm() || !(entity.getMainHandItem().getItem() instanceof WeaponItem)) {
            return false;
        }
        WeaponPoses.transform((HumanoidModel<?>) (Object) this, entity, arm);
        return true;
    }
}

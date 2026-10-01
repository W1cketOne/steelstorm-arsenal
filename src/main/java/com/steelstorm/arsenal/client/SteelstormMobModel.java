package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.entity.SteelstormMonster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;

/**
 * Vanilla humanoid model with two extra poses: a drawn bow while an archer is aggressive, and both
 * arms raised overhead while any Steelstorm enemy winds up a telegraphed attack.
 */
public class SteelstormMobModel<T extends SteelstormMonster> extends HumanoidModel<T> {
    public SteelstormMobModel(ModelPart root) {
        super(root);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        rightArmPose = ArmPose.EMPTY;
        leftArmPose = ArmPose.EMPTY;
        if (entity.getMainHandItem().getItem() instanceof BowItem && entity.isAggressive()) {
            rightArmPose = ArmPose.BOW_AND_ARROW;
        } else if (!entity.getMainHandItem().isEmpty()) {
            rightArmPose = ArmPose.ITEM;
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.isCharging()) {
            // Telegraph: weapon raised high overhead, trembling slightly.
            float shake = Mth.sin(ageInTicks * 1.7F) * 0.06F;
            rightArm.xRot = -Mth.PI * 0.95F + shake;
            leftArm.xRot = -Mth.PI * 0.85F - shake;
            rightArm.yRot = -0.2F;
            leftArm.yRot = 0.2F;
            rightArm.zRot = 0;
            leftArm.zRot = 0;
        }
    }
}

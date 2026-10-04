package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.entity.SteelstormMonster;
import com.steelstorm.arsenal.entity.StormHerald;
import com.steelstorm.arsenal.client.anim.WeaponAnimator;
import com.steelstorm.arsenal.client.anim.WeaponPoses;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;

/**
 * The player model (with its second skin layer for hoods, cloaks and armour plates) plus extra
 * poses: a drawn bow while an archer is aggressive, both arms raised overhead while any Steelstorm
 * enemy winds up a telegraphed attack, and a floating caster's pose for the Storm Herald.
 */
public class SteelstormMobModel<T extends SteelstormMonster> extends PlayerModel<T> {
    public SteelstormMobModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        rightArmPose = ArmPose.EMPTY;
        leftArmPose = ArmPose.EMPTY;
        if (entity.getMainHandItem().getItem() instanceof BowItem && entity.isAggressive()) {
            rightArmPose = ArmPose.BOW_AND_ARROW;
        } else if (entity.getMainHandItem().getItem() instanceof WeaponItem weapon) {
            // Mod weapons swing and pose exactly like they do for players.
            rightArmPose = ArmPose.ITEM;
        } else if (!entity.getMainHandItem().isEmpty()) {
            rightArmPose = ArmPose.ITEM;
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof StormHerald) {
            // Floating: legs hang loose and sway, the free arm reaches out.
            rightLeg.xRot = 0.15F + Mth.sin(ageInTicks * 0.08F) * 0.08F;
            leftLeg.xRot = 0.25F + Mth.sin(ageInTicks * 0.08F + 1.0F) * 0.08F;
            leftArm.xRot = -0.4F + Mth.sin(ageInTicks * 0.1F) * 0.1F;
            leftArm.zRot = -0.5F;
        }
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
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }
}

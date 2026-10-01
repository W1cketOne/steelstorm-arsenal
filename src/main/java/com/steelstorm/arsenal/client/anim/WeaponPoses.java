package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * The arm poses Steelstorm weapons use in third person, added to vanilla's ArmPose enum through
 * META-INF/enumextensions.json. They hand over to {@link WeaponAnimator}.
 */
public final class WeaponPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> ONE_HANDED = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) WeaponPoses::transform);
    public static final EnumProxy<HumanoidModel.ArmPose> TWO_HANDED = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) WeaponPoses::transform);

    private WeaponPoses() {
    }

    private static void transform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (arm != entity.getMainArm() || !(entity.getMainHandItem().getItem() instanceof WeaponItem weapon)) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        WeaponAnimator.poseThirdPerson(model, entity, weapon.type(), partialTick);
    }
}

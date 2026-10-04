package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Third-person arm posing for Steelstorm weapons, called from the HumanoidModel.setupAnim mixin.
 * Hands over to {@link WeaponAnimator}.
 */
public final class WeaponPoses {

    private WeaponPoses() {
    }

    public static void transform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (arm != entity.getMainArm() || !(entity.getMainHandItem().getItem() instanceof WeaponItem weapon)) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        WeaponAnimator.poseThirdPerson(model, entity, weapon.type(), partialTick);
    }
}

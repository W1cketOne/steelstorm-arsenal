package com.steelstorm.arsenal.client.boss;

import com.steelstorm.arsenal.entity.boss.ForgeColossus;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The Forge Colossus: every attack has its own keyframed animation. */
public class ForgeColossusModel extends HierarchicalModel<ForgeColossus> {
    static final AnimationDefinition IDLE = Anim.of(3.0F).loop()
            .rot("torso", 0, 0, 0, 0, 1.5F, -3, 0, 0, 3.0F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 1.5F, 4, 0, 0, 3.0F, 0, 0, 0)
            .rot("right_arm", 0, 0, 0, 4, 1.5F, -4, 0, 6, 3.0F, 0, 0, 4)
            .rot("left_arm", 0, 0, 0, -4, 1.5F, -4, 0, -6, 3.0F, 0, 0, -4)
            .build();
    static final AnimationDefinition POUND = Anim.of(1.8F)
            .rot("right_arm", 0, 0, 0, 0, 0.6F, -165, 0, 20, 0.85F, -170, 0, 15, 1.0F, -30, 0, 10, 1.4F, -30, 0, 10, 1.8F, 0, 0, 0)
            .rot("left_arm", 0, 0, 0, 0, 0.6F, -165, 0, -20, 0.85F, -170, 0, -15, 1.0F, -30, 0, -10, 1.4F, -30, 0, -10, 1.8F, 0, 0, 0)
            .rot("right_forearm", 0, 0, 0, 0, 0.6F, -30, 0, 0, 1.0F, 0, 0, 0, 1.8F, 0, 0, 0)
            .rot("left_forearm", 0, 0, 0, 0, 0.6F, -30, 0, 0, 1.0F, 0, 0, 0, 1.8F, 0, 0, 0)
            .rot("torso", 0, 0, 0, 0, 0.6F, -14, 0, 0, 0.85F, -16, 0, 0, 1.0F, 30, 0, 0, 1.4F, 28, 0, 0, 1.8F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.6F, -20, 0, 0, 1.0F, 10, 0, 0, 1.8F, 0, 0, 0)
            .pos("hips", 0, 0, 0, 0, 0.6F, 0, -1, 0, 1.0F, 0, 4, 0, 1.4F, 0, 4, 0, 1.8F, 0, 0, 0)
            .rot("right_leg", 0, 0, 0, 0, 1.0F, -25, 0, 8, 1.4F, -25, 0, 8, 1.8F, 0, 0, 0)
            .rot("left_leg", 0, 0, 0, 0, 1.0F, 20, 0, -8, 1.4F, 20, 0, -8, 1.8F, 0, 0, 0)
            .build();
    static final AnimationDefinition SWEEP = Anim.of(1.7F)
            .rot("torso", 0, 0, 0, 0, 0.5F, 0, 70, 0, 0.75F, 0, -160, 0, 1.05F, 0, -150, 0, 1.25F, 0, 120, 0, 1.7F, 0, 0, 0)
            .rot("right_arm", 0, 0, 0, 0, 0.5F, -40, 0, 70, 0.75F, -80, 0, 85, 1.25F, -60, 0, 40, 1.7F, 0, 0, 0)
            .rot("left_arm", 0, 0, 0, 0, 0.5F, -20, 0, -30, 1.05F, -60, 0, -50, 1.25F, -85, 0, -85, 1.7F, 0, 0, 0)
            .pos("hips", 0, 0, 0, 0, 0.6F, 0, 2, 0, 1.3F, 0, 2, 0, 1.7F, 0, 0, 0)
            .build();
    static final AnimationDefinition BARRAGE = Anim.of(2.5F)
            .rot("torso", 0, 0, 0, 0, 0.4F, -22, 0, 0, 2.0F, -22, 0, 0, 2.5F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.4F, -25, 0, 0, 2.0F, -25, 0, 0, 2.5F, 0, 0, 0)
            .rot("right_arm", 0, 0, 0, 0, 0.4F, -20, 0, 50, 0.8F, -40, 0, 60, 1.2F, -20, 0, 50, 1.6F, -40, 0, 60, 2.0F, -20, 0, 50, 2.5F, 0, 0, 0)
            .rot("left_arm", 0, 0, 0, 0, 0.4F, -20, 0, -50, 0.8F, -40, 0, -60, 1.2F, -20, 0, -50, 1.6F, -40, 0, -60, 2.0F, -20, 0, -50, 2.5F, 0, 0, 0)
            .scale("torso", 0, 1, 1, 1, 0.6F, 1.04F, 1.04F, 1.04F, 0.9F, 1, 1, 1, 1.2F, 1.04F, 1.04F, 1.04F, 1.5F, 1, 1, 1, 2.5F, 1, 1, 1)
            .build();
    static final AnimationDefinition CHARGE = Anim.of(1.7F)
            .rot("torso", 0, 0, 0, 0, 0.45F, 30, 0, 0, 1.2F, 30, 0, 0, 1.45F, -5, 0, 0, 1.7F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.45F, 20, 0, 0, 1.2F, 20, 0, 0, 1.7F, 0, 0, 0)
            .rot("right_arm", 0, 0, 0, 0, 0.45F, 40, 0, 15, 0.6F, -30, 0, 15, 0.75F, 40, 0, 15, 0.9F, -30, 0, 15, 1.05F, 40, 0, 15, 1.2F, -30, 0, 15, 1.7F, 0, 0, 0)
            .rot("left_arm", 0, 0, 0, 0, 0.45F, -30, 0, -15, 0.6F, 40, 0, -15, 0.75F, -30, 0, -15, 0.9F, 40, 0, -15, 1.05F, -30, 0, -15, 1.2F, 40, 0, -15, 1.7F, 0, 0, 0)
            .rot("right_leg", 0, 0, 0, 0, 0.5F, -35, 0, 0, 0.65F, 35, 0, 0, 0.8F, -35, 0, 0, 0.95F, 35, 0, 0, 1.1F, -35, 0, 0, 1.25F, 0, 0, 0)
            .rot("left_leg", 0, 0, 0, 0, 0.5F, 35, 0, 0, 0.65F, -35, 0, 0, 0.8F, 35, 0, 0, 0.95F, -35, 0, 0, 1.1F, 35, 0, 0, 1.25F, 0, 0, 0)
            .build();
    static final AnimationDefinition ERUPTION = Anim.of(2.8F)
            .rot("right_arm", 0, 0, 0, 0, 0.6F, -170, 0, 10, 0.8F, -60, 0, 5, 2.2F, -60, 0, 5, 2.8F, 0, 0, 0)
            .rot("left_arm", 0, 0, 0, 0, 0.6F, -170, 0, -10, 0.8F, -60, 0, -5, 2.2F, -60, 0, -5, 2.8F, 0, 0, 0)
            .rot("torso", 0, 0, 0, 0, 0.6F, -18, 0, 0, 0.8F, 40, 0, 0, 2.2F, 40, 0, 0, 2.8F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.8F, -30, 0, 0, 2.2F, -30, 0, 0, 2.8F, 0, 0, 0)
            .pos("hips", 0, 0, 0, 0, 0.8F, 0, 5, 0, 2.2F, 0, 5, 0, 2.8F, 0, 0, 0)
            .rot("right_leg", 0, 0, 0, 0, 0.8F, -40, 0, 10, 2.2F, -40, 0, 10, 2.8F, 0, 0, 0)
            .rot("left_leg", 0, 0, 0, 0, 0.8F, -40, 0, -10, 2.2F, -40, 0, -10, 2.8F, 0, 0, 0)
            .build();
    static final AnimationDefinition OVERHEAT = Anim.of(2.0F)
            .rot("torso", 0, 0, 0, 0, 0.4F, -28, 0, 0, 0.5F, -24, 3, 0, 0.6F, -28, -3, 0, 0.7F, -24, 3, 0, 0.8F, -28, -3, 0, 0.9F, -24, 3, 0,
                    1.0F, -28, 0, 0, 1.5F, -20, 0, 0, 2.0F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.4F, -45, 0, 0, 1.5F, -40, 0, 0, 2.0F, 0, 0, 0)
            .rot("right_arm", 0, 0, 0, 0, 0.4F, -30, 0, 75, 1.5F, -30, 0, 70, 2.0F, 0, 0, 0)
            .rot("left_arm", 0, 0, 0, 0, 0.4F, -30, 0, -75, 1.5F, -30, 0, -70, 2.0F, 0, 0, 0)
            .rot("right_forearm", 0, 0, 0, 0, 0.4F, -50, 0, 0, 1.5F, -50, 0, 0, 2.0F, 0, 0, 0)
            .rot("left_forearm", 0, 0, 0, 0, 0.4F, -50, 0, 0, 1.5F, -50, 0, 0, 2.0F, 0, 0, 0)
            .build();
    private static final AnimationDefinition[] MOVES = {null, POUND, SWEEP, BARRAGE, CHARGE, ERUPTION, OVERHEAT};

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public ForgeColossusModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("root");
        ModelPart torso = body.getChild("hips").getChild("torso");
        this.head = torso.getChild("head");
        this.rightArm = torso.getChild("right_arm");
        this.leftArm = torso.getChild("left_arm");
        this.rightLeg = body.getChild("right_leg");
        this.leftLeg = body.getChild("left_leg");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ForgeColossus entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        if (entity.currentMove() == 0) {
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
            head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
            float walk = Mth.cos(limbSwing * 0.45F) * 0.6F * Math.min(1, limbSwingAmount * 1.5F);
            rightLeg.xRot = walk;
            leftLeg.xRot = -walk;
            rightArm.xRot = -walk * 0.6F;
            leftArm.xRot = walk * 0.6F;
        }
        animate(entity.idle, IDLE, ageInTicks);
        for (int i = 1; i < MOVES.length; i++) {
            animate(entity.moveStates[i], MOVES[i], ageInTicks);
        }
    }
}

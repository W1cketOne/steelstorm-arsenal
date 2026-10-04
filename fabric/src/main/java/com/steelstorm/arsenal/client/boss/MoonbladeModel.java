package com.steelstorm.arsenal.client.boss;

import com.steelstorm.arsenal.entity.boss.MoonbladeRevenant;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The Moonblade Revenant: a floating four-armed swordswoman, every move keyframed. */
public class MoonbladeModel extends HierarchicalModel<MoonbladeRevenant> {
    static final AnimationDefinition IDLE = Anim.of(2.4F).loop()
            .pos("root", 0, 0, 0, 0, 1.2F, 0, -1.5F, 0, 2.4F, 0, 0, 0)
            .rot("tail", 0, 8, 0, 0, 1.2F, -6, 0, 4, 2.4F, 8, 0, 0)
            .rot("skirt", 0, 4, 0, 0, 1.2F, -3, 0, -2, 2.4F, 4, 0, 0)
            .rot("upper_right_arm", 0, -20, 0, 30, 1.2F, -26, 0, 34, 2.4F, -20, 0, 30)
            .rot("upper_left_arm", 0, -20, 0, -30, 1.2F, -26, 0, -34, 2.4F, -20, 0, -30)
            .rot("lower_right_arm", 0, 10, 0, 18, 1.2F, 14, 0, 14, 2.4F, 10, 0, 18)
            .rot("lower_left_arm", 0, 10, 0, -18, 1.2F, 14, 0, -14, 2.4F, 10, 0, -18)
            .rot("upper_right_blade", 0, -40, 0, 0, 2.4F, -40, 0, 0)
            .rot("upper_left_blade", 0, -40, 0, 0, 2.4F, -40, 0, 0)
            .build();
    static final AnimationDefinition CRESCENTS = Anim.of(1.5F)
            .rot("upper_right_arm", 0, 0, 0, 0, 0.35F, -170, 0, 20, 0.5F, -40, 0, -30, 1.5F, 0, 0, 0)
            .rot("upper_left_arm", 0, 0, 0, 0, 0.6F, -170, 0, -20, 0.75F, -40, 0, 30, 1.5F, 0, 0, 0)
            .rot("lower_right_arm", 0, 0, 0, 0, 0.85F, -150, 0, 60, 1.0F, -60, 0, -10, 1.5F, 0, 0, 0)
            .rot("body", 0, 0, 0, 0, 0.5F, 0, -25, 0, 0.75F, 0, 25, 0, 1.0F, 0, -15, 0, 1.5F, 0, 0, 0)
            .build();
    static final AnimationDefinition BLINK = Anim.of(1.4F)
            .scale("root", 0, 1, 1, 1, 0.18F, 0.15F, 1.4F, 0.15F, 0.22F, 0.15F, 1.4F, 0.15F, 0.32F, 1, 1, 1, 1.4F, 1, 1, 1)
            .rot("upper_right_arm", 0, 0, 0, 0, 0.35F, -150, 0, -60, 0.6F, -60, 0, 50, 1.0F, -60, 0, 50, 1.4F, 0, 0, 0)
            .rot("upper_left_arm", 0, 0, 0, 0, 0.35F, -150, 0, 60, 0.6F, -60, 0, -50, 1.0F, -60, 0, -50, 1.4F, 0, 0, 0)
            .rot("body", 0, 0, 0, 0, 0.6F, 15, 0, 0, 1.0F, 15, 0, 0, 1.4F, 0, 0, 0)
            .build();
    static final AnimationDefinition DANCE = Anim.of(2.5F)
            .rot("body", 0, 0, 0, 0, 0.4F, 0, 0, 0, 2.2F, 0, 1080, 0, 2.5F, 0, 1080, 0)
            .rot("upper_right_arm", 0, 0, 0, 0, 0.4F, -10, 0, 95, 2.2F, -10, 0, 95, 2.5F, 0, 0, 0)
            .rot("upper_left_arm", 0, 0, 0, 0, 0.4F, -10, 0, -95, 2.2F, -10, 0, -95, 2.5F, 0, 0, 0)
            .rot("lower_right_arm", 0, 0, 0, 0, 0.4F, 30, 0, 80, 2.2F, 30, 0, 80, 2.5F, 0, 0, 0)
            .rot("lower_left_arm", 0, 0, 0, 0, 0.4F, 30, 0, -80, 2.2F, 30, 0, -80, 2.5F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.4F, -15, 0, 0, 2.2F, -15, 0, 0, 2.5F, 0, 0, 0)
            .build();
    static final AnimationDefinition MOONFALL = Anim.of(3.0F)
            .rot("upper_right_arm", 0, 0, 0, 0, 0.5F, -175, 0, 15, 2.5F, -175, 0, 15, 3.0F, 0, 0, 0)
            .rot("upper_left_arm", 0, 0, 0, 0, 0.5F, -175, 0, -15, 2.5F, -175, 0, -15, 3.0F, 0, 0, 0)
            .rot("lower_right_arm", 0, 0, 0, 0, 0.5F, -100, 0, 50, 2.5F, -100, 0, 50, 3.0F, 0, 0, 0)
            .rot("lower_left_arm", 0, 0, 0, 0, 0.5F, -100, 0, -50, 2.5F, -100, 0, -50, 3.0F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.5F, -35, 0, 0, 2.5F, -35, 0, 0, 3.0F, 0, 0, 0)
            .pos("root", 0, 0, 0, 0, 0.5F, 0, -5, 0, 2.5F, 0, -5, 0, 3.0F, 0, 0, 0)
            .build();
    static final AnimationDefinition ECLIPSE = Anim.of(2.0F)
            .pos("root", 0, 0, 0, 0, 0.8F, 0, -9, 0, 1.4F, 0, -9, 0, 2.0F, 0, 0, 0)
            .rot("body", 0, 0, 0, 0, 0.8F, -20, 0, 0, 1.4F, -20, 0, 0, 2.0F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.8F, -50, 0, 0, 1.4F, -50, 0, 0, 2.0F, 0, 0, 0)
            .rot("upper_right_arm", 0, 0, 0, 0, 0.8F, -60, 0, 110, 1.4F, -60, 0, 110, 2.0F, 0, 0, 0)
            .rot("upper_left_arm", 0, 0, 0, 0, 0.8F, -60, 0, -110, 1.4F, -60, 0, -110, 2.0F, 0, 0, 0)
            .rot("lower_right_arm", 0, 0, 0, 0, 0.8F, 0, 0, 70, 1.4F, 0, 0, 70, 2.0F, 0, 0, 0)
            .rot("lower_left_arm", 0, 0, 0, 0, 0.8F, 0, 0, -70, 1.4F, 0, 0, -70, 2.0F, 0, 0, 0)
            .build();
    private static final AnimationDefinition[] MOVES = {null, CRESCENTS, BLINK, DANCE, MOONFALL, ECLIPSE};

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;

    public MoonbladeModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("root").getChild("body");
        this.head = body.getChild("head");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(MoonbladeRevenant entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        if (entity.currentMove() == 0) {
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            // Leans into her glide.
            body.xRot = Math.min(1, limbSwingAmount * 2) * 0.35F;
        }
        animate(entity.idle, IDLE, ageInTicks);
        for (int i = 1; i < MOVES.length; i++) {
            animate(entity.moveStates[i], MOVES[i], ageInTicks, entity.enraged() ? 1.25F : 1.0F);
        }
    }
}

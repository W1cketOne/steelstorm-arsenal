package com.steelstorm.arsenal.client.anim;

import static com.steelstorm.arsenal.client.anim.Keyframes.key;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.anim.CastPose;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Every weapon's swing, idle hold and ability pose, in first person (moving the held item) and
 * third person (moving arms and body). Swings alternate between two cuts on consecutive attacks.
 */
public final class WeaponAnimator {
    private static final float N = Float.NaN;

    // ------------------------------------------------------------------ third person
    // Channels: right arm x/y/z rotation, left arm x/y/z rotation, body yaw, forward reach.

    private static final Map<WeaponType, float[]> HOLD = new EnumMap<>(WeaponType.class);
    private static final Map<WeaponType, Keyframes[]> SWING_TP = new EnumMap<>(WeaponType.class);
    private static final Map<CastPose, Keyframes> CAST_TP = new EnumMap<>(CastPose.class);

    // ------------------------------------------------------------------ first person
    // Channels: dx, dy, dz, rotation x/y/z (degrees), applied on top of the resting hand position.

    private static final Map<WeaponType, Keyframes[]> SWING_FP = new EnumMap<>(WeaponType.class);
    private static final Map<CastPose, Keyframes> CAST_FP = new EnumMap<>(CastPose.class);

    static {
        float r = -0.35F;
        HOLD.put(WeaponType.LONGSWORD, new float[]{r, 0, 0, N, N, N});
        HOLD.put(WeaponType.KATANA, new float[]{r, 0, 0, N, N, N});
        HOLD.put(WeaponType.DUAL_DAGGERS, new float[]{r, 0, 0, N, N, N});
        HOLD.put(WeaponType.BATTLEAXE, new float[]{r, 0, 0, N, N, N});
        HOLD.put(WeaponType.GREATSWORD, new float[]{-0.95F, -0.45F, 0, -1.05F, 0.55F, 0});
        HOLD.put(WeaponType.SPEAR, new float[]{-0.55F, -0.1F, 0, -0.85F, 0.45F, 0});
        HOLD.put(WeaponType.WARHAMMER, new float[]{-0.6F, -0.35F, 0, -0.75F, 0.55F, 0});
        HOLD.put(WeaponType.SCYTHE, new float[]{-0.4F, -0.3F, 0, -0.95F, 0.6F, 0});

        oneHanded(WeaponType.LONGSWORD,
                new float[]{-2.6F, 0.35F, 0.25F, 0.35F, 0}, new float[]{-0.75F, -0.75F, -0.2F, -0.45F, 1.5F},
                new float[]{-2.3F, -0.7F, -0.3F, -0.3F, 0}, new float[]{-0.8F, 0.6F, 0.3F, 0.45F, 1.5F}, 0.22F, 0.58F);
        oneHanded(WeaponType.KATANA,
                new float[]{-1.45F, 0.9F, 0, 0.5F, 0}, new float[]{-1.45F, -1.05F, 0, -0.6F, 1.5F},
                new float[]{-1.45F, -1.0F, 0, -0.5F, 0}, new float[]{-1.45F, 0.95F, 0, 0.6F, 1.5F}, 0.15F, 0.5F);
        oneHanded(WeaponType.DUAL_DAGGERS,
                new float[]{-1.2F, 0, 0, 0, -1}, new float[]{-1.55F, -0.1F, 0, -0.25F, 3.5F},
                new float[]{-1.0F, 0.6F, 0, 0.3F, 0}, new float[]{-1.2F, -0.8F, 0, -0.4F, 1.5F}, 0.25F, 0.45F);
        oneHanded(WeaponType.BATTLEAXE,
                new float[]{-2.8F, 0.5F, 0.3F, 0.45F, 0}, new float[]{-0.6F, -0.85F, -0.2F, -0.55F, 1.5F},
                new float[]{-2.7F, -0.6F, -0.3F, -0.45F, 0}, new float[]{-0.6F, 0.75F, 0.2F, 0.55F, 1.5F}, 0.25F, 0.58F);
        twoHanded(WeaponType.GREATSWORD,
                new float[]{-2.9F, -0.15F, 0, -2.9F, 0.15F, 0, 0, 0}, new float[]{-0.45F, -0.35F, 0, -0.55F, 0.45F, 0, 0, 2}, 0.3F, 0.62F);
        twoHanded(WeaponType.SPEAR,
                new float[]{-1.2F, -0.1F, 0, -1.3F, 0.45F, 0, 0, -1.5F}, new float[]{-1.55F, -0.05F, 0, -1.6F, 0.35F, 0, -0.2F, 4}, 0.25F, 0.5F);
        twoHanded(WeaponType.WARHAMMER,
                new float[]{-3.0F, -0.1F, 0, -3.0F, 0.1F, 0, 0, 0}, new float[]{-0.35F, -0.25F, 0, -0.45F, 0.35F, 0, 0, 2}, 0.35F, 0.62F);
        twoHanded(WeaponType.SCYTHE,
                new float[]{-1.2F, 0.9F, 0, -1.3F, 1.4F, 0, 0.6F, 0}, new float[]{-1.25F, -1.1F, 0, -1.2F, -0.4F, 0, -0.8F, 1}, 0.2F, 0.55F);

        CAST_TP.put(CastPose.RAISE, hold(0.15F, 0.85F, -3.0F, -0.2F, 0, -3.0F, 0.2F, 0, 0, 0));
        CAST_TP.put(CastPose.THRUST, hold(0.2F, 0.8F, -1.55F, -0.1F, 0, -1.2F, 0.5F, 0, -0.2F, 4));
        CAST_TP.put(CastPose.SPIN, hold(0.12F, 0.88F, -0.3F, 0, -1.3F, -0.3F, 0, 1.3F, 0, 0));
        CAST_TP.put(CastPose.LEAP, hold(0.1F, 0.9F, -2.7F, -0.2F, 0, -2.7F, 0.2F, 0, 0, 0));
        CAST_TP.put(CastPose.STANCE, hold(0.1F, 0.9F, -1.25F, -0.75F, 0, -1.15F, 0.75F, 0, 0, 1));
        CAST_TP.put(CastPose.ROAR, hold(0.15F, 0.85F, -0.6F, 0, -1.2F, -0.6F, 0, 1.2F, 0, 0));
        CAST_TP.put(CastPose.DODGE, hold(0.1F, 0.9F, -1.6F, 0, 0, -1.6F, 0, 0, 0, 0));
        CAST_TP.put(CastPose.SLAM, Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
                key(0.35F, -3.0F, -0.1F, 0, -3.0F, 0.1F, 0, 0, 0), key(0.55F, -0.3F, -0.2F, 0, -0.35F, 0.3F, 0, 0, 2),
                key(0.85F, -0.3F, -0.2F, 0, -0.35F, 0.3F, 0, 0, 2), key(1, N, N, N, N, N, N, 0, 0)));
        CAST_TP.put(CastPose.THROW, Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
                key(0.35F, -2.9F, 0.2F, 0, -0.5F, 0, 0.3F, 0.4F, 0), key(0.55F, -1.3F, -0.1F, 0, -0.4F, 0, 0, -0.3F, 2),
                key(1, N, N, N, N, N, N, 0, 0)));
        CAST_TP.put(CastPose.SWEEP, Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
                key(0.2F, -1.3F, 1.0F, 0, -1.2F, 1.4F, 0, 0.6F, 0), key(0.6F, -1.3F, -1.2F, 0, -1.2F, -0.4F, 0, -0.8F, 1),
                key(1, N, N, N, N, N, N, 0, 0)));
        CAST_TP.put(CastPose.DRAW, Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
                key(0.1F, -1.3F, 0.9F, 0, N, N, N, 0.5F, 0), key(0.4F, -1.4F, -1.2F, 0, N, N, N, -0.7F, 1),
                key(0.8F, -1.4F, -1.2F, 0, N, N, N, -0.7F, 1), key(1, N, N, N, N, N, N, 0, 0)));
        CAST_TP.put(CastPose.RISING, Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
                key(0.15F, 0.3F, 0.2F, 0, N, N, N, 0.2F, 0), key(0.55F, -3.0F, -0.3F, 0, N, N, N, -0.2F, 1),
                key(1, N, N, N, N, N, N, 0, 0)));

        SWING_FP.put(WeaponType.LONGSWORD, pairFp(new float[]{0.15F, 0.15F, 0.05F, -25, 0, -35}, new float[]{-0.35F, -0.2F, -0.15F, 40, 35, 55},
                new float[]{-0.2F, 0.2F, 0.05F, -20, 10, 40}, new float[]{0.3F, -0.2F, -0.15F, 40, -30, -55}, 0.15F, 0.5F));
        SWING_FP.put(WeaponType.GREATSWORD, pairFp(new float[]{0.05F, 0.35F, 0.15F, -60, 0, -10}, new float[]{-0.1F, -0.35F, -0.25F, 70, 15, 10},
                new float[]{0.1F, 0.3F, 0.15F, -55, 0, 15}, new float[]{0.05F, -0.35F, -0.25F, 70, -15, -10}, 0.3F, 0.6F));
        SWING_FP.put(WeaponType.KATANA, pairFp(new float[]{0.3F, 0, 0.1F, 0, -40, -70}, new float[]{-0.45F, 0.05F, -0.2F, 0, 50, -80},
                new float[]{-0.4F, 0.05F, 0.1F, 0, 45, -80}, new float[]{0.35F, 0, -0.2F, 0, -45, -70}, 0.12F, 0.45F));
        SWING_FP.put(WeaponType.DUAL_DAGGERS, pairFp(new float[]{0, 0, 0.12F, 5, 0, 0}, new float[]{0, 0.05F, -0.45F, -20, 0, 0},
                new float[]{0.25F, 0.05F, 0, 0, -30, -50}, new float[]{-0.3F, -0.05F, -0.15F, 0, 35, -60}, 0.25F, 0.45F));
        SWING_FP.put(WeaponType.SPEAR, pairFp(new float[]{0, 0, 0.15F, 0, 0, 0}, new float[]{0, 0.05F, -0.6F, -10, 0, 0},
                new float[]{0.05F, 0, 0.15F, 0, -5, 0}, new float[]{-0.05F, 0.05F, -0.6F, -10, 5, 0}, 0.25F, 0.5F));
        SWING_FP.put(WeaponType.WARHAMMER, pairFp(new float[]{0.05F, 0.45F, 0.2F, -75, 0, 0}, new float[]{0, -0.4F, -0.3F, 60, 0, 0},
                new float[]{0.05F, 0.45F, 0.2F, -75, 0, 10}, new float[]{0, -0.4F, -0.3F, 60, 0, -10}, 0.35F, 0.6F));
        SWING_FP.put(WeaponType.SCYTHE, pairFp(new float[]{0.45F, 0.1F, 0, 0, -50, -45}, new float[]{-0.55F, -0.05F, -0.2F, 0, 60, -45},
                new float[]{-0.45F, 0.1F, 0, 0, 50, -45}, new float[]{0.5F, -0.05F, -0.2F, 0, -55, -45}, 0.2F, 0.55F));
        SWING_FP.put(WeaponType.BATTLEAXE, pairFp(new float[]{0.25F, 0.3F, 0.1F, -50, 0, -30}, new float[]{-0.3F, -0.3F, -0.2F, 55, 25, 40},
                new float[]{-0.2F, 0.3F, 0.1F, -50, 0, 30}, new float[]{0.3F, -0.3F, -0.2F, 55, -25, -40}, 0.25F, 0.55F));

        CAST_FP.put(CastPose.RAISE, holdFp(0.15F, 0.85F, 0, 0.35F, 0.1F, -60, 0, 0));
        CAST_FP.put(CastPose.THRUST, holdFp(0.2F, 0.75F, 0, 0.02F, -0.55F, -8, 0, 0));
        CAST_FP.put(CastPose.LEAP, holdFp(0.1F, 0.9F, 0, 0.4F, 0, -70, 0, 0));
        CAST_FP.put(CastPose.STANCE, holdFp(0.1F, 0.9F, -0.15F, 0.1F, -0.05F, 0, -25, -75));
        CAST_FP.put(CastPose.ROAR, holdFp(0.15F, 0.85F, 0.1F, -0.25F, 0.1F, 25, 0, 15));
        CAST_FP.put(CastPose.DODGE, holdFp(0.1F, 0.9F, 0, -0.3F, 0, 20, 0, 0));
        CAST_FP.put(CastPose.SLAM, Keyframes.of(key(0, 0, 0, 0, 0, 0, 0), key(0.35F, 0.05F, 0.45F, 0.2F, -75, 0, 0),
                key(0.55F, 0, -0.4F, -0.3F, 60, 0, 0), key(0.85F, 0, -0.4F, -0.3F, 60, 0, 0), key(1, 0, 0, 0, 0, 0, 0)));
        CAST_FP.put(CastPose.THROW, Keyframes.of(key(0, 0, 0, 0, 0, 0, 0), key(0.35F, 0.15F, 0.3F, 0.25F, -50, 0, -15),
                key(0.55F, -0.05F, 0, -0.5F, 30, 0, 0), key(1, 0, 0, 0, 0, 0, 0)));
        CAST_FP.put(CastPose.SWEEP, Keyframes.of(key(0, 0, 0, 0, 0, 0, 0), key(0.2F, 0.45F, 0.1F, 0, 0, -50, -45),
                key(0.6F, -0.55F, -0.05F, -0.2F, 0, 60, -45), key(1, 0, 0, 0, 0, 0, 0)));
        CAST_FP.put(CastPose.DRAW, Keyframes.of(key(0, 0, 0, 0, 0, 0, 0), key(0.1F, 0.3F, 0, 0.1F, 0, -40, -70),
                key(0.4F, -0.45F, 0.05F, -0.2F, 0, 50, -80), key(0.8F, -0.45F, 0.05F, -0.2F, 0, 50, -80), key(1, 0, 0, 0, 0, 0, 0)));
        CAST_FP.put(CastPose.RISING, Keyframes.of(key(0, 0, 0, 0, 0, 0, 0), key(0.15F, 0, -0.3F, 0, 40, 0, 0),
                key(0.5F, 0, 0.4F, 0, -70, 0, 0), key(1, 0, 0, 0, 0, 0, 0)));
    }

    private static void oneHanded(WeaponType type, float[] upA, float[] downA, float[] upB, float[] downB, float tUp, float tDown) {
        SWING_TP.put(type, new Keyframes[]{oneHandedTrack(upA, downA, tUp, tDown), oneHandedTrack(upB, downB, tUp, tDown)});
    }

    /** {rx, ry, rz, body, reach} at the wind-up and the end of the cut; the hold pose either side. */
    private static Keyframes oneHandedTrack(float[] up, float[] down, float tUp, float tDown) {
        return Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
                key(tUp, up[0], up[1], up[2], N, N, N, up[3], up[4]),
                key(tDown, down[0], down[1], down[2], N, N, N, down[3], down[4]),
                key(1, N, N, N, N, N, N, 0, 0));
    }

    private static void twoHanded(WeaponType type, float[] up, float[] down, float tUp, float tDown) {
        Keyframes track = Keyframes.of(key(0, N, N, N, N, N, N, 0, 0), key(tUp, up), key(tDown, down), key(1, N, N, N, N, N, N, 0, 0));
        float[] upB = up.clone();
        float[] downB = down.clone();
        upB[1] = -up[4];
        upB[4] = -up[1];
        downB[1] = -down[4];
        downB[4] = -down[1];
        upB[6] = -up[6];
        downB[6] = -down[6];
        Keyframes mirrored = Keyframes.of(key(0, N, N, N, N, N, N, 0, 0), key(tUp, upB), key(tDown, downB), key(1, N, N, N, N, N, N, 0, 0));
        SWING_TP.put(type, new Keyframes[]{track, mirrored});
    }

    private static Keyframes hold(float in, float out, float... v) {
        return Keyframes.of(key(0, N, N, N, N, N, N, 0, 0), key(in, v), key(out, v), key(1, N, N, N, N, N, N, 0, 0));
    }

    private static Keyframes[] pairFp(float[] upA, float[] downA, float[] upB, float[] downB, float tUp, float tDown) {
        float[] zero = new float[6];
        return new Keyframes[]{
                Keyframes.of(key(0, zero), key(tUp, upA), key(tDown, downA), key(1, zero)),
                Keyframes.of(key(0, zero), key(tUp, upB), key(tDown, downB), key(1, zero))};
    }

    private static Keyframes holdFp(float in, float out, float... v) {
        float[] zero = new float[6];
        return Keyframes.of(key(0, zero), key(in, v), key(out, v), key(1, zero));
    }

    // ------------------------------------------------------------------ third person

    /** Poses a humanoid holding a weapon: idle hold, swing, or ability pose. */
    public static void poseThirdPerson(HumanoidModel<?> model, LivingEntity entity, WeaponType type, float partialTick) {
        float swing = model.attackTime;
        float[] hold = HOLD.get(type);
        float walkR = model.rightArm.xRot;
        float walkL = model.leftArm.xRot;
        model.rightArm.xRot = walkR * (Float.isNaN(hold[3]) ? 0.5F : 0.25F) + hold[0];
        model.rightArm.yRot = hold[1];
        model.rightArm.zRot = hold[2];
        if (!Float.isNaN(hold[3])) {
            model.leftArm.xRot = walkL * 0.25F + hold[3];
            model.leftArm.yRot = hold[4];
            model.leftArm.zRot = hold[5];
        }

        ClientAnims.Active cast = ClientAnims.get(entity, partialTick);
        Keyframes track = null;
        float t = 0;
        if (cast != null && CAST_TP.containsKey(cast.pose())) {
            track = CAST_TP.get(cast.pose());
            t = cast.t();
        } else {
            float progress = SwingClock.progress(entity, swing, type, partialTick);
            if (progress > 0) {
                track = SWING_TP.get(type)[SwingClock.cut(entity)];
                t = progress;
            }
        }
        // Our own swing replaces the vanilla one.
        model.attackTime = 0;
        if (track == null) {
            return;
        }
        float[] v = new float[8];
        float[] base = {model.rightArm.xRot, model.rightArm.yRot, model.rightArm.zRot,
                model.leftArm.xRot, model.leftArm.yRot, model.leftArm.zRot, 0, 0};
        track.sample(t, v, base);
        apply(model.rightArm, v[0], v[1], v[2]);
        apply(model.leftArm, v[3], v[4], v[5]);
        if (cast != null && cast.pose() == CastPose.FLURRY) {
            model.rightArm.xRot = -1.4F + 0.3F * Mth.sin(cast.t() * Mth.PI * 10);
            v[7] = 2.0F + 1.5F * Mth.sin(cast.t() * Mth.PI * 20);
        }
        if (cast != null && (cast.pose() == CastPose.LEAP || cast.pose() == CastPose.DODGE)) {
            float tuck = Mth.sin(cast.t() * Mth.PI);
            model.rightLeg.xRot = -1.1F * tuck;
            model.leftLeg.xRot = -0.5F * tuck;
        }
        if (cast != null && cast.pose() == CastPose.ROAR) {
            model.head.xRot = -0.6F * Mth.sin(cast.t() * Mth.PI);
        }
        float body = Float.isNaN(v[6]) ? 0 : v[6];
        if (body != 0) {
            model.body.yRot = body;
            model.rightArm.z = Mth.sin(body) * 5.0F;
            model.rightArm.x = -Mth.cos(body) * 5.0F;
            model.leftArm.z = -Mth.sin(body) * 5.0F;
            model.leftArm.x = Mth.cos(body) * 5.0F;
            model.rightArm.yRot += body;
            model.leftArm.yRot += body;
        }
        float reach = Float.isNaN(v[7]) ? 0 : v[7];
        model.rightArm.z -= reach;
        if (!Float.isNaN(v[3]) || !Float.isNaN(hold[3])) {
            model.leftArm.z -= reach * 0.8F;
        }
    }

    private static void apply(net.minecraft.client.model.geom.ModelPart part, float x, float y, float z) {
        if (!Float.isNaN(x)) {
            part.xRot = x;
        }
        if (!Float.isNaN(y)) {
            part.yRot = y;
        }
        if (!Float.isNaN(z)) {
            part.zRot = z;
        }
    }

    // ------------------------------------------------------------------ first person

    /**
     * Places the held weapon in first person. Includes vanilla's resting arm position, so the
     * caller can skip vanilla's own transforms.
     */
    public static void poseFirstPerson(PoseStack pose, LivingEntity player, HumanoidArm arm, WeaponType type, float partialTick,
                                       float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        ClientAnims.Active cast = ClientAnims.get(player, partialTick);
        float[] v = new float[6];
        if (cast != null && cast.pose() == CastPose.SPIN) {
            pose.translate(side * -0.1F, 0.05F, -0.1F);
            pose.mulPose(Axis.YP.rotationDegrees(side * -360 * Mth.sin(cast.t() * Mth.HALF_PI)));
            pose.mulPose(Axis.ZP.rotationDegrees(side * -60 * Mth.sin(cast.t() * Mth.PI)));
            return;
        }
        if (cast != null && cast.pose() == CastPose.FLURRY) {
            float stab = Math.abs(Mth.sin(cast.t() * Mth.PI * 6));
            pose.translate(side * 0.1F * Mth.sin(cast.t() * 37), 0.02F, -0.4F * stab);
            return;
        }
        if (cast != null && CAST_FP.containsKey(cast.pose())) {
            CAST_FP.get(cast.pose()).sample(cast.t(), v);
            if (cast.pose() == CastPose.RAISE) {
                v[0] += 0.01F * Mth.sin(cast.ticks() * 3.1F);
                v[1] += 0.01F * Mth.sin(cast.ticks() * 2.3F);
            }
        } else {
            float progress = SwingClock.progress(player, swing, type, partialTick);
            if (progress <= 0) {
                return;
            }
            SWING_FP.get(type)[SwingClock.cut(player)].sample(progress, v);
        }
        pose.translate(side * v[0], v[1], v[2]);
        pose.mulPose(Axis.XP.rotationDegrees(v[3]));
        pose.mulPose(Axis.YP.rotationDegrees(side * v[4]));
        pose.mulPose(Axis.ZP.rotationDegrees(side * v[5]));
    }

    public static boolean isTwoHanded(WeaponType type) {
        return type == WeaponType.GREATSWORD || type == WeaponType.SPEAR || type == WeaponType.WARHAMMER || type == WeaponType.SCYTHE;
    }

    private WeaponAnimator() {
    }
}

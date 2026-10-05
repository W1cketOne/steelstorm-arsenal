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
    /** First-person shoulder pivot relative to the camera, and how much of each attack's arc it shows. */
    private static float[] FP_SHOULDER = {0.05F, -0.45F, 0.4F};
    /** How much of each attack's yaw, pitch and roll first person shows. */
    private static float[] FP_SCALE = {0.85F, 0.6F, 0.8F};
    private static long tuneRead;
    /** Dev aid: with STEELSTORM_ANIMLOG=path set, every first-person frame's attack pose is logged there. */
    private static final java.io.PrintWriter ANIM_LOG = openAnimLog();

    private static java.io.PrintWriter openAnimLog() {
        String path = System.getenv("STEELSTORM_ANIMLOG");
        if (path == null) {
            return null;
        }
        try {
            return new java.io.PrintWriter(new java.io.FileWriter(path));
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Dev aid: config/steelstorm-anim-tune.properties, if present, overrides the first-person arc live. */
    private static void tune() {
        long now = System.currentTimeMillis();
        if (now - tuneRead < 1000) {
            return;
        }
        tuneRead = now;
        java.nio.file.Path file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("steelstorm-anim-tune.properties");
        if (!java.nio.file.Files.exists(file)) {
            return;
        }
        try (var in = java.nio.file.Files.newBufferedReader(file)) {
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            FP_SHOULDER = floats(p.getProperty("shoulder"), FP_SHOULDER);
            FP_SCALE = floats(p.getProperty("scale"), FP_SCALE);
        } catch (Exception ignored) {
        }
    }

    private static float[] floats(String s, float[] def) {
        if (s == null) {
            return def;
        }
        String[] parts = s.split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Float.parseFloat(parts[i].trim());
        }
        return out;
    }

    // ------------------------------------------------------------------ third person
    // Channels: right arm x/y/z rotation, left arm x/y/z rotation, body yaw, forward reach.

    private static final Map<WeaponType, float[]> HOLD = new EnumMap<>(WeaponType.class);
    private static final Map<WeaponType, Keyframes[]> SWING_TP = new EnumMap<>(WeaponType.class);
    private static final Map<CastPose, Keyframes> CAST_TP = new EnumMap<>(CastPose.class);

    // ------------------------------------------------------------------ first person
    // Channels: dx, dy, dz, rotation x/y/z (degrees), applied on top of the resting hand position.

    private static final Map<WeaponType, Keyframes[]> SWING_FP = new EnumMap<>(WeaponType.class);
    private static final Map<CastPose, Keyframes> CAST_FP = new EnumMap<>(CastPose.class);
    /** The combo finisher: a big overhead chop, whatever the weapon. */
    private static final Keyframes FINISHER_TP = Keyframes.of(key(0, N, N, N, N, N, N, 0, 0),
            key(0.3F, -3.1F, -0.1F, 0, -3.0F, 0.1F, 0, 0, 0), key(0.5F, -0.2F, -0.1F, 0, -0.3F, 0.2F, 0, 0, 2.5F),
            key(0.8F, -0.2F, -0.1F, 0, -0.3F, 0.2F, 0, 0, 2.5F), key(1, N, N, N, N, N, N, 0, 0));
    private static final Keyframes FINISHER_FP = Keyframes.of(key(0, 0, 0, 0, 0, 0, 0), key(0.3F, 0.0F, 0.5F, 0.25F, -85, 0, 0),
            key(0.5F, -0.05F, -0.45F, -0.35F, 70, 0, 0), key(0.8F, -0.05F, -0.45F, -0.35F, 70, 0, 0), key(1, 0, 0, 0, 0, 0, 0));

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

        CAST_TP.put(CastPose.INSPECT, hold(0.15F, 0.85F, -1.35F, -0.55F, 0.1F, N, N, N, 0, 0));
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
            float[] a = new float[AttackAnims.CHANNELS];
            if (SwingClock.sample(entity, swing, type, partialTick, a)) {
                model.attackTime = 0;
                // Blend from whatever the body was doing (running, resting, jumping), so the attack
                // neither snaps in from nor snaps back to a different pose.
                runAndJump(model, entity, type, partialTick);
                attackThirdPerson(model, entity, type, a);
                return;
            }
        }
        // Our own swing replaces the vanilla one.
        model.attackTime = 0;
        if (track == null) {
            runAndJump(model, entity, type, partialTick);
            return;
        }
        float[] v = new float[8];
        float[] base = {model.rightArm.xRot, model.rightArm.yRot, model.rightArm.zRot,
                model.leftArm.xRot, model.leftArm.yRot, model.leftArm.zRot, 0, 0};
        track.sample(t, v, base);
        // Keep third-person swings readable: the arms move part of the way, the body turns less, and
        // the arm only reaches forward a little instead of detaching from the shoulder.
        for (int i = 0; i < 6; i++) {
            if (!Float.isNaN(v[i])) {
                v[i] = base[i] + (v[i] - base[i]) * 0.72F;
            }
        }
        if (!Float.isNaN(v[6])) {
            v[6] *= 0.6F;
        }
        if (!Float.isNaN(v[7])) {
            v[7] *= 0.3F;
        }
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

    /** Forward lean for each model this frame, applied after vanilla's setupAnim resets the body (see HumanoidModelMixin). */
    public static final java.util.Map<HumanoidModel<?>, Float> LEAN = new java.util.IdentityHashMap<>();

    /**
     * Poses the whole body for a melee attack: the weapon arm swings about the shoulder along the
     * attack's arc, a two-handed grip brings the off hand with it, the free arm counterbalances,
     * the torso twists into the cut and the lead leg steps forward.
     */
    private static void attackThirdPerson(HumanoidModel<?> model, LivingEntity entity, WeaponType type, float[] a) {
        float w = a[7];
        float yaw = a[0] * Mth.DEG_TO_RAD;
        float pitch = a[1] * Mth.DEG_TO_RAD;
        float roll = a[2] * Mth.DEG_TO_RAD;
        boolean two = isTwoHanded(type);
        float rx = -0.7F - pitch * 0.9F;
        float ry = -yaw * 0.85F;
        float rz = -roll * 0.12F;
        blend(model.rightArm, rx, ry, rz, w);
        if (two) {
            blend(model.leftArm, rx + 0.08F, ry + 0.55F, -rz, w);
        } else {
            // The free arm swings out the other way for balance.
            blend(model.leftArm, -0.45F + pitch * 0.15F, yaw * 0.35F, -0.35F - Math.abs(yaw) * 0.12F, w * 0.85F);
        }
        float body = a[4];
        if (body != 0) {
            model.body.yRot = body;
            model.rightArm.z = Mth.sin(body) * 5.0F;
            model.rightArm.x = -Mth.cos(body) * 5.0F;
            model.leftArm.z = -Mth.sin(body) * 5.0F;
            model.leftArm.x = Mth.cos(body) * 5.0F;
            model.rightArm.yRot += body;
            model.leftArm.yRot += body;
        }
        float push = a[3] * 6.0F;
        model.rightArm.z -= push;
        if (two) {
            model.leftArm.z -= push * 0.8F;
        }
        float step = a[6];
        model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.55F * step);
        model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.38F * step);
        if (a[5] != 0) {
            LEAN.put(model, a[5]);
        }
    }

    /** Applies this frame's attack lean at the end of setupAnim (vanilla zeroes body.xRot first). */
    public static void applyLean(HumanoidModel<?> model) {
        Float lean = LEAN.remove(model);
        if (lean == null || model.crouching) {
            // Vanilla never resets head.z, so put back anything a lean moved.
            if (LEANED.remove(model) != null) {
                model.head.z = 0;
                model.hat.z = 0;
            }
            return;
        }
        LEANED.put(model, Boolean.TRUE);
        model.body.xRot += lean;
        float dy = Mth.sin(lean) * 12.0F * 0.25F;
        float dz = Mth.sin(lean) * 12.0F * 0.5F;
        model.rightArm.y += dy;
        model.leftArm.y += dy;
        model.head.y = dy;
        model.hat.y = dy;
        model.rightArm.z += dz;
        model.leftArm.z += dz;
        // Set, not add: head.z is never reset by vanilla and would creep away frame after frame.
        model.head.z = dz;
        model.hat.z = dz;
    }

    private static final java.util.Map<HumanoidModel<?>, Boolean> LEANED = new java.util.WeakHashMap<>();

    /** Sprinting carries the weapon low and trailing (or across the chest); jumping raises it and tucks a knee. */
    private static void runAndJump(HumanoidModel<?> model, LivingEntity entity, WeaponType type, float partialTick) {
        MovementAnims.State s = MovementAnims.get(entity);
        float sprint = s.sprint(partialTick);
        float air = s.air(partialTick);
        float land = s.land(partialTick);
        boolean two = isTwoHanded(type);
        float pump = Mth.cos(entity.walkAnimation.position(partialTick) * 0.6662F) * entity.walkAnimation.speed(partialTick);
        if (sprint > 0.001F) {
            if (two) {
                // Port arms: the haft held diagonally across the body, bouncing with each stride.
                blend(model.rightArm, -0.55F + 0.1F * pump, -0.75F, 0.1F, sprint);
                blend(model.leftArm, -1.0F - 0.1F * pump, 0.65F, -0.1F, sprint);
            } else {
                // Weapon arm swept back so the blade trails behind; the free arm pumps hard.
                blend(model.rightArm, 0.75F + 0.25F * pump, 0.25F, 0.35F, sprint);
                blend(model.leftArm, -1.3F * pump, 0, -0.1F, sprint);
            }
        }
        if (air > 0.001F) {
            if (two) {
                blend(model.rightArm, -1.5F, -0.55F, 0, air * 0.8F);
                blend(model.leftArm, -1.7F, 0.55F, 0, air * 0.8F);
            } else {
                blend(model.rightArm, -1.6F, 0.1F, 0.15F, air * 0.7F);
                blend(model.leftArm, -0.5F, 0, -0.9F, air * 0.8F);
            }
            model.rightLeg.xRot = Mth.lerp(air, model.rightLeg.xRot, -0.95F);
            model.leftLeg.xRot = Mth.lerp(air, model.leftLeg.xRot, 0.4F);
        }
        float rest = s.rest(partialTick);
        if (rest > 0.001F) {
            if (two) {
                // Heavy weapons rest on the shoulder, the off hand on the haft.
                blend(model.rightArm, -2.5F, -0.45F, 0.15F, rest);
                blend(model.leftArm, -1.35F, 0.75F, 0, rest);
            } else {
                // Blade lowered and angled out, at ease.
                blend(model.rightArm, -0.1F, 0.1F, 0.2F, rest);
            }
            float breathe = Mth.sin((entity.tickCount + partialTick) * 0.08F) * 0.04F * rest;
            model.rightArm.xRot += breathe;
            model.leftArm.xRot -= breathe;
        }
        if (land > 0.001F) {
            // Arms dip with the impact.
            model.rightArm.xRot += 0.35F * land;
            model.leftArm.xRot += 0.35F * land;
        }
    }

    private static void blend(net.minecraft.client.model.geom.ModelPart part, float x, float y, float z, float t) {
        part.xRot = Mth.lerp(t, part.xRot, x);
        part.yRot = Mth.lerp(t, part.yRot, y);
        part.zRot = Mth.lerp(t, part.zRot, z);
    }

    /** First-person run sway, jump lift and landing dip, added while not swinging or casting. */
    private static void moveFirstPerson(PoseStack pose, LivingEntity player, int side, WeaponType type, float partialTick) {
        moveFirstPerson(pose, player, side, type, partialTick, 1.0F);
    }

    private static void moveFirstPerson(PoseStack pose, LivingEntity player, int side, WeaponType type, float partialTick, float scale) {
        MovementAnims.State s = MovementAnims.get(player);
        float sprint = s.sprint(partialTick) * scale;
        float air = s.air(partialTick) * scale;
        float land = s.land(partialTick) * scale;
        if (sprint <= 0.001F && air <= 0.001F && land <= 0.001F) {
            return;
        }
        float phase = player.walkAnimation.position(partialTick) * 0.6662F;
        float vy = (float) player.getDeltaMovement().y;
        boolean two = isTwoHanded(type);
        // Run: the weapon pulls in toward the centre and tips forward, rocking with each stride.
        float dx = side * (-0.07F * sprint + 0.025F * sprint * Mth.sin(phase));
        float dy = -0.05F * sprint + 0.03F * sprint * Math.abs(Mth.cos(phase)) - 0.12F * land
                + air * Mth.clamp(-vy * 0.35F, -0.12F, 0.14F);
        float dz = 0.04F * sprint;
        pose.translate(dx, dy, dz);
        pose.mulPose(Axis.XP.rotationDegrees((two ? 10 : 16) * sprint + 10 * land - 14 * air));
        pose.mulPose(Axis.ZP.rotationDegrees(side * ((two ? 22 : 14) * sprint + 6 * sprint * Mth.sin(phase))));
        pose.mulPose(Axis.YP.rotationDegrees(side * (8 * air)));
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
        ClientAnims.Active cast = ClientAnims.get(player, partialTick);
        float[] a = null;
        if (cast == null) {
            a = new float[AttackAnims.CHANNELS];
            if (!SwingClock.sampleFp(player, swing, type, partialTick, a)) {
                a = null;
            }
        }
        if (a != null) {
            // The arm swings about the shoulder (below and to the side of the camera), so the
            // weapon sweeps across the view in a real arc instead of sliding around.
            tune();
            pose.translate(side * FP_SHOULDER[0], FP_SHOULDER[1], FP_SHOULDER[2]);
            pose.mulPose(Axis.YP.rotationDegrees(side * a[0] * FP_SCALE[0]));
            // Downward cuts would drop the weapon out of view from a shoulder below the camera, so
            // they show less of their arc than upswings do.
            pose.mulPose(Axis.XP.rotationDegrees(a[1] * (a[1] > 0 ? FP_SCALE[1] : FP_SCALE[1] * 0.45F)));
            pose.translate(-side * FP_SHOULDER[0], -FP_SHOULDER[1], -FP_SHOULDER[2]);
        }
        // Vanilla dips the item while the attack recharges; with big 3D weapons that reads as the
        // weapon bobbing about between swings, so only a hint of it is kept.
        pose.translate(side * 0.56F, -0.52F + equip * -0.08F, -0.72F);
        if (ANIM_LOG != null) {
            float[] v = a == null ? new float[AttackAnims.CHANNELS] : a;
            ANIM_LOG.printf(java.util.Locale.ROOT, "%d %s %.2f %.2f %.2f %.3f %.3f%n", System.nanoTime() / 1000000, type,
                    v[0], v[1], v[2], v[3], v[7]);
            ANIM_LOG.flush();
        }
        if (a != null) {
            // Running/jump sway fades out as the attack takes over and back in as it ends.
            moveFirstPerson(pose, player, side, type, partialTick, 1.0F - a[7]);
            pose.translate(0, 0, -a[3]);
            pose.mulPose(Axis.ZP.rotationDegrees(side * a[2] * FP_SCALE[2]));
            return;
        }
        float[] v = new float[6];
        if (cast != null && cast.pose() == CastPose.SPIN) {
            pose.translate(side * -0.1F, 0.05F, -0.1F);
            pose.mulPose(Axis.YP.rotationDegrees(side * -360 * Mth.sin(cast.t() * Mth.HALF_PI)));
            pose.mulPose(Axis.ZP.rotationDegrees(side * -60 * Mth.sin(cast.t() * Mth.PI)));
            return;
        }
        if (cast != null && cast.pose() == CastPose.INSPECT) {
            inspect(pose, side, cast.t());
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
            moveFirstPerson(pose, player, side, type, partialTick);
            return;
        }
        pose.translate(side * v[0], v[1], v[2]);
        pose.mulPose(Axis.XP.rotationDegrees(v[3]));
        pose.mulPose(Axis.YP.rotationDegrees(side * v[4]));
        pose.mulPose(Axis.ZP.rotationDegrees(side * v[5]));
    }

    /** Brings the weapon in, turns the flat of the blade to the camera, flips it round, then puts it back. */
    private static void inspect(PoseStack pose, int side, float t) {
        float in = smooth(t / 0.18F) * (1 - smooth((t - 0.84F) / 0.16F));
        float turn = smooth((t - 0.15F) / 0.2F) * (1 - smooth((t - 0.8F) / 0.15F));
        float flip = smooth((t - 0.42F) / 0.3F);
        pose.translate(side * -0.2F * in, 0.05F * in + 0.012F * Mth.sin(t * 20) * in, -0.16F * in);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 28 * in));
        pose.mulPose(Axis.YP.rotationDegrees(side * (-70 * turn + 360 * flip)));
        pose.mulPose(Axis.XP.rotationDegrees(-12 * in));
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    public static boolean isTwoHanded(WeaponType type) {
        return type == WeaponType.GREATSWORD || type == WeaponType.SPEAR || type == WeaponType.WARHAMMER || type == WeaponType.SCYTHE;
    }

    private WeaponAnimator() {
    }
}

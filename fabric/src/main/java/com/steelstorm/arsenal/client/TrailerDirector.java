package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.entity.SteelstormBoss;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Dev tool for filming: only active when the STEELSTORM_DIRECTOR environment variable names a
 * script file. Whenever that file changes it is re-read and its shot starts from the top.
 *
 * <pre>
 * cam abs|player|boss [yaw]   keys are world coordinates, or offsets from the player / nearest
 *                             boss (rotated by its facing at the start of the shot with "yaw")
 * cam duel                    offsets from the player in a frame facing the nearest boss (+z
 *                             toward it, +x to the left); look-at offsets from their midpoint
 * k t x y z lx ly lz          camera at (x,y,z) looking at (lx,ly,lz) at t seconds (Catmull-Rom)
 * aim boss|off                keep the player's view on the nearest boss
 * hud on|off|hand             hide the HUD and hand ("hand": just the HUD)
 * me show|hide                hide the player's own body
 * speed s                     script seconds per real second
 * </pre>
 */
public final class TrailerDirector {
    private static final Path SCRIPT = System.getenv("STEELSTORM_DIRECTOR") == null ? null : Path.of(System.getenv("STEELSTORM_DIRECTOR"));
    private static long modified = -1;
    private static long startNanos;
    private static final List<double[]> KEYS = new ArrayList<>();
    private static String frame = "abs";
    private static boolean frameYaw;
    private static float frameYawDeg;
    private static Vec3 frameOrigin = Vec3.ZERO;
    private static boolean aimBoss;
    public static boolean hideSelf;
    /** HUD hidden but the first-person hand kept. */
    public static boolean hideGuiOnly;
    private static boolean hideHud;
    /** Script seconds per real second (filming with a slowed tick rate). */
    private static double speed = 1;
    private static double duelYaw = Double.NaN;

    private TrailerDirector() {
    }

    public static boolean enabled() {
        return SCRIPT != null;
    }

    public static boolean cameraActive() {
        return SCRIPT != null && !KEYS.isEmpty();
    }

    /** Called every client tick: picks up script changes and steers the player's aim. */
    public static void tick(Minecraft mc) {
        if (SCRIPT == null || mc.player == null) {
            return;
        }
        try {
            long m = Files.exists(SCRIPT) ? Files.getLastModifiedTime(SCRIPT).toMillis() : 0;
            if (m != modified) {
                modified = m;
                load(mc, m == 0 ? List.of() : Files.readAllLines(SCRIPT));
            }
        } catch (Exception ignored) {
        }
        mc.options.hideGui = hideHud;
        if (aimBoss) {
            LivingEntity boss = nearestBoss(mc);
            if (boss != null) {
                Vec3 eye = mc.player.getEyePosition();
                Vec3 to = boss.position().add(0, boss.getBbHeight() * 0.6, 0).subtract(eye);
                float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
                float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
                mc.player.setYRot(mc.player.getYRot() + Mth.wrapDegrees(yaw - mc.player.getYRot()) * 0.35F);
                mc.player.setXRot(Mth.clamp(mc.player.getXRot() + (pitch - mc.player.getXRot()) * 0.35F, -60, 60));
                mc.player.setYHeadRot(mc.player.getYRot());
            }
        }
    }

    private static void load(Minecraft mc, List<String> lines) {
        KEYS.clear();
        frame = "abs";
        frameYaw = false;
        aimBoss = false;
        hideSelf = false;
        hideHud = false;
        hideGuiOnly = false;
        speed = 1;
        duelYaw = Double.NaN;
        for (String raw : lines) {
            String[] p = raw.trim().split("\\s+");
            if (p.length == 0 || p[0].isEmpty() || p[0].startsWith("#")) {
                continue;
            }
            switch (p[0]) {
                case "cam" -> {
                    frame = p[1];
                    frameYaw = p.length > 2 && p[2].equals("yaw");
                }
                case "k" -> {
                    double[] k = new double[7];
                    for (int i = 0; i < 7; i++) {
                        k[i] = Double.parseDouble(p[i + 1]);
                    }
                    KEYS.add(k);
                }
                case "aim" -> aimBoss = p[1].equals("boss");
                case "hud" -> {
                    hideHud = p[1].equals("off");
                    hideGuiOnly = p[1].equals("hand");
                }
                case "me" -> hideSelf = p[1].equals("hide");
                case "speed" -> speed = Double.parseDouble(p[1]);
                default -> {
                }
            }
        }
        Entity target = target(mc);
        frameOrigin = target == null ? Vec3.ZERO : target.position();
        // Body, not head: a player standing still can have the two up to 50 degrees apart.
        frameYawDeg = target == null ? 0 : target instanceof LivingEntity living ? living.yBodyRot : target.getYRot();
        startNanos = System.nanoTime();
    }

    @Nullable
    private static Entity target(Minecraft mc) {
        return switch (frame) {
            case "player" -> mc.player;
            case "boss" -> nearestBoss(mc);
            default -> null;
        };
    }

    @Nullable
    private static LivingEntity nearestBoss(Minecraft mc) {
        LivingEntity best = null;
        double bestD = 96 * 96;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof SteelstormBoss boss && boss.isAlive()) {
                double d = e.distanceToSqr(mc.player);
                if (d < bestD) {
                    bestD = d;
                    best = boss;
                }
            }
        }
        return best;
    }

    /** Camera {x, y, z, yaw, pitch} for this frame, or null when no shot is running. */
    @Nullable
    public static double[] camera(float partialTick) {
        if (!cameraActive()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        double t = (System.nanoTime() - startNanos) / 1e9 * speed;
        double[] s = sample(t);
        Vec3 pos = new Vec3(s[0], s[1], s[2]);
        Vec3 look = new Vec3(s[3], s[4], s[5]);
        if (frame.equals("duel") && mc.player != null) {
            Entity boss = nearestBoss(mc);
            Vec3 me = mc.player.getPosition(partialTick);
            Vec3 them = boss == null ? me.add(0, 0, 1) : boss.getPosition(partialTick);
            Vec3 dir = them.subtract(me);
            double want = Math.atan2(dir.x, dir.z);
            if (Double.isNaN(duelYaw)) {
                duelYaw = want;
            }
            // Ease the frame round rather than snapping with every sidestep.
            double diff = Math.atan2(Math.sin(want - duelYaw), Math.cos(want - duelYaw));
            duelYaw += diff * 0.04;
            Vec3 mid = me.add(them).scale(0.5);
            float a = (float) duelYaw;
            pos = me.add(pos.yRot(a));
            look = mid.add(look.yRot(a));
        } else if (!frame.equals("abs")) {
            Entity target = target(mc);
            Vec3 origin = target == null ? frameOrigin : target.getPosition(partialTick);
            if (frameYaw) {
                pos = pos.yRot(-frameYawDeg * Mth.DEG_TO_RAD);
                look = look.yRot(-frameYawDeg * Mth.DEG_TO_RAD);
            }
            pos = pos.add(origin);
            look = look.add(origin);
        }
        Vec3 d = look.subtract(pos);
        double yaw = Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG - 90;
        double pitch = -Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG;
        return new double[]{pos.x, pos.y, pos.z, yaw, pitch};
    }

    private static double[] sample(double t) {
        int n = KEYS.size();
        if (n == 1 || t <= KEYS.get(0)[0]) {
            return slice(KEYS.get(0));
        }
        if (t >= KEYS.get(n - 1)[0]) {
            return slice(KEYS.get(n - 1));
        }
        int i = 0;
        while (KEYS.get(i + 1)[0] < t) {
            i++;
        }
        double[] p0 = KEYS.get(Math.max(0, i - 1));
        double[] p1 = KEYS.get(i);
        double[] p2 = KEYS.get(i + 1);
        double[] p3 = KEYS.get(Math.min(n - 1, i + 2));
        double u = (t - p1[0]) / (p2[0] - p1[0]);
        double[] out = new double[6];
        for (int c = 0; c < 6; c++) {
            double a = p0[c + 1];
            double b = p1[c + 1];
            double cc = p2[c + 1];
            double d = p3[c + 1];
            out[c] = 0.5 * (2 * b + (-a + cc) * u + (2 * a - 5 * b + 4 * cc - d) * u * u + (-a + 3 * b - 3 * cc + d) * u * u * u);
        }
        return out;
    }

    private static double[] slice(double[] k) {
        return new double[]{k[1], k[2], k[3], k[4], k[5], k[6]};
    }
}

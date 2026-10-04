package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Mth;

/**
 * Melee attacks in the spirit of Better Combat: every weapon type has a three-hit combo, and
 * each hit has a wind-up, a fast strike that eases out, a short follow-through and a slow
 * recovery back to the guard.
 *
 * <p>An attack is described as the weapon arm swinging about the shoulder, so first and third
 * person show the same motion. Channels:
 * <ol start="0">
 *   <li>yaw: degrees, positive sweeps the weapon to the wielder's left</li>
 *   <li>pitch: degrees, positive raises it</li>
 *   <li>roll: degrees, positive tilts the blade's top to the left</li>
 *   <li>push: thrust forward, in blocks (first person) / pixels x 10 (third person)</li>
 *   <li>body: torso twist, radians, positive turns the chest to the left</li>
 *   <li>lean: forward lean, radians</li>
 *   <li>step: lead-leg step forward, 0..1</li>
 *   <li>weight: 0 at rest, 1 while the attack owns the arms</li>
 * </ol>
 */
public final class AttackAnims {
    public static final int CHANNELS = 8;

    public enum Ease {
        SMOOTH, OUT, IN_OUT, LINEAR;

        float apply(float f) {
            return switch (this) {
                case SMOOTH -> f * f * (3 - 2 * f);
                // Half smoothstep, half quadratic ease-out: the cut accelerates out of the wind-up
                // instead of starting at full speed (which reads as a jump), and still lands hard.
                case OUT -> 0.5F * (f * f * (3 - 2 * f)) + 0.5F * (1 - (1 - f) * (1 - f));
                case IN_OUT -> -(Mth.cos(Mth.PI * f) - 1) / 2;
                case LINEAR -> f;
            };
        }
    }

    /** One attack: keys of {time, channels...}, each with the easing used to reach it. */
    public static final class Attack {
        final float[][] keys;
        final Ease[] eases;
        /** The window (in attack progress) where the blade is moving fast: trails are drawn here. */
        public final float strikeFrom;
        public final float strikeTo;

        Attack(float[][] keys, Ease[] eases, float strikeFrom, float strikeTo) {
            this.keys = keys;
            this.eases = eases;
            this.strikeFrom = strikeFrom;
            this.strikeTo = strikeTo;
        }

        public void sample(float t, float[] out) {
            if (t <= keys[0][0]) {
                System.arraycopy(keys[0], 1, out, 0, CHANNELS);
                return;
            }
            for (int i = 0; i < keys.length - 1; i++) {
                float[] a = keys[i];
                float[] b = keys[i + 1];
                if (t <= b[0]) {
                    float span = b[0] - a[0];
                    float f = eases[i + 1].apply(span <= 0 ? 1 : (t - a[0]) / span);
                    for (int c = 0; c < CHANNELS; c++) {
                        out[c] = a[c + 1] + (b[c + 1] - a[c + 1]) * f;
                    }
                    return;
                }
            }
            System.arraycopy(keys[keys.length - 1], 1, out, 0, CHANNELS);
        }
    }

    private static final Map<WeaponType, Attack[]> COMBOS = new EnumMap<>(WeaponType.class);

    /**
     * A swing from one arm pose to another.
     *
     * @param from {yaw, pitch, roll} at the top of the wind-up
     * @param to   {yaw, pitch, roll} where the cut ends
     * @param body torso twist {wind-up, strike}
     */
    private static Attack swing(float tWind, float tStrike, float[] from, float[] to, float push, float[] body, float lean, float step) {
        // The cut itself always gets a good third of the attack: shorter and a fast weapon sweeps
        // 70-90 degrees between two frames, which reads as choppy rather than quick.
        // ...and the recovery back to guard at least a third, or it snaps back just as hard.
        tWind = Math.min(tWind, 0.25F);
        tStrike = Math.min(0.55F, Math.max(tStrike, tWind + 0.28F));
        float tHold = tStrike + 0.08F;
        float[][] keys = {
                {0, 0, 0, 0, 0, 0, 0, 0, 0},
                {tWind, from[0], from[1], from[2], -push * 0.25F, body[0], -lean * 0.3F, step * 0.2F, 1},
                {tStrike, to[0], to[1], to[2], push, body[1], lean, step, 1},
                {tHold, to[0] * 1.06F, to[1] - 4, to[2], push * 0.9F, body[1] * 1.08F, lean, step, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0}};
        Ease[] eases = {Ease.LINEAR, Ease.SMOOTH, Ease.OUT, Ease.SMOOTH, Ease.IN_OUT};
        return new Attack(keys, eases, tWind - 0.02F, tStrike + 0.06F);
    }

    private static float[] p(float yaw, float pitch, float roll) {
        return new float[]{yaw, pitch, roll};
    }

    private static float[] b(float wind, float strike) {
        return new float[]{wind, strike};
    }

    static {
        COMBOS.put(WeaponType.LONGSWORD, new Attack[]{
                swing(0.3F, 0.5F, p(-65, 35, -55), p(75, -25, 30), 0.1F, b(-0.35F, 0.45F), 0.12F, 0.45F),
                swing(0.3F, 0.5F, p(70, 30, 60), p(-70, -20, -35), 0.1F, b(0.4F, -0.4F), 0.12F, 0.45F),
                swing(0.34F, 0.55F, p(5, 95, 0), p(0, -45, 0), 0.25F, b(0, 0), 0.28F, 0.75F)});
        COMBOS.put(WeaponType.KATANA, new Attack[]{
                swing(0.26F, 0.44F, p(-68, 12, -70), p(68, 0, 65), 0.08F, b(-0.4F, 0.5F), 0.1F, 0.5F),
                swing(0.26F, 0.44F, p(68, 22, 70), p(-68, -8, -65), 0.08F, b(0.45F, -0.45F), 0.1F, 0.5F),
                swing(0.3F, 0.5F, p(-40, -35, -30), p(50, 75, 25), 0.15F, b(-0.3F, 0.35F), 0.05F, 0.6F)});
        COMBOS.put(WeaponType.DUAL_DAGGERS, new Attack[]{
                swing(0.22F, 0.4F, p(-10, 6, 0), p(6, 0, 0), 0.45F, b(-0.15F, 0.25F), 0.12F, 0.4F),
                swing(0.22F, 0.4F, p(-50, 18, -40), p(45, -10, 30), 0.15F, b(-0.3F, 0.35F), 0.1F, 0.35F),
                swing(0.25F, 0.45F, p(60, 40, 60), p(-55, -30, -45), 0.2F, b(0.35F, -0.35F), 0.15F, 0.5F)});
        COMBOS.put(WeaponType.GREATSWORD, new Attack[]{
                swing(0.34F, 0.56F, p(-95, 25, -70), p(95, -15, 65), 0.1F, b(-0.55F, 0.6F), 0.12F, 0.55F),
                swing(0.34F, 0.56F, p(90, -15, 60), p(-60, 65, -20), 0.1F, b(0.5F, -0.4F), 0.05F, 0.45F),
                swing(0.38F, 0.6F, p(0, 115, 0), p(0, -55, 0), 0.3F, b(0, 0), 0.38F, 0.9F)});
        COMBOS.put(WeaponType.SPEAR, new Attack[]{
                swing(0.3F, 0.48F, p(-6, 4, 0), p(2, -2, 0), 0.6F, b(-0.25F, 0.2F), 0.18F, 0.7F),
                swing(0.3F, 0.48F, p(-12, 12, 0), p(4, 6, 0), 0.65F, b(-0.3F, 0.25F), 0.15F, 0.75F),
                swing(0.3F, 0.52F, p(-80, 10, -70), p(80, -10, 65), 0.05F, b(-0.45F, 0.5F), 0.08F, 0.5F)});
        COMBOS.put(WeaponType.WARHAMMER, new Attack[]{
                swing(0.4F, 0.6F, p(5, 110, 0), p(0, -60, 0), 0.25F, b(0.1F, -0.05F), 0.35F, 0.8F),
                swing(0.38F, 0.58F, p(-90, 20, -60), p(80, -20, 50), 0.1F, b(-0.55F, 0.55F), 0.12F, 0.55F),
                swing(0.42F, 0.62F, p(0, 125, 0), p(0, -70, 0), 0.3F, b(0, 0), 0.42F, 1.0F)});
        COMBOS.put(WeaponType.SCYTHE, new Attack[]{
                swing(0.34F, 0.56F, p(-100, 6, -80), p(100, -15, -80), 0.05F, b(-0.6F, 0.65F), 0.1F, 0.5F),
                swing(0.34F, 0.56F, p(100, 12, 80), p(-100, -10, 80), 0.05F, b(0.6F, -0.65F), 0.1F, 0.5F),
                swing(0.38F, 0.6F, p(0, 100, 10), p(10, -50, 0), 0.2F, b(0, 0), 0.32F, 0.8F)});
        COMBOS.put(WeaponType.BATTLEAXE, new Attack[]{
                swing(0.34F, 0.54F, p(-60, 70, -40), p(60, -40, 30), 0.12F, b(-0.4F, 0.4F), 0.2F, 0.55F),
                swing(0.34F, 0.54F, p(60, 70, 40), p(-60, -40, -30), 0.12F, b(0.4F, -0.4F), 0.2F, 0.55F),
                swing(0.38F, 0.58F, p(0, 105, 0), p(0, -55, 0), 0.28F, b(0, 0), 0.35F, 0.85F)});
    }

    public static Attack get(WeaponType type, int step) {
        Attack[] combo = COMBOS.get(type);
        return combo[Math.floorMod(step, combo.length)];
    }

    private AttackAnims() {
    }
}

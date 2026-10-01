package com.steelstorm.arsenal.client.anim;

/**
 * A tiny keyframe track: rows of {time, value...} with time running 0..1. Values between keys
 * are eased (smoothstep), so motions accelerate out of one key and settle into the next.
 * {@link Float#NaN} marks a channel the track leaves alone.
 */
public final class Keyframes {
    private final float[][] keys;

    private Keyframes(float[][] keys) {
        this.keys = keys;
    }

    public static Keyframes of(float[]... keys) {
        return new Keyframes(keys);
    }

    public static float[] key(float t, float... values) {
        float[] row = new float[values.length + 1];
        row[0] = t;
        System.arraycopy(values, 0, row, 1, values.length);
        return row;
    }

    /** Samples every channel at time t into {@code out}. */
    public void sample(float t, float[] out) {
        sample(t, out, null);
    }

    /**
     * Like {@link #sample(float, float[])}, but a NaN key blends from or to {@code base} (the
     * pose the part already has), so tracks can start and end on whatever the body is doing.
     */
    public void sample(float t, float[] out, float[] base) {
        if (t <= keys[0][0]) {
            copy(keys[0], out);
            return;
        }
        for (int i = 0; i < keys.length - 1; i++) {
            float[] a = keys[i];
            float[] b = keys[i + 1];
            if (t <= b[0]) {
                float span = b[0] - a[0];
                float f = span <= 0 ? 1 : (t - a[0]) / span;
                f = f * f * (3 - 2 * f);
                for (int c = 0; c < out.length; c++) {
                    float va = a[c + 1];
                    float vb = b[c + 1];
                    if (base != null && !Float.isNaN(base[c])) {
                        va = Float.isNaN(va) ? base[c] : va;
                        vb = Float.isNaN(vb) ? base[c] : vb;
                    }
                    out[c] = Float.isNaN(va) || Float.isNaN(vb) ? Float.NaN : va + (vb - va) * f;
                }
                return;
            }
        }
        copy(keys[keys.length - 1], out);
    }

    private static void copy(float[] row, float[] out) {
        System.arraycopy(row, 1, out, 0, out.length);
    }
}

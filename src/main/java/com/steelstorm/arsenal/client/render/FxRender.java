package com.steelstorm.arsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shapes for the glowing effects: crescents, rings, halos and spheres. Everything here is drawn
 * with additive blending ({@code RenderType.eyes}), so overlapping layers brighten each other and
 * nothing needs sorting. Colours are 0xRRGGBB; `brightness` scales them.
 */
public final class FxRender {
    public static final ResourceLocation SLASH = SteelstormArsenal.id("textures/entity/slash_wave.png");
    public static final ResourceLocation RING = SteelstormArsenal.id("textures/entity/vortex_ring.png");
    public static final ResourceLocation GLOW = SteelstormArsenal.id("textures/entity/glow.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, float u, float v, int color, float brightness) {
        int r = (int) (((color >> 16) & 0xFF) * brightness);
        int g = (int) (((color >> 8) & 0xFF) * brightness);
        int b = (int) ((color & 0xFF) * brightness);
        vc.addVertex(pose, p.x, p.y, p.z).setColor(Math.min(255, r), Math.min(255, g), Math.min(255, b), 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    /** A quad drawn from both sides. Corners go a, b, c, d around the edge. */
    public static void quad(VertexConsumer vc, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                            float u0, float v0, float u1, float v1, int color, float brightness) {
        vertex(vc, pose, a, u0, v0, color, brightness);
        vertex(vc, pose, b, u1, v0, color, brightness);
        vertex(vc, pose, c, u1, v1, color, brightness);
        vertex(vc, pose, d, u0, v1, color, brightness);
        vertex(vc, pose, d, u0, v1, color, brightness);
        vertex(vc, pose, c, u1, v1, color, brightness);
        vertex(vc, pose, b, u1, v0, color, brightness);
        vertex(vc, pose, a, u0, v0, color, brightness);
    }

    private static Vector3f v(Vec3 p) {
        return new Vector3f((float) p.x, (float) p.y, (float) p.z);
    }

    /**
     * A sword-wave crescent centred on the origin, bulging along `forward`. Drawn as a flat band
     * plus a ribbon across it, so it reads from any angle.
     *
     * @param roll 0 = horizontal crescent, 90 = vertical
     */
    public static void crescent(VertexConsumer vc, PoseStack.Pose pose, Vec3 forward, float roll, float halfWidth, int color,
                                float brightness) {
        Vec3 f = forward.normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(f).normalize();
        double r = Math.toRadians(roll);
        Vec3 s = side.scale(Math.cos(r)).add(up.scale(Math.sin(r)));
        Vec3 u = side.scale(-Math.sin(r)).add(up.scale(Math.cos(r)));
        int n = 20;
        double arc = Math.toRadians(78);
        double radius = halfWidth / Math.sin(arc);
        float band = halfWidth * 0.38F;
        float ribbon = halfWidth * 0.24F;
        Vec3[] outer = new Vec3[n + 1];
        Vec3[] inner = new Vec3[n + 1];
        float[] taper = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = -arc + 2 * arc * i / n;
            taper[i] = (float) Math.pow(Math.cos(t / arc * Math.PI / 2), 0.7);
            outer[i] = s.scale(radius * Math.sin(t)).add(f.scale((radius * Math.cos(t) - radius) * 0.55));
            inner[i] = outer[i].subtract(f.scale(band * taper[i]));
        }
        for (int i = 0; i < n; i++) {
            float u0 = i / (float) n;
            float u1 = (i + 1) / (float) n;
            // Flat band: hot leading edge fading toward the back.
            quad(vc, pose, v(outer[i]), v(outer[i + 1]), v(inner[i + 1]), v(inner[i]), u0, 0.5F, u1, 1.0F, color, brightness);
            // White-hot core along the edge.
            Vec3 c0 = outer[i].subtract(f.scale(band * taper[i] * 0.3));
            Vec3 c1 = outer[i + 1].subtract(f.scale(band * taper[i + 1] * 0.3));
            quad(vc, pose, v(outer[i]), v(outer[i + 1]), v(c1), v(c0), u0, 0.5F, u1, 1.0F, 0xFFFFFF, brightness * 0.55F);
            // Ribbon across the band so it is visible edge-on.
            Vec3 m0 = outer[i].subtract(f.scale(band * taper[i] * 0.25));
            Vec3 m1 = outer[i + 1].subtract(f.scale(band * taper[i + 1] * 0.25));
            Vec3 h0 = u.scale(ribbon * taper[i]);
            Vec3 h1 = u.scale(ribbon * taper[i + 1]);
            quad(vc, pose, v(m0.add(h0)), v(m1.add(h1)), v(m1.subtract(h1)), v(m0.subtract(h0)), u0, 0.0F, u1, 1.0F, color, brightness * 0.8F);
        }
    }

    /** A flat ring in the local XZ plane, from `inner` to `outer` radius. `spin` scrolls its texture. */
    public static void ring(VertexConsumer vc, PoseStack.Pose pose, float inner, float outer, int segments, float spin, int color,
                            float brightness) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f o0 = new Vector3f(Mth.cos(a0) * outer, 0, Mth.sin(a0) * outer);
            Vector3f o1 = new Vector3f(Mth.cos(a1) * outer, 0, Mth.sin(a1) * outer);
            Vector3f i1 = new Vector3f(Mth.cos(a1) * inner, 0, Mth.sin(a1) * inner);
            Vector3f i0 = new Vector3f(Mth.cos(a0) * inner, 0, Mth.sin(a0) * inner);
            float u0 = i / (float) segments * 2 + spin;
            float u1 = (i + 1) / (float) segments * 2 + spin;
            quad(vc, pose, o0, o1, i1, i0, u0, 0, u1, 1, color, brightness);
        }
    }

    /** A camera-facing square of soft glow; call after rotating the pose to face the camera. */
    public static void halo(VertexConsumer vc, PoseStack.Pose pose, float size, int color, float brightness) {
        quad(vc, pose, new Vector3f(-size, -size, 0), new Vector3f(size, -size, 0), new Vector3f(size, size, 0), new Vector3f(-size, size, 0),
                0, 0, 1, 1, color, brightness);
    }

    /** A vertical strip of glow from y0 to y1, `width` wide, facing the camera around the Y axis. */
    public static void beam(VertexConsumer vc, PoseStack.Pose pose, float y0, float y1, float width, int color, float brightness) {
        quad(vc, pose, new Vector3f(-width, y0, 0), new Vector3f(width, y0, 0), new Vector3f(width, y1, 0), new Vector3f(-width, y1, 0),
                0, 0, 1, 1, color, brightness);
    }

    /** A sphere for position-only render types (the end portal starfield). */
    public static void sphere(VertexConsumer vc, PoseStack.Pose pose, float radius, int rings, int segments) {
        for (int i = 0; i < rings; i++) {
            float t0 = Mth.PI * i / rings - Mth.HALF_PI;
            float t1 = Mth.PI * (i + 1) / rings - Mth.HALF_PI;
            for (int j = 0; j < segments; j++) {
                float p0 = Mth.TWO_PI * j / segments;
                float p1 = Mth.TWO_PI * (j + 1) / segments;
                // Counter-clockwise seen from outside, so the outer faces survive culling.
                spherePoint(vc, pose, radius, t0, p0);
                spherePoint(vc, pose, radius, t1, p0);
                spherePoint(vc, pose, radius, t1, p1);
                spherePoint(vc, pose, radius, t0, p1);
            }
        }
    }

    private static void spherePoint(VertexConsumer vc, PoseStack.Pose pose, float r, float theta, float phi) {
        float c = Mth.cos(theta);
        vc.addVertex(pose, r * c * Mth.cos(phi), r * Mth.sin(theta), r * c * Mth.sin(phi));
    }

    private FxRender() {
    }
}

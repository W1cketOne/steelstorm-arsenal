package com.steelstorm.arsenal.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A glowing trail that follows the blade itself in first person: each frame of a swing records
 * where the blade's edge is on screen, and the recent positions are joined into a fading ribbon.
 */
public final class BladeTrail {
    private record Sample(Vector3f tip, Vector3f base, long nanos) {
    }

    private static final Deque<Sample> SAMPLES = new ArrayDeque<>();
    private static final long LIFE = 110_000_000L; // 0.11 s

    private BladeTrail() {
    }

    /** Model-space y (pixels) of the blade's base and tip for each weapon type. */
    private static float[] span(WeaponType type) {
        return switch (type) {
            case LONGSWORD -> new float[]{7, 30};
            case GREATSWORD -> new float[]{8, 30};
            case KATANA -> new float[]{6, 30};
            case DUAL_DAGGERS -> new float[]{5, 16};
            case SPEAR -> new float[]{23, 31};
            case WARHAMMER -> new float[]{15, 23};
            case SCYTHE -> new float[]{18, 26};
            case BATTLEAXE -> new float[]{12, 24};
        };
    }

    /**
     * Records the blade position (call with the pose the item is about to be drawn with) and draws
     * the trail. `swinging` is false between swings, when the trail just fades out.
     */
    public static void renderFirstPerson(PoseStack pose, MultiBufferSource buffers, LivingEntity player, ItemStack stack, WeaponType type,
                                         ItemDisplayContext ctx, boolean leftHand, boolean swinging, int color) {
        long now = System.nanoTime();
        if (swinging) {
            Minecraft mc = Minecraft.getInstance();
            BakedModel model = mc.getItemRenderer().getModel(stack, player.level(), player, 0);
            pose.pushPose();
            model.applyTransform(ctx, pose, leftHand);
            pose.translate(-0.5F, -0.5F, -0.5F);
            Matrix4f m = pose.last().pose();
            float[] span = span(type);
            Vector3f tip = m.transformPosition(new Vector3f(0.5F, span[1] / 16F, 0.5F));
            // Only the outer third of the blade leaves a trail, so it reads as a streak, not a sheet.
            float inner = span[0] + (span[1] - span[0]) * 0.65F;
            Vector3f base = m.transformPosition(new Vector3f(0.5F, inner / 16F, 0.5F));
            pose.popPose();
            SAMPLES.addLast(new Sample(tip, base, now));
        }
        while (!SAMPLES.isEmpty() && now - SAMPLES.peekFirst().nanos() > LIFE) {
            SAMPLES.removeFirst();
        }
        if (SAMPLES.size() < 2) {
            return;
        }
        float r = (color >> 16 & 255) / 255F;
        float g = (color >> 8 & 255) / 255F;
        float b = (color & 255) / 255F;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f id = new Matrix4f();
        Sample prev = null;
        for (Sample s : SAMPLES) {
            if (prev != null) {
                float a0 = 1 - (now - prev.nanos()) / (float) LIFE;
                float a1 = 1 - (now - s.nanos()) / (float) LIFE;
                a0 = Math.max(0, a0) * a0 * 0.45F;
                a1 = Math.max(0, a1) * a1 * 0.45F;
                quad(vc, id, prev.base(), prev.tip(), s.tip(), s.base(), r, g, b, a0, a1);
            }
            prev = s;
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float r, float g, float bl,
                             float alphaA, float alphaC) {
        float wr = Math.min(1, r + 0.15F);
        float wg = Math.min(1, g + 0.15F);
        float wb = Math.min(1, bl + 0.15F);
        vc.addVertex(m, a.x, a.y, a.z).setColor(r, g, bl, alphaA * 0.2F);
        vc.addVertex(m, b.x, b.y, b.z).setColor(wr, wg, wb, alphaA);
        vc.addVertex(m, c.x, c.y, c.z).setColor(wr, wg, wb, alphaC);
        vc.addVertex(m, d.x, d.y, d.z).setColor(r, g, bl, alphaC * 0.2F);
        vc.addVertex(m, d.x, d.y, d.z).setColor(r, g, bl, alphaC * 0.2F);
        vc.addVertex(m, c.x, c.y, c.z).setColor(wr, wg, wb, alphaC);
        vc.addVertex(m, b.x, b.y, b.z).setColor(wr, wg, wb, alphaA);
        vc.addVertex(m, a.x, a.y, a.z).setColor(r, g, bl, alphaA * 0.2F);
    }
}

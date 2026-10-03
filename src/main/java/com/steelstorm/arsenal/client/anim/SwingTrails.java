package com.steelstorm.arsenal.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.client.ClientCombatState;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * Glowing ribbons that follow the blade through every swing, coloured by the weapon. The arc is
 * worked out from the swing's progress and direction, so it needs no stored history. Also the
 * small FOV punch when a hit lands.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class SwingTrails {
    private static final int SEGMENTS = 14;

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        boolean any = false;
        for (Player p : mc.level.players()) {
            ItemStack stack = p.getMainHandItem();
            if (!(stack.getItem() instanceof WeaponItem weapon) || p.isInvisible()) {
                continue;
            }
            float progress = SwingClock.progress(p, p.getAttackAnim(partial), weapon.type(), partial);
            if (progress <= 0.12F) {
                continue;
            }
            boolean firstPerson = p == mc.player && camera.getEntity() == p && !camera.isDetached();
            any |= draw(vc, pose, cam, p, weapon.type(), stack, progress, partial, firstPerson);
        }
        if (any) {
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static boolean draw(VertexConsumer vc, PoseStack pose, Vec3 cam, Player p, WeaponType type, ItemStack stack,
                                float progress, float partial, boolean firstPerson) {
        // The cut sweeps across 0.15..0.6 of the swing; the ribbon trails the tip and fades afterwards.
        float s = Mth.clamp((progress - 0.15F) / 0.45F, 0, 1);
        float fade = progress < 0.6F ? 1 : Mth.clamp(1 - (progress - 0.6F) / 0.3F, 0, 1);
        if (fade <= 0 || s <= 0) {
            return false;
        }
        boolean finisher = SwingClock.finisher(p);
        int cut = SwingClock.cut(p);
        float yaw = p.getViewYRot(partial) * Mth.DEG_TO_RAD;
        float pitch = Mth.clamp(p.getViewXRot(partial), -40, 40) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 right = fwd.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(fwd).normalize();
        boolean two = WeaponAnimator.isTwoHanded(type);
        float radius = two ? 1.9F : 1.45F;
        float inner = radius * 0.45F;
        Vec3 center = p.getEyePosition(partial).add(0, -0.35, 0).add(fwd.scale(firstPerson ? 0.9 : 0.45));
        // Start and end angles of the arc in the right/up plane (or the up/forward plane for a finisher).
        float a0;
        float a1;
        if (finisher) {
            a0 = 110;
            a1 = -70;
        } else if (cut == 0) {
            a0 = 150;
            a1 = -25;
        } else {
            a0 = 30;
            a1 = 205;
        }
        float head = s;
        float tail = Math.max(0, s - 0.55F);
        int color = WeaponLooks.trailColor(stack);
        float r = (color >> 16 & 255) / 255F;
        float g = (color >> 8 & 255) / 255F;
        float b = (color & 255) / 255F;
        Matrix4f m = pose.last().pose();
        Vec3 prevIn = null;
        Vec3 prevOut = null;
        float prevA = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = Mth.lerp(i / (float) SEGMENTS, tail, head);
            float ang = Mth.lerp(t, a0, a1) * Mth.DEG_TO_RAD;
            Vec3 dir;
            if (finisher) {
                dir = up.scale(Mth.sin(ang)).add(fwd.scale(Mth.cos(ang)));
            } else {
                // Tilt the plane a little so cuts read as diagonal slashes.
                Vec3 tiltUp = up.add(fwd.scale(cut == 0 ? 0.25 : -0.25)).normalize();
                dir = right.scale(Mth.cos(ang)).add(tiltUp.scale(Mth.sin(ang) * 0.75));
            }
            Vec3 in = center.add(dir.scale(inner)).subtract(cam);
            Vec3 out = center.add(dir.scale(radius)).subtract(cam);
            float alpha = fade * (i / (float) SEGMENTS) * 0.85F;
            if (prevIn != null) {
                quad(vc, m, prevIn, prevOut, out, in, r, g, b, prevA, alpha);
            }
            prevIn = in;
            prevOut = out;
            prevA = alpha;
        }
        return true;
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl,
                             float alphaA, float alphaC) {
        // Outer edge bright and white-hot, inner edge fading into the weapon's colour.
        vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alphaA * 0.15F);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(Math.min(1, r + 0.5F), Math.min(1, g + 0.5F), Math.min(1, bl + 0.5F), alphaA);
        vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(Math.min(1, r + 0.5F), Math.min(1, g + 0.5F), Math.min(1, bl + 0.5F), alphaC);
        vc.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, alphaC * 0.15F);
        // Back face so the ribbon shows from both sides.
        vc.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, alphaC * 0.15F);
        vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(Math.min(1, r + 0.5F), Math.min(1, g + 0.5F), Math.min(1, bl + 0.5F), alphaC);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(Math.min(1, r + 0.5F), Math.min(1, g + 0.5F), Math.min(1, bl + 0.5F), alphaA);
        vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alphaA * 0.15F);
    }

    /** A quick zoom-in punch when a hit lands (bigger on every third). */
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        float punch = ClientCombatState.hitPunch;
        if (punch > 0 && event.usedConfiguredFov()) {
            event.setFOV(event.getFOV() * (1 - 0.035F * punch));
        }
    }

    private SwingTrails() {
    }
}

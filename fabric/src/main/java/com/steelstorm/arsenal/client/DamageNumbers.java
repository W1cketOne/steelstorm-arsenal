package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.network.DamageNumberPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.ClientTickEvent;
import com.steelstorm.compat.neo.neoforge.client.event.RenderLevelStageEvent;

/** Damage numbers that pop out of enemies, arc upward and fade. Crits are big and gold. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class DamageNumbers {
    private static final int LIFE = 26;

    private static final class Num {
        Vec3 pos;
        Vec3 vel;
        final String text;
        final int color;
        final float size;
        int age;

        Num(Vec3 pos, Vec3 vel, String text, int color, float size) {
            this.pos = pos;
            this.vel = vel;
            this.text = text;
            this.color = color;
            this.size = size;
        }
    }

    private static final List<Num> NUMS = new ArrayList<>();

    public static void add(DamageNumberPayload p) {
        if (!Config.SHOW_DAMAGE_NUMBERS.get() || NUMS.size() > 60) {
            return;
        }
        var rng = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.random;
        double dx = rng == null ? 0 : (rng.nextDouble() - 0.5) * 0.08;
        double dz = rng == null ? 0 : (rng.nextDouble() - 0.5) * 0.08;
        float v = p.amount();
        String text = v >= 10 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
        int color = switch (p.kind()) {
            case 1 -> 0xFFC233;
            case 2 -> 0x7FD8FF;
            default -> 0xFFFFFF;
        };
        float size = p.kind() == 1 ? 1.5F : 1.0F + Math.min(0.6F, v / 30F);
        if (p.kind() == 1) {
            text = text + "!";
        }
        NUMS.add(new Num(new Vec3(p.x(), p.y(), p.z()), new Vec3(dx, 0.14, dz), text, color, size));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        NUMS.removeIf(n -> ++n.age > LIFE);
        for (Num n : NUMS) {
            n.pos = n.pos.add(n.vel);
            n.vel = new Vec3(n.vel.x * 0.9, n.vel.y * 0.82 - 0.004, n.vel.z * 0.9);
        }
        if (Minecraft.getInstance().level == null) {
            NUMS.clear();
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || NUMS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Num n : NUMS) {
            float age = n.age + partial;
            Vec3 at = n.pos.add(n.vel.scale(partial)).subtract(cam);
            float pop = age < 3 ? 1.0F + (3 - age) * 0.25F : 1.0F;
            float alpha = age > LIFE - 8 ? Mth.clamp((LIFE - age) / 8F, 0, 1) : 1;
            int a = Math.max(8, (int) (alpha * 255));
            float s = 0.045F * n.size * pop;
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(camera.rotation());
            pose.scale(s, -s, s);
            float w = -font.width(n.text) / 2.0F;
            font.drawInBatch(n.text, w + 1, 1, (a << 24) | 0x202020, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0,
                    0xF000F0);
            pose.translate(0, 0, -0.01F);
            font.drawInBatch(n.text, w, 0, (a << 24) | n.color, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0,
                    0xF000F0);
            pose.popPose();
        }
        buffers.endBatch();
    }

    private DamageNumbers() {
    }
}

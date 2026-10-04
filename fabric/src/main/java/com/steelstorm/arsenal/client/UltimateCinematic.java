package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import com.steelstorm.compat.neo.neoforge.client.event.ClientTickEvent;
import com.steelstorm.compat.neo.neoforge.client.event.ViewportEvent;

/**
 * When you unleash an ultimate the camera swings out behind you, pulls back and slowly circles
 * while it lands, then returns to where it was.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class UltimateCinematic {
    private static final int LENGTH = 40;
    private static int age = -1;
    private static CameraType previous;
    private static float side = 1;

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.ULTIMATE_CINEMATIC.get() || mc.player == null || age >= 0) {
            return;
        }
        previous = mc.options.getCameraType();
        if (previous.isFirstPerson()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        side = mc.player.getRandom().nextBoolean() ? 1 : -1;
        age = 0;
    }

    private static float progress(float partial) {
        return age < 0 ? -1 : Mth.clamp((age + partial) / LENGTH, 0, 1);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (age < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (++age >= LENGTH || mc.player == null) {
            if (previous != null && mc.options.getCameraType() != previous) {
                mc.options.setCameraType(previous);
            }
            age = -1;
            previous = null;
        }
    }

    @SubscribeEvent
    public static void onDistance(CalculateDetachedCameraDistanceEvent event) {
        float t = progress((float) Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (t >= 0) {
            float out = smooth(t / 0.25F) * (1 - smooth((t - 0.7F) / 0.3F));
            event.setDistance(event.getDistance() + 3.0F * out);
        }
    }

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        float t = progress((float) event.getPartialTick());
        if (t >= 0 && !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            // A gentle tilt down onto the action; no orbiting, so the view never swings about.
            float lift = smooth(t / 0.25F) * (1 - smooth((t - 0.7F) / 0.3F));
            event.setPitch(event.getPitch() + 8 * lift);
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private UltimateCinematic() {
    }
}

package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.network.KillStreakPayload;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.Mastery;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Kill-streak banners (DOUBLE KILL...) and mastery rank-ups, popping in near the top of the screen. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class KillBanner {
    private static String text = "";
    private static String sub = "";
    private static int color = 0xFFFFFF;
    private static int age = 1000;
    private static String mastery = "";
    private static int masteryAge = 1000;

    public static void show(KillStreakPayload p) {
        Minecraft mc = Minecraft.getInstance();
        int n = p.count();
        if (n < 0) {
            int rank = -n;
            mastery = "\u2605 MASTERY: " + Mastery.NAMES[rank].toUpperCase() + " \u2605  (+" + (3 * rank) + "% damage)";
            masteryAge = 0;
            return;
        } else {
            text = switch (n) {
                case 2 -> "DOUBLE KILL";
                case 3 -> "TRIPLE KILL";
                case 4 -> "QUADRA KILL";
                case 5 -> "RAMPAGE";
                default -> "UNSTOPPABLE";
            };
            sub = n >= 6 ? n + " kills!" : "";
            color = switch (n) {
                case 2 -> 0xFFFFFF;
                case 3 -> 0x7FD8FF;
                case 4 -> 0xB15CFF;
                case 5 -> 0xFF7A1A;
                default -> 0xFF2E4A;
            };
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ULTIMATE_READY.get(), 1.0F + Math.min(5, n) * 0.08F, 0.7F));
        }
        age = 0;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        age++;
        masteryAge++;
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.TITLE, SteelstormArsenal.id("kill_banner"), KillBanner::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        float mt = masteryAge + delta.getGameTimeDeltaPartialTick(false);
        if (mt < 70 && !mastery.isEmpty()) {
            float ma = mt > 55 ? Mth.clamp((70 - mt) / 15F, 0, 1) : 1;
            float mpop = mt < 4 ? 1.8F - mt * 0.2F : 1.0F;
            g.pose().pushPose();
            g.pose().translate(g.guiWidth() / 2.0F, g.guiHeight() * 0.24F + 30, 300);
            g.pose().scale(1.4F * mpop, 1.4F * mpop, 1);
            g.drawString(mc.font, mastery, -mc.font.width(mastery) / 2, 0, (Math.max(8, (int) (ma * 255)) << 24) | 0xFFC233, true);
            g.pose().popPose();
        }
        float t = age + delta.getGameTimeDeltaPartialTick(false);
        if (t > 50 || text.isEmpty()) {
            return;
        }
        float pop = t < 4 ? 2.2F - t * 0.3F : 1.0F;
        float alpha = t > 38 ? Mth.clamp((50 - t) / 12F, 0, 1) : 1;
        int a = Math.max(8, (int) (alpha * 255));
        float scale = 2.2F * pop;
        g.pose().pushPose();
        g.pose().translate(g.guiWidth() / 2.0F, g.guiHeight() * 0.24F, 300);
        g.pose().scale(scale, scale, 1);
        int w = mc.font.width(text);
        g.drawString(mc.font, text, -w / 2, -4, (a << 24) | color, true);
        g.pose().popPose();
        if (!sub.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(g.guiWidth() / 2.0F, g.guiHeight() * 0.24F + 16, 300);
            g.drawString(mc.font, sub, -mc.font.width(sub) / 2, 0, (a << 24) | 0xE8E8E8, true);
            g.pose().popPose();
        }
    }

    private KillBanner() {
    }
}

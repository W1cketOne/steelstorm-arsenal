package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Abilities;
import com.steelstorm.arsenal.ability.Ability;
import com.steelstorm.arsenal.ability.AbilitySet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.RegisterGuiLayersEvent;
import com.steelstorm.compat.neo.neoforge.client.gui.VanillaGuiLayers;

/**
 * The combat HUD: stamina bar above the hunger bar, the held weapon's abilities and ultimate to
 * the right of the hotbar, and the combo counter by the crosshair.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class CombatHud {
    private static final ResourceLocation HUD = SteelstormArsenal.id("textures/gui/hud.png");
    private static final int BAR_WIDTH = 83;

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, SteelstormArsenal.id("stamina"), CombatHud::renderStamina);
        event.registerAbove(VanillaGuiLayers.HOTBAR, SteelstormArsenal.id("abilities"), CombatHud::renderAbilities);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, SteelstormArsenal.id("combo"), CombatHud::renderCombo);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SteelstormArsenal.id("flash"), CombatHud::renderFlash);
    }

    private static boolean hidden(Minecraft mc) {
        return mc.options.hideGui || mc.player == null || mc.player.isSpectator();
    }

    private static void renderStamina(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden(mc) || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        int right = g.guiWidth() / 2 + 91;
        int left = right - BAR_WIDTH;
        // Above the hunger row, and above the air bubbles when they show.
        int rightHeight = 49;
        if (mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) || mc.player.getAirSupply() < mc.player.getMaxAirSupply()) {
            rightHeight += 10;
        }
        int y = g.guiHeight() - rightHeight + 2;

        float fraction = Mth.clamp(ClientCombatState.stamina / ClientCombatState.maxStamina, 0, 1);
        int filled = Math.round(81 * fraction);
        boolean flash = ClientCombatState.lowStaminaFlash > 0 && (ClientCombatState.lowStaminaFlash / 3) % 2 == 0;
        g.blit(HUD, left, y, 0, 32, BAR_WIDTH, 7, 128, 64);
        if (flash) {
            g.fill(left + 1, y + 1, right - 1, y + 6, 0xAA9A1A1A);
        }
        // Fill from the right, like the hunger bar it sits on.
        int v = fraction > 0.3F ? 40 : 46;
        if (filled > 0) {
            g.blit(HUD, right - 1 - filled, y + 1, 81 - filled, v, filled, 5, 128, 64);
        }
        for (int i = 1; i < 4; i++) {
            int x = left + 1 + 81 * i / 4;
            g.fill(x, y + 1, x + 1, y + 6, 0x40000000);
        }
    }

    private static void renderAbilities(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden(mc)) {
            return;
        }
        AbilitySet set = Abilities.forStack(mc.player.getMainHandItem());
        if (set == null) {
            return;
        }
        boolean synced = set.id().equals(ClientCombatState.setId);
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int x0 = g.guiWidth() / 2 + 98;
        int y = g.guiHeight() - 22;
        Font font = mc.font;
        float costMultiplier = Config.SPECIAL_COST_MULTIPLIER.get().floatValue();
        boolean creative = mc.player.getAbilities().instabuild;
        for (int slot = 0; slot < 3; slot++) {
            Ability ability = set.get(slot);
            int x = x0 + slot * 23;
            int press = ClientCombatState.pressAge[slot];
            int dy = press < 3 ? 1 : 0;
            g.blit(HUD, x, y + dy, 0, 0, 22, 22, 128, 64);
            g.blit(ability.icon(), x + 3, y + 3 + dy, 0, 0, 16, 16, 16, 16);
            int left = synced ? ClientCombatState.cooldownLeft[slot] : 0;
            int charges = ability.charges() > 1 ? (synced ? ClientCombatState.charges[slot] : ability.charges()) : 1;
            if (ability.charges() > 1 && charges > 0 && left > 0) {
                // Still usable: only a thin bar shows the next charge coming back.
                float frac = Mth.clamp(1 - (left - partial) / (float) Math.max(1, ClientCombatState.cooldownTotal[slot]), 0, 1);
                g.fill(x + 3, y + 17 + dy, x + 19, y + 19 + dy, 0xC0101010);
                g.fill(x + 3, y + 17 + dy, x + 3 + Math.round(16 * frac), y + 19 + dy, 0xFF7FD8FF);
            } else if (left > 0) {
                cooldownOverlay(g, font, x + 3, y + 3 + dy, 16, left, ClientCombatState.cooldownTotal[slot], partial);
            } else {
                if (!creative && ClientCombatState.stamina < ability.staminaCost() * costMultiplier) {
                    g.fill(x + 3, y + 3 + dy, x + 19, y + 19 + dy, 0x88601010);
                }
                readyFlash(g, x + 3, y + 3 + dy, 16, ClientCombatState.readyAge[slot]);
            }
            keyLabel(g, font, ModKeyMappings.ABILITIES[slot].getTranslatedKeyMessage().getString(), x + 21, y + 14 + dy);
            if (ability.charges() > 1) {
                // One pip per charge across the top of the frame.
                for (int k = 0; k < ability.charges(); k++) {
                    int px = x + 4 + k * 5;
                    g.fill(px, y - 3 + dy, px + 4, y - 1 + dy, k < charges ? 0xFF7FD8FF : 0xFF303844);
                }
            }
        }
        renderUltimate(g, font, set.get(3), x0 + 69, y - 4, synced, partial);
    }

    private static void renderUltimate(GuiGraphics g, Font font, Ability ability, int x, int y, boolean synced, float partial) {
        float charge = Mth.clamp(ClientCombatState.ultimate / 100.0F, 0, 1);
        boolean creative = Minecraft.getInstance().player.getAbilities().instabuild;
        int left = synced ? ClientCombatState.cooldownLeft[3] : 0;
        boolean ready = (charge >= 1 || creative) && left <= 0;
        int press = ClientCombatState.pressAge[3];
        int dy = press < 3 ? 1 : 0;
        if (ready) {
            // Pulsing golden glow around the frame.
            float time = Minecraft.getInstance().player.tickCount + partial;
            int alpha = (int) (90 + 70 * Mth.sin(time * 0.25F));
            g.fill(x - 2, y - 2 + dy, x + 28, y + 28 + dy, (alpha << 24) | 0xFFC233);
        }
        g.blit(HUD, x, y + dy, 24, 0, 26, 26, 128, 64);
        g.blit(ability.icon(), x + 5, y + 5 + dy, 0, 0, 16, 16, 16, 16);
        if (left > 0) {
            cooldownOverlay(g, font, x + 5, y + 5 + dy, 16, left, ClientCombatState.cooldownTotal[3], partial);
        } else if (!ready) {
            // The icon fills up from the bottom as the meter charges.
            int empty = Math.round(16 * (1 - charge));
            g.fill(x + 5, y + 5 + dy, x + 21, y + 5 + dy + empty, 0xB0101010);
            g.fill(x + 5, y + 5 + dy + empty, x + 21, y + 6 + dy + empty, 0xFFFFD166);
            String pct = (int) (charge * 100) + "%";
            g.pose().pushPose();
            g.pose().translate(x + 13, y + 10 + dy, 200);
            g.pose().scale(0.5F, 0.5F, 1);
            g.drawString(font, pct, -font.width(pct) / 2, 0, 0xFFFFE9A8, true);
            g.pose().popPose();
        } else if (ClientCombatState.ultimateReadyAge < 12) {
            readyFlash(g, x + 5, y + 5 + dy, 16, ClientCombatState.ultimateReadyAge / 2);
        }
        keyLabel(g, font, ModKeyMappings.ULTIMATE.getTranslatedKeyMessage().getString(), x + 25, y + 18 + dy);
        if (ClientCombatState.ultimateCharge >= 0) {
            float frac = Mth.clamp((ClientCombatState.ultimateCharge + partial) / com.steelstorm.arsenal.ability.AbilityManager.FULL_CHARGE, 0, 1);
            int barY = y - 8 + dy;
            g.fill(x - 1, barY - 1, x + 27, barY + 4, 0xE0101010);
            int color = frac >= 1 ? ((Minecraft.getInstance().player.tickCount / 2) % 2 == 0 ? 0xFFFFFFFF : 0xFFFFC233) : 0xFFFFC233;
            g.fill(x, barY, x + Math.round(26 * frac), barY + 3, color);
            String text = String.format(java.util.Locale.ROOT, "x%.2f", 1 + com.steelstorm.arsenal.ability.AbilityManager.MAX_POWER_BONUS * frac);
            g.pose().pushPose();
            g.pose().translate(x + 13, barY - 7, 220);
            g.pose().scale(0.6F, 0.6F, 1);
            g.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFE9A8, true);
            g.pose().popPose();
        }
    }

    private static void cooldownOverlay(GuiGraphics g, Font font, int x, int y, int size, int left, int total, float partial) {
        float frac = Mth.clamp((left - partial) / (float) Math.max(1, total), 0, 1);
        int h = Math.round(size * frac);
        g.fill(x, y + size - h, x + size, y + size, 0xB0101010);
        g.fill(x, y, x + size, y + size - h, 0x30000000);
        String secs = left >= 100 ? String.valueOf((left + 19) / 20) : String.format(java.util.Locale.ROOT, "%.1f", left / 20.0F);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.drawString(font, secs, x + size / 2 - font.width(secs) / 2 + 1, y + size / 2 - 4, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    private static void readyFlash(GuiGraphics g, int x, int y, int size, int age) {
        if (age < 8) {
            int alpha = (int) (200 * (1 - age / 8.0F));
            g.fill(x, y, x + size, y + size, (alpha << 24) | 0xFFFFFF);
        }
    }

    private static void keyLabel(GuiGraphics g, Font font, String label, int rightX, int y) {
        if (label.length() > 3) {
            label = label.substring(0, 3);
        }
        g.pose().pushPose();
        g.pose().translate(rightX, y, 210);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, label, -font.width(label), 0, 0xFFE8F4FF, true);
        g.pose().popPose();
    }

    private static void renderCombo(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden(mc) || !Config.SHOW_COMBO_COUNTER.get() || ClientCombatState.combo < 2) {
            return;
        }
        int age = ClientCombatState.comboAge;
        if (age > 60) {
            return;
        }
        float alpha = age < 40 ? 1.0F : 1.0F - (age - 40) / 20.0F;
        float pop = age < 4 ? 1.0F + (4 - age) * 0.12F : 1.0F;
        int color = ((int) (alpha * 255) << 24) | (ClientCombatState.combo % 3 == 0 ? 0xFFD24A : 0xFFFFFF);
        String text = "x" + ClientCombatState.combo;
        g.pose().pushPose();
        g.pose().translate(g.guiWidth() / 2.0F + 10, g.guiHeight() / 2.0F + 4, 0);
        g.pose().scale(pop, pop, 1);
        g.drawString(mc.font, text, 0, 0, color, true);
        g.pose().popPose();
    }

    /** A white flash with a golden vignette when an ultimate goes off. */
    private static void renderFlash(GuiGraphics g, DeltaTracker delta) {
        float f = ClientCombatState.flash;
        if (f <= 0.01F) {
            return;
        }
        int a = (int) (Math.min(1, f) * 120);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0xFFF6E0);
        int edge = (int) (Math.min(1, f) * 160);
        int w = g.guiWidth(), h = g.guiHeight(), b = Math.max(8, h / 10);
        g.fillGradient(0, 0, w, b, (edge << 24) | 0xFFB800, 0x00FFB800);
        g.fillGradient(0, h - b, w, h, 0x00FFB800, (edge << 24) | 0xFFB800);
    }

    private CombatHud() {
    }
}

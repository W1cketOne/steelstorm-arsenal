package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Stamina bar above the hunger bar, special cooldown next to the hotbar, combo counter by the crosshair. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class CombatHud {
    private static final int BAR_WIDTH = 81;

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, SteelstormArsenal.id("stamina"), CombatHud::renderStamina);
        event.registerAbove(VanillaGuiLayers.HOTBAR, SteelstormArsenal.id("special_cooldown"), CombatHud::renderSpecial);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, SteelstormArsenal.id("combo"), CombatHud::renderCombo);
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
        int y = g.guiHeight() - mc.gui.rightHeight + 3;
        mc.gui.rightHeight += 7;

        float fraction = Mth.clamp(ClientCombatState.stamina / ClientCombatState.maxStamina, 0, 1);
        int filled = Math.round((BAR_WIDTH - 2) * fraction);
        boolean flash = ClientCombatState.lowStaminaFlash > 0 && (ClientCombatState.lowStaminaFlash / 3) % 2 == 0;
        g.fill(left, y, right, y + 5, 0xFF101418);
        g.fill(left + 1, y + 1, right - 1, y + 4, flash ? 0xFF7A1A1A : 0xFF2A323A);
        int top = fraction > 0.3F ? 0xFF7FD0FF : 0xFFFFC060;
        int bottom = fraction > 0.3F ? 0xFF2F7FD8 : 0xFFD0701A;
        // Fill from the right, like the hunger bar it sits on.
        g.fillGradient(right - 1 - filled, y + 1, right - 1, y + 4, top, bottom);
        // Tick marks every 25%.
        for (int i = 1; i < 4; i++) {
            int x = left + 1 + (BAR_WIDTH - 2) * i / 4;
            g.fill(x, y + 1, x + 1, y + 4, 0x55000000);
        }
    }

    private static void renderSpecial(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof WeaponItem)) {
            return;
        }
        int x = g.guiWidth() / 2 + 91 + 28;
        int y = g.guiHeight() - 21;
        g.fill(x - 2, y - 2, x + 18, y + 18, 0xAA000000);
        g.renderItem(held, x, y);
        Font font = mc.font;
        int left = ClientCombatState.specialCooldownLeft;
        if (left > 0) {
            float frac = left / (float) ClientCombatState.specialCooldownTotal;
            int h = Math.round(16 * Mth.clamp(frac, 0, 1));
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.fill(x, y + 16 - h, x + 16, y + 16, 0xB0101010);
            String secs = String.valueOf((left + 19) / 20);
            g.drawString(font, secs, x + 9 - font.width(secs) / 2, y + 4, 0xFFFFFFFF, true);
            g.pose().popPose();
        } else {
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            Component key = ModKeyMappings.SPECIAL.getTranslatedKeyMessage();
            String label = key.getString();
            if (label.length() > 3) {
                label = label.substring(0, 3);
            }
            g.drawString(font, label, x + 17 - font.width(label), y + 9, 0xFF7FFFD4, true);
            g.pose().popPose();
        }
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

    private CombatHud() {
    }
}

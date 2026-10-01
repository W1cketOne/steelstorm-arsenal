package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.network.DodgePayload;
import com.steelstorm.arsenal.network.SpecialPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ClientCombatState.tick();
        // Keys only send requests; the server checks stamina and cooldowns.
        while (ModKeyMappings.DODGE.consumeClick()) {
            if (mc.screen == null && !player.isSpectator()) {
                PacketDistributor.sendToServer(new DodgePayload(player.input.forwardImpulse, player.input.leftImpulse));
            }
        }
        while (ModKeyMappings.SPECIAL.consumeClick()) {
            if (mc.screen == null && !player.isSpectator()) {
                PacketDistributor.sendToServer(SpecialPayload.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCombatState.reset();
    }

    /** Small, decaying camera shake on heavy hits. Can be turned off in the client config. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ClientCombatState.shakeTicks <= 0 || !Config.SCREEN_SHAKE.get()) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float progress = (ClientCombatState.shakeTicks - partial) / ClientCombatState.shakeTotal;
        float amount = ClientCombatState.shakeStrength * Math.max(0, progress) * Config.SCREEN_SHAKE_STRENGTH.get().floatValue();
        float time = (Minecraft.getInstance().player.tickCount + partial) * 2.7F;
        event.setYaw(event.getYaw() + Mth.sin(time) * amount * 0.9F);
        event.setPitch(event.getPitch() + Mth.cos(time * 1.3F) * amount * 0.7F);
        event.setRoll(event.getRoll() + Mth.sin(time * 0.7F) * amount * 1.2F);
    }

    private ClientEvents() {
    }
}

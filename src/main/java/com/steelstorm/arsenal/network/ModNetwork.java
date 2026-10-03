package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.AbilityManager;
import com.steelstorm.arsenal.client.ClientPayloadHandler;
import com.steelstorm.arsenal.combat.DodgeHandler;
import com.steelstorm.arsenal.combat.SwingEffects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("5");
        // Keybinds only *request* actions; the server validates stamina, cooldowns and charge.
        registrar.playToServer(DodgePayload.TYPE, DodgePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                DodgeHandler.tryDodge(player, payload.forward(), payload.strafe());
            }
        });
        registrar.playToServer(AbilityPayload.TYPE, AbilityPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                switch (payload.phase()) {
                    case AbilityPayload.CHARGE_START -> AbilityManager.startCharge(player);
                    case AbilityPayload.CHARGE_RELEASE -> AbilityManager.releaseCharge(player);
                    default -> AbilityManager.tryActivate(player, payload.slot());
                }
            }
        });
        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                com.steelstorm.arsenal.combat.ArmorAbilities.doubleJump(player);
            }
        });
        registrar.playToServer(SwingPayload.TYPE, SwingPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SwingEffects.onSwing(player, payload.heavy(), payload.roll());
            }
        });
        // Client handlers live in the client package and are only invoked on the client.
        registrar.playToClient(CombatSyncPayload.TYPE, CombatSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleSync(payload));
        registrar.playToClient(FeedbackPayload.TYPE, FeedbackPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleFeedback(payload));
        registrar.playToClient(PlayerAnimPayload.TYPE, PlayerAnimPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleAnim(payload));
    }

    private ModNetwork() {
    }
}

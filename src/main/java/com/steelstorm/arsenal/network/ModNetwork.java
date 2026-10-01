package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.client.ClientPayloadHandler;
import com.steelstorm.arsenal.combat.DodgeHandler;
import com.steelstorm.arsenal.combat.SpecialAbilities;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Keybinds only *request* actions; the server validates stamina and cooldowns.
        registrar.playToServer(DodgePayload.TYPE, DodgePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                DodgeHandler.tryDodge(player, payload.forward(), payload.strafe());
            }
        });
        registrar.playToServer(SpecialPayload.TYPE, SpecialPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SpecialAbilities.tryActivate(player);
            }
        });
        // Client handlers live in the client package and are only invoked on the client.
        registrar.playToClient(CombatSyncPayload.TYPE, CombatSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleSync(payload));
        registrar.playToClient(FeedbackPayload.TYPE, FeedbackPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleFeedback(payload));
    }

    private ModNetwork() {
    }
}

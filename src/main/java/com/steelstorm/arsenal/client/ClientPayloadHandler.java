package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.anim.CastPose;
import com.steelstorm.arsenal.client.anim.ClientAnims;
import com.steelstorm.arsenal.network.CombatSyncPayload;
import com.steelstorm.arsenal.network.FeedbackPayload;
import com.steelstorm.arsenal.network.PlayerAnimPayload;

/** Handles server-to-client packets. Only ever called on the client. */
public final class ClientPayloadHandler {
    public static void handleSync(CombatSyncPayload payload) {
        ClientCombatState.apply(payload);
    }

    public static void handleFeedback(FeedbackPayload payload) {
        ClientCombatState.shake(payload.shake(), payload.ticks());
    }

    public static void handleAnim(PlayerAnimPayload payload) {
        ClientAnims.start(payload.entityId(), CastPose.byId(payload.pose()), payload.duration());
    }

    private ClientPayloadHandler() {
    }
}

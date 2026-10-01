package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.network.CombatSyncPayload;
import com.steelstorm.arsenal.network.FeedbackPayload;

/** Handles server-to-client packets. Only ever called on the client. */
public final class ClientPayloadHandler {
    public static void handleSync(CombatSyncPayload payload) {
        ClientCombatState.apply(payload);
    }

    public static void handleFeedback(FeedbackPayload payload) {
        ClientCombatState.shake(payload.shake(), payload.ticks());
    }

    private ClientPayloadHandler() {
    }
}

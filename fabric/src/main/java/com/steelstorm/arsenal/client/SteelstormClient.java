package com.steelstorm.arsenal.client;

import com.steelstorm.compat.client.ClientHooks;
import net.fabricmc.api.ClientModInitializer;

/** Client-only entry point. Never loaded on a dedicated server. */
public class SteelstormClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientHooks.init();
    }
}

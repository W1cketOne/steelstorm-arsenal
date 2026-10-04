package com.steelstorm.compat.neo.neoforge.event.server;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.server.MinecraftServer;

public class ServerStartedEvent extends Event {
    private final MinecraftServer server;
    public ServerStartedEvent(MinecraftServer server) { this.server = server; }
    public MinecraftServer getServer() { return server; }
}

package com.steelstorm.compat.neo.neoforge.event.server;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.server.MinecraftServer;

public class ServerStoppedEvent extends Event {
    private final MinecraftServer server;
    public ServerStoppedEvent(MinecraftServer server) { this.server = server; }
    public MinecraftServer getServer() { return server; }
}

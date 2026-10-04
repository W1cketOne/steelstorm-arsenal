package com.steelstorm.compat.neo.neoforge.event.tick;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.server.MinecraftServer;

public abstract class ServerTickEvent extends Event {
    private final MinecraftServer server;
    protected ServerTickEvent(MinecraftServer server) { this.server = server; }
    public MinecraftServer getServer() { return server; }
    public static class Pre extends ServerTickEvent { public Pre(MinecraftServer s) { super(s); } }
    public static class Post extends ServerTickEvent { public Post(MinecraftServer s) { super(s); } }
}

package com.steelstorm.compat.neo.neoforge.event.tick;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.entity.player.Player;

public abstract class PlayerTickEvent extends Event {
    private final Player player;
    protected PlayerTickEvent(Player player) { this.player = player; }
    public Player getEntity() { return player; }
    public static class Pre extends PlayerTickEvent { public Pre(Player p) { super(p); } }
    public static class Post extends PlayerTickEvent { public Post(Player p) { super(p); } }
}

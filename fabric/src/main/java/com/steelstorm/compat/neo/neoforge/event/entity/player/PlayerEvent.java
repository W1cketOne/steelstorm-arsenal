package com.steelstorm.compat.neo.neoforge.event.entity.player;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class PlayerEvent extends Event {
    private final Player player;
    protected PlayerEvent(Player player) { this.player = player; }
    public Player getEntity() { return player; }
    public static class PlayerLoggedInEvent extends PlayerEvent { public PlayerLoggedInEvent(Player p) { super(p); } }
    public static class PlayerLoggedOutEvent extends PlayerEvent { public PlayerLoggedOutEvent(Player p) { super(p); } }
    public static class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;
        public PlayerRespawnEvent(Player p, boolean endConquered) { super(p); this.endConquered = endConquered; }
        public boolean isEndConquered() { return endConquered; }
    }
    public static class PlayerChangedDimensionEvent extends PlayerEvent {
        private final ResourceKey<Level> from;
        private final ResourceKey<Level> to;
        public PlayerChangedDimensionEvent(Player p, ResourceKey<Level> from, ResourceKey<Level> to) { super(p); this.from = from; this.to = to; }
        public ResourceKey<Level> getFrom() { return from; }
        public ResourceKey<Level> getTo() { return to; }
    }
}

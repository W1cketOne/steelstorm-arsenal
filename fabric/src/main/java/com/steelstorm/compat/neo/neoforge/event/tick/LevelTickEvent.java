package com.steelstorm.compat.neo.neoforge.event.tick;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.level.Level;

public abstract class LevelTickEvent extends Event {
    private final Level level;
    protected LevelTickEvent(Level level) { this.level = level; }
    public Level getLevel() { return level; }
    public static class Pre extends LevelTickEvent { public Pre(Level l) { super(l); } }
    public static class Post extends LevelTickEvent { public Post(Level l) { super(l); } }
}

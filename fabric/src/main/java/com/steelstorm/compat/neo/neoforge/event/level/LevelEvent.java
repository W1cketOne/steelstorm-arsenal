package com.steelstorm.compat.neo.neoforge.event.level;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

public abstract class LevelEvent extends Event {
    private final LevelAccessor level;
    protected LevelEvent(LevelAccessor level) { this.level = level; }
    public LevelAccessor getLevel() { return level; }
    public static class CreateSpawnPosition extends LevelEvent {
        public CreateSpawnPosition(ServerLevel level) { super(level); }
    }
}

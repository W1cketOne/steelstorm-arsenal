package com.steelstorm.compat.neo.neoforge.event.entity.player;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CriticalHitEvent extends Event {
    private final Player player;
    private final Entity target;
    private boolean crit;
    public CriticalHitEvent(Player player, Entity target, boolean crit) { this.player = player; this.target = target; this.crit = crit; }
    public Player getEntity() { return player; }
    public Entity getTarget() { return target; }
    public boolean isCriticalHit() { return crit; }
    public void setCriticalHit(boolean crit) { this.crit = crit; }
}

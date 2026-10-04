package com.steelstorm.compat.neo.neoforge.event.entity.player;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AttackEntityEvent extends Event {
    private final Player player;
    private final Entity target;
    public AttackEntityEvent(Player player, Entity target) { this.player = player; this.target = target; }
    public Player getEntity() { return player; }
    public Entity getTarget() { return target; }
}

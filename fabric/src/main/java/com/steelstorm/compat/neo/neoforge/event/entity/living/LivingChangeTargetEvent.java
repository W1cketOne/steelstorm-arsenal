package com.steelstorm.compat.neo.neoforge.event.entity.living;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.entity.LivingEntity;

public class LivingChangeTargetEvent extends Event {
    private final LivingEntity entity;
    private LivingEntity target;
    public LivingChangeTargetEvent(LivingEntity entity, LivingEntity target) { this.entity = entity; this.target = target; }
    public LivingEntity getEntity() { return entity; }
    public LivingEntity getNewAboutToBeSetTarget() { return target; }
    public void setNewAboutToBeSetTarget(LivingEntity target) { this.target = target; }
}

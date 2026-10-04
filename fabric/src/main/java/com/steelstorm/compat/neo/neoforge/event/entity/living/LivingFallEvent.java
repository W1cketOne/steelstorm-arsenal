package com.steelstorm.compat.neo.neoforge.event.entity.living;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.entity.LivingEntity;

public class LivingFallEvent extends Event {
    private final LivingEntity entity;
    private float distance;
    private float multiplier;
    public LivingFallEvent(LivingEntity entity, float distance, float multiplier) { this.entity = entity; this.distance = distance; this.multiplier = multiplier; }
    public LivingEntity getEntity() { return entity; }
    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }
    public float getDamageMultiplier() { return multiplier; }
    public void setDamageMultiplier(float multiplier) { this.multiplier = multiplier; }
}

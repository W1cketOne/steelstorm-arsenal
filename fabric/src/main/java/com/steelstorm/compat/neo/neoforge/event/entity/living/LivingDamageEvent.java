package com.steelstorm.compat.neo.neoforge.event.entity.living;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public abstract class LivingDamageEvent extends Event {
    private final LivingEntity entity;
    private final DamageSource source;
    protected LivingDamageEvent(LivingEntity entity, DamageSource source) { this.entity = entity; this.source = source; }
    public LivingEntity getEntity() { return entity; }
    public DamageSource getSource() { return source; }
    public static class Post extends LivingDamageEvent {
        private final float damage;
        public Post(LivingEntity entity, DamageSource source, float damage) { super(entity, source); this.damage = damage; }
        public float getNewDamage() { return damage; }
    }
}

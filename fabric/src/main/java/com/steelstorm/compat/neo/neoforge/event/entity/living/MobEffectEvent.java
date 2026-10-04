package com.steelstorm.compat.neo.neoforge.event.entity.living;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public abstract class MobEffectEvent extends Event {
    private final LivingEntity entity;
    private final MobEffectInstance effect;
    protected MobEffectEvent(LivingEntity entity, MobEffectInstance effect) { this.entity = entity; this.effect = effect; }
    public LivingEntity getEntity() { return entity; }
    public MobEffectInstance getEffectInstance() { return effect; }
    public static class Applicable extends MobEffectEvent {
        public enum Result { APPLY, DEFAULT, DO_NOT_APPLY }
        private Result result = Result.DEFAULT;
        public Applicable(LivingEntity entity, MobEffectInstance effect) { super(entity, effect); }
        public void setResult(Result result) { this.result = result; }
        public Result getResult() { return result; }
    }
}

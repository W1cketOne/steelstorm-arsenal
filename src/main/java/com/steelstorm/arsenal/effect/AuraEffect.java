package com.steelstorm.arsenal.effect;

import com.steelstorm.arsenal.fx.FxParticleType;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * An effect that shows itself with Steelstorm particles around whoever has it (frost for Frozen,
 * embers for Berserk...). Any gameplay comes from attribute modifiers or the combat handlers.
 */
public class AuraEffect extends MobEffect {
    private final Supplier<? extends FxParticleType> particle;
    private final int particleColor;
    private final int interval;
    private final float particleScale;

    public AuraEffect(MobEffectCategory category, int color, Supplier<? extends FxParticleType> particle, int particleColor, int interval,
                      float particleScale) {
        super(category, color);
        this.particle = particle;
        this.particleColor = particleColor;
        this.interval = Math.max(1, interval);
        this.particleScale = particleScale;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            double w = entity.getBbWidth() * 0.45;
            level.sendParticles(particle.get().with(particleColor, particleScale), entity.getX(), entity.getY(0.55), entity.getZ(),
                    2, w, entity.getBbHeight() * 0.3, w, 0.02);
        }
        return true;
    }
}

package com.steelstorm.arsenal.effect;

import com.steelstorm.arsenal.registry.ModDamageTypes;
import com.steelstorm.arsenal.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Bleed: damage over time that stacks. Each new application raises the amplifier (up to 5 stacks)
 * and each stack adds 0.5 damage per second. Ignores armour but never kills on its own below 1 HP.
 */
public class BleedEffect extends MobEffect {
    public static final int MAX_STACKS = 5;

    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 0x9E1B1B);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            float damage = 1.0F + amplifier * 0.5F;
            if (entity.getHealth() > 1.0F) {
                entity.hurt(ModDamageTypes.bleed(level), Math.min(damage, entity.getHealth() - 0.5F));
                // Don't let the bleed tick swallow the next real hit's invulnerability window.
                entity.invulnerableTime = 0;
            }
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY(0.6), entity.getZ(),
                    2 + amplifier, 0.25, 0.3, 0.25, 0.0);
        }
        return true;
    }

    /** Adds one stack of Bleed, refreshing the duration. */
    public static void apply(LivingEntity target, int durationTicks) {
        MobEffectInstance current = target.getEffect(ModEffects.BLEED);
        int amplifier = current == null ? 0 : Math.min(MAX_STACKS - 1, current.getAmplifier() + 1);
        int duration = current == null ? durationTicks : Math.max(durationTicks, current.getDuration());
        target.addEffect(new MobEffectInstance(ModEffects.BLEED, duration, amplifier));
    }
}

package com.steelstorm.arsenal.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** An effect whose behaviour comes entirely from attribute modifiers or event handlers. */
public class SimpleEffect extends MobEffect {
    public SimpleEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

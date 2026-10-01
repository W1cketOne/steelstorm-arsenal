package com.steelstorm.arsenal.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stagger: the victim is slowed (attribute modifier) and cannot land melee attacks
 * (enforced in {@link com.steelstorm.arsenal.combat.CombatEvents}).
 */
public class StaggerEffect extends MobEffect {
    public StaggerEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8C22C);
    }
}

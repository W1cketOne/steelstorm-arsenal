package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.effect.SimpleEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SteelstormArsenal.MODID);

    /** Slowed and unable to land melee hits (enforced in CombatEvents). */
    public static final DeferredHolder<MobEffect, MobEffect> STAGGER = EFFECTS.register("stagger",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xE8C22C)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, SteelstormArsenal.id("effect.stagger"), -0.45,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Stacking damage over time. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEED = EFFECTS.register("bleed", BleedEffect::new);

    /** -4 armour. */
    public static final DeferredHolder<MobEffect, MobEffect> ARMOR_BREAK = EFFECTS.register("armor_break",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x8A8A8A)
                    .addAttributeModifier(Attributes.ARMOR, SteelstormArsenal.id("effect.armor_break"), -4.0,
                            AttributeModifier.Operation.ADD_VALUE));

    private ModEffects() {
    }
}

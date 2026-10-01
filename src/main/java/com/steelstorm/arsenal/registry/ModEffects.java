package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.effect.StaggerEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SteelstormArsenal.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> STAGGER = EFFECTS.register("stagger", () -> new StaggerEffect()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, SteelstormArsenal.id("effect.stagger"), -0.45,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {
    }
}

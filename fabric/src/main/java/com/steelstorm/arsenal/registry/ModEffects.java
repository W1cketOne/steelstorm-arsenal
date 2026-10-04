package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.effect.AuraEffect;
import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.effect.SimpleEffect;
import com.steelstorm.arsenal.fx.Fx;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.steelstorm.compat.neo.neoforge.registries.DeferredHolder;
import com.steelstorm.compat.neo.neoforge.registries.DeferredRegister;

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

    /** Encased in ice: can't move, jump or land melee hits. */
    public static final DeferredHolder<MobEffect, MobEffect> FROZEN = EFFECTS.register("frozen",
            () -> new AuraEffect(MobEffectCategory.HARMFUL, 0x9FF3FF, ModParticles.FROST, Fx.FROST, 5, 1.2F)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, SteelstormArsenal.id("effect.frozen"), -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, SteelstormArsenal.id("effect.frozen_jump"), -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Battleaxe ultimate: faster, harder swings, lifesteal, immune to Stagger. */
    public static final DeferredHolder<MobEffect, MobEffect> BERSERK = EFFECTS.register("berserk",
            () -> new AuraEffect(MobEffectCategory.BENEFICIAL, 0xD0182C, ModParticles.GLOW, 0xFF4040, 3, 1.5F)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, SteelstormArsenal.id("effect.berserk_speed"), 0.6,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, SteelstormArsenal.id("effect.berserk_damage"), 4.0,
                            AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, SteelstormArsenal.id("effect.berserk_move"), 0.15,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Longsword stance: the next melee attack against you is countered. */
    public static final DeferredHolder<MobEffect, MobEffect> RIPOSTE = EFFECTS.register("riposte",
            () -> new AuraEffect(MobEffectCategory.BENEFICIAL, 0xDCE6F0, ModParticles.SPARK, Fx.STEEL, 4, 1.0F)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, SteelstormArsenal.id("effect.riposte"), -0.3,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Dual daggers ultimate: wrapped in shadow, faster, and enemies lose track of you. */
    public static final DeferredHolder<MobEffect, MobEffect> SHADOW_VEIL = EFFECTS.register("shadow_veil",
            () -> new AuraEffect(MobEffectCategory.BENEFICIAL, 0x3A2E5C, ModParticles.SMOKE, 0x241A38, 2, 1.1F)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, SteelstormArsenal.id("effect.shadow_veil"), 0.3,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Scythe: marked enemies take 15% more damage and heal whoever hits them. */
    public static final DeferredHolder<MobEffect, MobEffect> MARKED = EFFECTS.register("marked",
            () -> new AuraEffect(MobEffectCategory.HARMFUL, 0xC084FC, ModParticles.RUNE, 0xC084FC, 10, 1.0F));

    private ModEffects() {
    }
}

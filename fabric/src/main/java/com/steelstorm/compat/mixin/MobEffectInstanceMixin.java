package com.steelstorm.compat.mixin;

import com.steelstorm.compat.neo.neoforge.registries.DeferredHolder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Effect instances made from our deferred holders must hold the registry's own reference:
 * saving an entity encodes the effect with a codec that rejects any other Holder, which crashed
 * world saves while a mob was staggered, bleeding, marked, etc.
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {
    @ModifyVariable(method = "<init>(Lnet/minecraft/core/Holder;IIZZZLnet/minecraft/world/effect/MobEffectInstance;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Holder<MobEffect> steelstorm$unwrap(Holder<MobEffect> effect) {
        return effect instanceof DeferredHolder<MobEffect, ?> deferred ? deferred.delegate() : effect;
    }
}

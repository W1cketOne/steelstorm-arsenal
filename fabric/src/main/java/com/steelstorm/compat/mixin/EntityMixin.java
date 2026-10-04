package com.steelstorm.compat.mixin;

import com.steelstorm.arsenal.combat.UltGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    // Explosions neither hurt nor shove a player mid-ultimate.
    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void steelstorm$ultIgnoresExplosions(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        if (UltGuard.isProtected((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}

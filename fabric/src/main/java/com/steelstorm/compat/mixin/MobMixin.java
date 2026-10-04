package com.steelstorm.compat.mixin;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void steelstorm$changeTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (target != null && target != self.getTarget()
                && NeoBus.post(new LivingChangeTargetEvent(self, target)).isCanceled()) {
            ci.cancel();
        }
    }
}

package com.steelstorm.compat.mixin;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Unique
    private float steelstorm$playerHealthBefore;

    @Inject(method = "tick", at = @At("HEAD"))
    private void steelstorm$tickPre(CallbackInfo ci) {
        NeoBus.post(new PlayerTickEvent.Pre((Player) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void steelstorm$tickPost(CallbackInfo ci) {
        NeoBus.post(new PlayerTickEvent.Post((Player) (Object) this));
    }

    // Player overrides actuallyHurt without calling super, so it needs its own hook.
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void steelstorm$beforeDamage(DamageSource source, float amount, CallbackInfo ci) {
        steelstorm$playerHealthBefore = ((Player) (Object) this).getHealth();
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void steelstorm$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        float dealt = steelstorm$playerHealthBefore - self.getHealth();
        if (dealt > 0 && !self.level().isClientSide) {
            NeoBus.post(new LivingDamageEvent.Post(self, source, dealt));
        }
    }
}

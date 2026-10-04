package com.steelstorm.compat.mixin.client;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void steelstorm$attackKey(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null || mc.player.isHandsBusy() || mc.missTime > 0) {
            return;
        }
        if (NeoBus.post(new InputEvent.InteractionKeyMappingTriggered(0, mc.options.keyAttack, InteractionHand.MAIN_HAND)).isCanceled()) {
            cir.setReturnValue(false);
        }
    }
}

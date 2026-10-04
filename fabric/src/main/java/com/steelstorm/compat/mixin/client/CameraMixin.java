package com.steelstorm.compat.mixin.client;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float steelstorm$detachedDistance(float distance) {
        return (float) NeoBus.post(new CalculateDetachedCameraDistanceEvent(distance)).getDistance();
    }
}

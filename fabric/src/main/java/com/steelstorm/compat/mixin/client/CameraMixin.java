package com.steelstorm.compat.mixin.client;

import com.steelstorm.arsenal.client.TrailerDirector;
import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float steelstorm$detachedDistance(float distance) {
        return (float) NeoBus.post(new CalculateDetachedCameraDistanceEvent(distance)).getDistance();
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void steelstorm$director(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (!TrailerDirector.enabled()) {
            return;
        }
        double[] cam = TrailerDirector.camera(partialTick);
        if (cam != null) {
            this.detached = true;
            setRotation((float) cam[3], (float) cam[4]);
            setPosition(new Vec3(cam[0], cam[1], cam[2]));
        }
    }
}

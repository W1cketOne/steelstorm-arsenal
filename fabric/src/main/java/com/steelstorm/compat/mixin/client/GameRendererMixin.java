package com.steelstorm.compat.mixin.client;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.client.event.ViewportEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", shift = At.Shift.AFTER))
    private void steelstorm$cameraAngles(DeltaTracker delta, CallbackInfo ci) {
        GameRenderer self = (GameRenderer) (Object) this;
        Camera camera = self.getMainCamera();
        ViewportEvent.ComputeCameraAngles event = NeoBus.post(new ViewportEvent.ComputeCameraAngles(camera,
                delta.getGameTimeDeltaPartialTick(true), camera.getYRot(), camera.getXRot(), 0.0F));
        camera.setRotation(event.getYaw(), event.getPitch());
        if (event.getRoll() != 0.0F) {
            // Same quaternion NeoForge builds for a rolled camera; renderLevel reads it straight after.
            float rad = (float) Math.PI / 180.0F;
            camera.rotation().rotationYXZ((float) Math.PI - event.getYaw() * rad, -event.getPitch() * rad, -event.getRoll() * rad);
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void steelstorm$fov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        ViewportEvent.ComputeFov event = NeoBus.post(new ViewportEvent.ComputeFov(camera, partialTick, cir.getReturnValue(), useFovSetting));
        cir.setReturnValue(event.getFOV());
    }
}

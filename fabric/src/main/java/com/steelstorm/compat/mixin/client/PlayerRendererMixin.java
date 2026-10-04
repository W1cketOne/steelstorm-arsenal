package com.steelstorm.compat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.client.event.RenderPlayerEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void steelstorm$pre(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                                CallbackInfo ci) {
        if (NeoBus.post(new RenderPlayerEvent.Pre(player, (PlayerRenderer) (Object) this, partialTick, pose, buffers, light)).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void steelstorm$post(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                                 CallbackInfo ci) {
        NeoBus.post(new RenderPlayerEvent.Post(player, (PlayerRenderer) (Object) this, partialTick, pose, buffers, light));
    }
}

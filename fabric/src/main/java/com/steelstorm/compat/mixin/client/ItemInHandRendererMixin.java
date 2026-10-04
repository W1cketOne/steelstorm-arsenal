package com.steelstorm.compat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.client.event.RenderHandEvent;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.IClientItemExtensions;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack pose,
                                    MultiBufferSource buffers, int light);

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void steelstorm$renderHand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing,
                                       ItemStack stack, float equip, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (NeoBus.post(new RenderHandEvent(hand, pose, buffers, light, partialTick, pitch, swing, equip, stack)).isCanceled()) {
            ci.cancel();
            return;
        }
        IClientItemExtensions ext = stack.isEmpty() ? null : RegisterClientExtensionsEvent.of(stack.getItem());
        if (ext == null || player != Minecraft.getInstance().player) {
            return;
        }
        boolean main = hand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        pose.pushPose();
        if (ext.applyForgeHandTransform(pose, Minecraft.getInstance().player, arm, stack, partialTick, equip, swing)) {
            boolean right = arm == HumanoidArm.RIGHT;
            renderItem(player, stack, right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    !right, pose, buffers, light);
            pose.popPose();
            ci.cancel();
            return;
        }
        pose.popPose();
    }
}

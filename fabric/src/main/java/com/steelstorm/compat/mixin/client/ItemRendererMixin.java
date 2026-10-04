package com.steelstorm.compat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.compat.client.HeldModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private BakedModel steelstorm$heldModel(BakedModel model, ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack pose,
                                            MultiBufferSource buffers, int light, int overlay) {
        return HeldModels.swap(stack, ctx, model);
    }
}

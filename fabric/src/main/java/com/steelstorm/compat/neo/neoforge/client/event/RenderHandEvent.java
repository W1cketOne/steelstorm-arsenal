package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class RenderHandEvent extends Event {
    private final InteractionHand hand;
    private final PoseStack pose;
    private final MultiBufferSource buffers;
    private final int light;
    private final float partialTick, swing, equip;
    private final ItemStack stack;
    public RenderHandEvent(InteractionHand hand, PoseStack pose, MultiBufferSource buffers, int light, float partialTick,
            float interpPitch, float swing, float equip, ItemStack stack) {
        this.hand = hand; this.pose = pose; this.buffers = buffers; this.light = light; this.partialTick = partialTick;
        this.swing = swing; this.equip = equip; this.stack = stack;
    }
    public InteractionHand getHand() { return hand; }
    public PoseStack getPoseStack() { return pose; }
    public MultiBufferSource getMultiBufferSource() { return buffers; }
    public int getPackedLight() { return light; }
    public float getPartialTick() { return partialTick; }
    public float getSwingProgress() { return swing; }
    public float getEquipProgress() { return equip; }
    public ItemStack getItemStack() { return stack; }
}

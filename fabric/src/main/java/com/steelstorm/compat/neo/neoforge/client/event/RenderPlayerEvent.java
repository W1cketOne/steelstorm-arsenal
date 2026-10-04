package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;

public abstract class RenderPlayerEvent extends Event {
    private final Player player;
    private final PlayerRenderer renderer;
    private final float partialTick;
    private final PoseStack pose;
    private final MultiBufferSource buffers;
    private final int light;
    protected RenderPlayerEvent(Player player, PlayerRenderer renderer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        this.player = player; this.renderer = renderer; this.partialTick = partialTick; this.pose = pose; this.buffers = buffers; this.light = light;
    }
    public Player getEntity() { return player; }
    public PlayerRenderer getRenderer() { return renderer; }
    public float getPartialTick() { return partialTick; }
    public PoseStack getPoseStack() { return pose; }
    public MultiBufferSource getMultiBufferSource() { return buffers; }
    public int getPackedLight() { return light; }
    public static class Pre extends RenderPlayerEvent {
        public Pre(Player p, PlayerRenderer r, float t, PoseStack s, MultiBufferSource b, int l) { super(p, r, t, s, b, l); }
    }
    public static class Post extends RenderPlayerEvent {
        public Post(Player p, PlayerRenderer r, float t, PoseStack s, MultiBufferSource b, int l) { super(p, r, t, s, b, l); }
    }
}

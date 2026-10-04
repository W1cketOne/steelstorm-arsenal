package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.network.SuitGlowPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Which players' armour is blazing with an ultimate's light, and in what colour. */
public final class SuitGlow {
    private record Glow(long until, int total, int color) {
    }

    private static final Int2ObjectOpenHashMap<Glow> GLOWS = new Int2ObjectOpenHashMap<>();

    public static void start(SuitGlowPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        GLOWS.put(payload.entityId(), new Glow(mc.level.getGameTime() + payload.ticks(), payload.ticks(), payload.color()));
    }

    /** Glow strength 0..1 for this entity right now (fades in and out), or 0. */
    public static float strength(Entity entity, float partial) {
        Glow g = GLOWS.get(entity.getId());
        if (g == null || entity.level() == null) {
            return 0;
        }
        float left = g.until() - entity.level().getGameTime() - partial;
        if (left <= 0) {
            GLOWS.remove(entity.getId());
            return 0;
        }
        float elapsed = g.total() - left;
        return Math.min(1.0F, Math.min(elapsed / 6.0F, left / 10.0F));
    }

    public static int color(Entity entity) {
        Glow g = GLOWS.get(entity.getId());
        return g == null ? 0xFFFFFF : g.color();
    }

    public static void clear() {
        GLOWS.clear();
    }

    private SuitGlow() {
    }
}

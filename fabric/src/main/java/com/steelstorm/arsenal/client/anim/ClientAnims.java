package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.anim.CastPose;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Which entities are playing a {@link CastPose} right now, and how far along they are. */
public final class ClientAnims {
    private record Playing(CastPose pose, long start, int duration) {
    }

    /** A pose in progress: {@code t} runs from 0 to 1 over its duration. */
    public record Active(CastPose pose, float t, float ticks) {
    }

    private static final Int2ObjectOpenHashMap<Playing> PLAYING = new Int2ObjectOpenHashMap<>();

    private ClientAnims() {
    }

    public static void start(int entityId, CastPose pose, int duration) {
        if (Minecraft.getInstance().level == null || pose == CastPose.NONE) {
            return;
        }
        PLAYING.put(entityId, new Playing(pose, Minecraft.getInstance().level.getGameTime(), Math.max(1, duration)));
    }

    @Nullable
    public static Active get(Entity entity, float partialTick) {
        Playing playing = PLAYING.get(entity.getId());
        if (playing == null || entity.level() == null) {
            return null;
        }
        float ticks = entity.level().getGameTime() - playing.start + partialTick;
        if (ticks >= playing.duration || ticks < 0) {
            if (ticks >= playing.duration + 2) {
                PLAYING.remove(entity.getId());
            }
            return null;
        }
        return new Active(playing.pose, ticks / playing.duration, ticks);
    }

    public static void clear() {
        PLAYING.clear();
    }
}

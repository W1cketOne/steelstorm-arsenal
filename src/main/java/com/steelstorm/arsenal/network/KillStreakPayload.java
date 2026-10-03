package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: you chained `count` kills (2+) or reached a mastery rank (count < 0: -rank). */
public record KillStreakPayload(int count) implements CustomPacketPayload {
    public static final Type<KillStreakPayload> TYPE = new Type<>(SteelstormArsenal.id("kill_streak"));
    public static final StreamCodec<ByteBuf, KillStreakPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KillStreakPayload::count, KillStreakPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

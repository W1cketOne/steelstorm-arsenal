package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: shake the camera (heavy hits, parries, specials). The client config can turn it off. */
public record FeedbackPayload(float shake, int ticks) implements CustomPacketPayload {
    public static final Type<FeedbackPayload> TYPE = new Type<>(SteelstormArsenal.id("feedback"));
    public static final StreamCodec<ByteBuf, FeedbackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FeedbackPayload::shake,
            ByteBufCodecs.VAR_INT, FeedbackPayload::ticks,
            FeedbackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

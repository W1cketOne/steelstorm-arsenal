package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player pressed Special. The server decides whether it actually happens. */
public record SpecialPayload() implements CustomPacketPayload {
    public static final SpecialPayload INSTANCE = new SpecialPayload();
    public static final Type<SpecialPayload> TYPE = new Type<>(SteelstormArsenal.id("special"));
    public static final StreamCodec<ByteBuf, SpecialPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

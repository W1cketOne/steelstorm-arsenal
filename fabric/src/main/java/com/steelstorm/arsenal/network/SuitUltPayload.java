package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the suit-ultimate key was pressed. */
public record SuitUltPayload() implements CustomPacketPayload {
    public static final Type<SuitUltPayload> TYPE = new Type<>(SteelstormArsenal.id("suit_ultimate"));
    public static final StreamCodec<ByteBuf, SuitUltPayload> STREAM_CODEC = StreamCodec.unit(new SuitUltPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

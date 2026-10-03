package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player jumped again in mid-air while wearing Stormsteel boots. */
public record DoubleJumpPayload() implements CustomPacketPayload {
    public static final Type<DoubleJumpPayload> TYPE = new Type<>(SteelstormArsenal.id("double_jump"));
    public static final StreamCodec<ByteBuf, DoubleJumpPayload> STREAM_CODEC = StreamCodec.unit(new DoubleJumpPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

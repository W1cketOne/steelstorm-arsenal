package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player pressed jump in mid-air wearing Voidwalker boots. */
public record BlinkPayload() implements CustomPacketPayload {
    public static final Type<BlinkPayload> TYPE = new Type<>(SteelstormArsenal.id("void_blink"));
    public static final StreamCodec<ByteBuf, BlinkPayload> STREAM_CODEC = StreamCodec.unit(new BlinkPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

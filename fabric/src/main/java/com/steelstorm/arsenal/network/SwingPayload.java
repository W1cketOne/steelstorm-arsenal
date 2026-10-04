package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player swung a Steelstorm weapon at full strength. Purely cosmetic: the
 * server only uses it to show the same slash trail and whoosh to everyone else nearby.
 */
public record SwingPayload(boolean heavy, float roll) implements CustomPacketPayload {
    public static final Type<SwingPayload> TYPE = new Type<>(SteelstormArsenal.id("swing"));
    public static final StreamCodec<ByteBuf, SwingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SwingPayload::heavy,
            ByteBufCodecs.FLOAT, SwingPayload::roll,
            SwingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

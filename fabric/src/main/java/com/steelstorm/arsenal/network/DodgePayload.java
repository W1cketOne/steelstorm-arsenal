package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player pressed Dodge. Carries the movement input so the roll goes that way. */
public record DodgePayload(float forward, float strafe) implements CustomPacketPayload {
    public static final Type<DodgePayload> TYPE = new Type<>(SteelstormArsenal.id("dodge"));
    public static final StreamCodec<ByteBuf, DodgePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DodgePayload::forward,
            ByteBufCodecs.FLOAT, DodgePayload::strafe,
            DodgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

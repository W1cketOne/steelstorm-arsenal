package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: a number to pop off an enemy you just hurt. `kind` is 0 normal, 1 critical, 2 ability. */
public record DamageNumberPayload(float x, float y, float z, float amount, int kind) implements CustomPacketPayload {
    public static final Type<DamageNumberPayload> TYPE = new Type<>(SteelstormArsenal.id("damage_number"));
    public static final StreamCodec<ByteBuf, DamageNumberPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DamageNumberPayload::x,
            ByteBufCodecs.FLOAT, DamageNumberPayload::y,
            ByteBufCodecs.FLOAT, DamageNumberPayload::z,
            ByteBufCodecs.FLOAT, DamageNumberPayload::amount,
            ByteBufCodecs.VAR_INT, DamageNumberPayload::kind,
            DamageNumberPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

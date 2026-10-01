package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player pressed an ability key (0-2) or the ultimate key (3). The server decides what happens. */
public record AbilityPayload(int slot) implements CustomPacketPayload {
    public static final Type<AbilityPayload> TYPE = new Type<>(SteelstormArsenal.id("ability"));
    public static final StreamCodec<ByteBuf, AbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityPayload::slot,
            AbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

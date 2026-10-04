package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player pressed an ability key (0-2) or the ultimate key (3). The server decides what happens.
 * `phase` is {@link #USE} for an instant use, or {@link #CHARGE_START} / {@link #CHARGE_RELEASE} while holding the ultimate.
 */
public record AbilityPayload(int slot, int phase) implements CustomPacketPayload {
    public static final Type<AbilityPayload> TYPE = new Type<>(SteelstormArsenal.id("ability"));
    public static final StreamCodec<ByteBuf, AbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityPayload::slot,
            ByteBufCodecs.VAR_INT, AbilityPayload::phase,
            AbilityPayload::new);
    public static final int USE = 0;
    public static final int CHARGE_START = 1;
    public static final int CHARGE_RELEASE = 2;

    public AbilityPayload(int slot) {
        this(slot, USE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

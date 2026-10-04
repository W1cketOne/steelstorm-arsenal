package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.compat.neo.neoforge.network.PacketDistributor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Server to clients: a player's armour blazes with light for `ticks` (during an ultimate). */
public record SuitGlowPayload(int entityId, int ticks, int color) implements CustomPacketPayload {
    public static final Type<SuitGlowPayload> TYPE = new Type<>(SteelstormArsenal.id("suit_glow"));
    public static final StreamCodec<ByteBuf, SuitGlowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SuitGlowPayload::entityId,
            ByteBufCodecs.VAR_INT, SuitGlowPayload::ticks,
            ByteBufCodecs.INT, SuitGlowPayload::color,
            SuitGlowPayload::new);

    public static void send(ServerPlayer player, int ticks, int color) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SuitGlowPayload(player.getId(), ticks, color));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

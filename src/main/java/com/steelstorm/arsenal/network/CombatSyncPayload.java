package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: everything the HUD needs. Cooldowns are sent as ticks remaining. */
public record CombatSyncPayload(float stamina, float maxStamina, int specialCooldownLeft, int specialCooldownTotal,
                                int dodgeCooldownLeft, int combo) implements CustomPacketPayload {
    public static final Type<CombatSyncPayload> TYPE = new Type<>(SteelstormArsenal.id("combat_sync"));
    public static final StreamCodec<ByteBuf, CombatSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CombatSyncPayload::stamina,
            ByteBufCodecs.FLOAT, CombatSyncPayload::maxStamina,
            ByteBufCodecs.VAR_INT, CombatSyncPayload::specialCooldownLeft,
            ByteBufCodecs.VAR_INT, CombatSyncPayload::specialCooldownTotal,
            ByteBufCodecs.VAR_INT, CombatSyncPayload::dodgeCooldownLeft,
            ByteBufCodecs.VAR_INT, CombatSyncPayload::combo,
            CombatSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

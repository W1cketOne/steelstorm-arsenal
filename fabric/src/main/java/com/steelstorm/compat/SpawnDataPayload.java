package com.steelstorm.compat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Extra spawn data for entities that implement IEntityWithComplexSpawn, sent right after the add-entity packet. */
public record SpawnDataPayload(int entityId, byte[] data) implements CustomPacketPayload {
    public static final Type<SpawnDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("steelstorm", "spawn_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpawnDataPayload::entityId,
            ByteBufCodecs.BYTE_ARRAY, SpawnDataPayload::data,
            SpawnDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

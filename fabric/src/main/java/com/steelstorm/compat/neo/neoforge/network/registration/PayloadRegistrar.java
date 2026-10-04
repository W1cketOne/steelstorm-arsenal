package com.steelstorm.compat.neo.neoforge.network.registration;

import com.steelstorm.compat.neo.neoforge.network.handling.IPayloadHandler;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class PayloadRegistrar {
    public record ClientEntry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
    }

    /** Client-bound handlers, registered with ClientPlayNetworking by the client initializer. */
    public static final List<ClientEntry<?>> CLIENT_HANDLERS = new ArrayList<>();

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> handler.handle(payload, ctx::player));
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        PayloadTypeRegistry.playS2C().register(type, codec);
        CLIENT_HANDLERS.add(new ClientEntry<>(type, handler));
        return this;
    }
}

package com.steelstorm.compat.neo.neoforge.network;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PacketDistributor {
    /** Installed by the client initializer so common code never touches client classes. */
    public static Consumer<CustomPacketPayload> clientSender = p -> { };

    public static void sendToServer(CustomPacketPayload payload) {
        clientSender.accept(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        for (ServerPlayer p : PlayerLookup.tracking(entity)) {
            if (p != entity) {
                sendToPlayer(p, payload);
            }
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        sendToPlayersTrackingEntity(entity, payload);
        if (entity instanceof ServerPlayer self) {
            sendToPlayer(self, payload);
        }
    }

    private PacketDistributor() {
    }
}

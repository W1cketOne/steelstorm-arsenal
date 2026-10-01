package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.anim.CastPose;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server to client: an entity starts a {@link CastPose} animation. */
public record PlayerAnimPayload(int entityId, int pose, int duration) implements CustomPacketPayload {
    public static final Type<PlayerAnimPayload> TYPE = new Type<>(SteelstormArsenal.id("anim"));
    public static final StreamCodec<ByteBuf, PlayerAnimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerAnimPayload::entityId,
            ByteBufCodecs.VAR_INT, PlayerAnimPayload::pose,
            ByteBufCodecs.VAR_INT, PlayerAnimPayload::duration,
            PlayerAnimPayload::new);

    /** Plays the pose on everyone who can see the entity (and the entity itself, if a player). */
    public static void send(Entity entity, CastPose pose) {
        PlayerAnimPayload payload = new PlayerAnimPayload(entity.getId(), pose.ordinal(), pose.duration);
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.steelstorm.compat.mixin;

import com.steelstorm.compat.SpawnDataPayload;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void steelstorm$spawnData(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> sender, CallbackInfo ci) {
        if (entity instanceof IEntityWithComplexSpawn complex && ServerPlayNetworking.canSend(player, SpawnDataPayload.TYPE)) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), entity.registryAccess());
            complex.writeSpawnData(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            sender.accept((Packet) ServerPlayNetworking.createS2CPacket(new SpawnDataPayload(entity.getId(), bytes)));
        }
    }
}

package com.steelstorm.arsenal.world.outpost;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.SteelstormConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Builds the Warrior's Outpost exactly once per new world and keeps freshly spawned players
 * out of its walls.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class OutpostEvents {
    private static final String GREETED_TAG = "steelstorm.outpost_greeted";

    private OutpostEvents() {
    }

    /** Only fires while a brand-new world is choosing its spawn, so existing worlds never get an outpost. */
    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            OutpostSavedData.get(level).markPending();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        OutpostSavedData data = OutpostSavedData.get(overworld);
        if (!data.isPending()) {
            return;
        }
        if (!SteelstormConfig.STARTER_OUTPOST.get()) {
            data.cancel();
            return;
        }
        try {
            OutpostPlacer.place(overworld, data);
        } catch (RuntimeException e) {
            // Never let a decoration crash world creation.
            SteelstormArsenal.LOGGER.error("Failed to place the Warrior's Outpost", e);
            data.cancel();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag persistent = player.getPersistentData();
        if (persistent.getBoolean(GREETED_TAG)) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        OutpostSavedData data = OutpostSavedData.get(overworld);
        if (!data.isPlaced() || data.safeSpawn() == null || player.level() != overworld) {
            return;
        }
        persistent.putBoolean(GREETED_TAG, true);
        BlockPos safe = data.safeSpawn();
        // First join: start on the path in front of the hall rather than wherever spawn fuzzing put us.
        if (player.getRespawnPosition() == null && player.blockPosition().closerThan(safe, 64)) {
            player.teleportTo(overworld, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, data.safeSpawnYaw(), 0.0f);
        }
        player.sendSystemMessage(Component.translatable("message.steelstorm.outpost_welcome").withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        if (player.level() != overworld) {
            return;
        }
        OutpostSavedData data = OutpostSavedData.get(overworld);
        BlockPos safe = data.safeSpawn();
        // Only world-spawn respawns are moved; a bed or anchor inside the hall is the player's choice.
        if (safe != null && player.getRespawnPosition() == null && OutpostPlacer.isInside(data, player.blockPosition())) {
            player.teleportTo(overworld, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, data.safeSpawnYaw(), 0.0f);
        }
    }
}

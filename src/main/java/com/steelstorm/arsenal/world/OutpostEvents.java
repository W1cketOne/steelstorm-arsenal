package com.steelstorm.arsenal.world;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = SteelstormArsenal.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class OutpostEvents {
    /** Only fires while a brand-new world chooses its spawn point, never for existing worlds. */
    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            OutpostSavedData.get(level).markPending();
        }
    }

    /** Builds after vanilla has finished picking the spawn point, exactly once per world. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        OutpostSavedData data = OutpostSavedData.get(overworld);
        if (data.isPending()) {
            try {
                data.markPlaced(OutpostBuilder.placeNearSpawn(overworld));
            } catch (RuntimeException e) {
                // Never crash world creation over a decoration; just don't try again.
                SteelstormArsenal.LOGGER.error("Failed to place the Warrior's Outpost", e);
                data.markPlaced(overworld.getSharedSpawnPos());
            }
        }
    }

    private OutpostEvents() {
    }
}

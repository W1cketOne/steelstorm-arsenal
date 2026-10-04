package com.steelstorm.arsenal.world;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.level.LevelEvent;
import com.steelstorm.compat.neo.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class OutpostEvents {
    /** Only fires while a brand-new world chooses its spawn point, never for existing worlds. */
    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && com.steelstorm.arsenal.Config.STARTER_OUTPOST.get()) {
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

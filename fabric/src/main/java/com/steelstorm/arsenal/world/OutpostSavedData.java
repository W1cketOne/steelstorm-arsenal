package com.steelstorm.arsenal.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world record for the Warrior's Outpost. {@code pending} is set only when a brand-new world
 * picks its spawn point; {@code placed} guarantees the outpost is built at most once, ever.
 */
public class OutpostSavedData extends SavedData {
    private static final String NAME = "steelstorm_outpost";

    private boolean pending;
    private boolean placed;
    private BlockPos position = BlockPos.ZERO;

    public static OutpostSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(OutpostSavedData::new, OutpostSavedData::load, null), NAME);
    }

    private static OutpostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        OutpostSavedData data = new OutpostSavedData();
        data.pending = tag.getBoolean("Pending");
        data.placed = tag.getBoolean("Placed");
        data.position = BlockPos.of(tag.getLong("Pos"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Pending", pending);
        tag.putBoolean("Placed", placed);
        tag.putLong("Pos", position.asLong());
        return tag;
    }

    public boolean isPending() {
        return pending && !placed;
    }

    public void markPending() {
        if (!placed) {
            pending = true;
            setDirty();
        }
    }

    public void markPlaced(BlockPos pos) {
        placed = true;
        pending = false;
        position = pos.immutable();
        setDirty();
    }

    public BlockPos position() {
        return position;
    }
}

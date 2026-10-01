package com.steelstorm.arsenal.world.outpost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Remembers whether the Warrior's Outpost has been placed in this world, so it is only ever built once,
 * and where it is, so new players can be put somewhere safe next to it.
 */
public class OutpostSavedData extends SavedData {
    private static final String NAME = "steelstorm_outpost";
    public static final SavedData.Factory<OutpostSavedData> FACTORY = new SavedData.Factory<>(OutpostSavedData::new, OutpostSavedData::load);

    /** Set when the world is first created; the outpost is built on the next server start. */
    private boolean pending;
    private boolean placed;
    @Nullable
    private BlockPos safeSpawn;
    private float safeSpawnYaw;
    @Nullable
    private BoundingBox bounds;

    public static OutpostSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static OutpostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        OutpostSavedData data = new OutpostSavedData();
        data.pending = tag.getBoolean("Pending");
        data.placed = tag.getBoolean("Placed");
        data.safeSpawn = NbtUtils.readBlockPos(tag, "SafeSpawn").orElse(null);
        data.safeSpawnYaw = tag.getFloat("SafeSpawnYaw");
        if (tag.contains("Bounds")) {
            int[] b = tag.getIntArray("Bounds");
            if (b.length == 6) {
                data.bounds = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Pending", pending);
        tag.putBoolean("Placed", placed);
        if (safeSpawn != null) {
            tag.put("SafeSpawn", NbtUtils.writeBlockPos(safeSpawn));
        }
        tag.putFloat("SafeSpawnYaw", safeSpawnYaw);
        if (bounds != null) {
            tag.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        }
        return tag;
    }

    public boolean isPending() {
        return pending && !placed;
    }

    public void markPending() {
        this.pending = true;
        setDirty();
    }

    public boolean isPlaced() {
        return placed;
    }

    public void markPlaced(BlockPos safeSpawn, float yaw, BoundingBox bounds) {
        this.placed = true;
        this.pending = false;
        this.safeSpawn = safeSpawn;
        this.safeSpawnYaw = yaw;
        this.bounds = bounds;
        setDirty();
    }

    /** Gives up on placing (e.g. disabled in config) so it is never attempted again. */
    public void cancel() {
        this.pending = false;
        setDirty();
    }

    @Nullable
    public BlockPos safeSpawn() {
        return safeSpawn;
    }

    public float safeSpawnYaw() {
        return safeSpawnYaw;
    }

    @Nullable
    public BoundingBox bounds() {
        return bounds;
    }
}

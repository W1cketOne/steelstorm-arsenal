package com.steelstorm.compat.neo.neoforge.event.entity;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public class RegisterSpawnPlacementsEvent extends Event {
    public enum Operation { REPLACE, OR, AND }
    public <T extends Entity> void register(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate, Operation op) {
        SpawnPlacements.register((EntityType) type, placement, heightmap, (SpawnPlacements.SpawnPredicate) predicate);
    }
}

package com.steelstorm.compat.neo.neoforge.common;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;

/** Entities are registered before items, so the type can be resolved right away. */
public class DeferredSpawnEggItem extends SpawnEggItem {
    public DeferredSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Properties props) {
        super(type.get(), background, highlight, props);
    }
}

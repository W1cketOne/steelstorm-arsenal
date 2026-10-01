package com.steelstorm.arsenal.entity;

import net.minecraft.world.item.ItemStack;

/** Thrown weapons rendered as their item by {@code ThrownWeaponRenderer}. */
public interface ThrownWeaponEntity {
    ItemStack getRenderStack();

    /** True for things that spin flat (chakram); false for things that fly point-first (spear, knives). */
    default boolean spins() {
        return false;
    }
}

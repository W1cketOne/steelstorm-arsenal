package com.steelstorm.arsenal.ability;

import org.jetbrains.annotations.Nullable;

/** The three abilities and the ultimate a weapon gives, in key order (R, G, V, Z by default). */
public record AbilitySet(String id, Ability first, Ability second, Ability third, Ability ultimate) {
    public static final int SLOTS = 4;

    @Nullable
    public Ability get(int slot) {
        return switch (slot) {
            case 0 -> first;
            case 1 -> second;
            case 2 -> third;
            case 3 -> ultimate;
            default -> null;
        };
    }

    public AbilitySet withUltimate(String newId, Ability newUltimate) {
        return new AbilitySet(newId, first, second, third, newUltimate);
    }
}

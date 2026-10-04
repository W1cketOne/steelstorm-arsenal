package com.steelstorm.arsenal.entity.boss;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;

/**
 * One attack in a boss's moveset: an animation (by id) and a timeline of what happens on each
 * tick of it. The boss stands its ground while a move plays unless the move moves it.
 */
public record BossMove(int id, int duration, double minRange, double maxRange, int cooldown, int weight, BooleanSupplier ready,
                       Action action) {
    @FunctionalInterface
    public interface Action {
        /** Called every tick of the move, t = 0 .. duration - 1. */
        void tick(int t, LivingEntity target);
    }

    public static BossMove of(int id, int duration, double minRange, double maxRange, int cooldown, Action action) {
        return new BossMove(id, duration, minRange, maxRange, cooldown, 10, () -> true, action);
    }

    public BossMove when(BooleanSupplier condition) {
        return new BossMove(id, duration, minRange, maxRange, cooldown, weight, condition, action);
    }

    public BossMove weight(int w) {
        return new BossMove(id, duration, minRange, maxRange, cooldown, w, ready, action);
    }
}

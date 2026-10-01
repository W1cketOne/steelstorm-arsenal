package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vanilla swings always last 6 ticks. This stretches them per weapon (a greatsword chop takes
 * twice as long as a dagger stab) and decides which of the two alternating cuts each swing uses.
 */
public final class SwingClock {
    private static final class State {
        float lastVanilla;
        double start = -1000;
        int cut;
    }

    private static final WeakHashMap<LivingEntity, State> STATES = new WeakHashMap<>();

    private SwingClock() {
    }

    public static int duration(WeaponType type) {
        return switch (type) {
            case DUAL_DAGGERS -> 6;
            case KATANA -> 7;
            case LONGSWORD, SPEAR -> 9;
            case SCYTHE, BATTLEAXE -> 11;
            case GREATSWORD, WARHAMMER -> 13;
        };
    }

    /** Progress 0..1 of the current swing, or 0 when not swinging. */
    public static float progress(LivingEntity entity, float vanillaSwing, WeaponType type, float partialTick) {
        State state = STATES.computeIfAbsent(entity, e -> new State());
        double now = entity.tickCount + partialTick;
        // A new swing starts when vanilla's progress jumps back toward zero.
        if (vanillaSwing > 0 && (state.lastVanilla == 0 || vanillaSwing < state.lastVanilla - 0.05F)) {
            if (now - state.start > duration(type) * 0.45) {
                state.start = now - vanillaSwing * 6;
                state.cut = 1 - state.cut;
            }
        }
        state.lastVanilla = vanillaSwing;
        float t = (float) ((now - state.start) / duration(type));
        return t >= 0 && t < 1 ? t : 0;
    }

    public static int cut(LivingEntity entity) {
        State state = STATES.get(entity);
        return state == null ? 0 : state.cut;
    }
}

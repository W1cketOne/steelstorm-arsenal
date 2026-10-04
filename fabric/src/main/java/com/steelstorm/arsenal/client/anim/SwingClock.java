package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Vanilla swings always last 6 ticks. This times each attack to the weapon instead (a greatsword
 * swing takes about as long as its cooldown, a dagger flick a fraction of that), walks through
 * the weapon's three-hit combo, and cross-fades into a new attack started mid-recovery so the
 * arms never snap.
 */
public final class SwingClock {
    private static final class State {
        float lastVanilla;
        double start = -1000;
        double lastEnd = -1000;
        float duration = 10;
        int step = -1;
        final float[] last = new float[AttackAnims.CHANNELS];
        final float[] from = new float[AttackAnims.CHANNELS];
        boolean blending;
    }

    private static final WeakHashMap<LivingEntity, State> STATES = new WeakHashMap<>();
    /** Ticks a fresh attack spends blending out of the previous one. */
    private static final float BLEND = 2.5F;

    private SwingClock() {
    }

    /** Fallback attack length for mobs, in ticks. */
    public static int duration(WeaponType type) {
        return switch (type) {
            case DUAL_DAGGERS -> 8;
            case KATANA -> 10;
            case LONGSWORD, SPEAR -> 12;
            case SCYTHE, BATTLEAXE -> 14;
            case GREATSWORD, WARHAMMER -> 16;
        };
    }

    private static float durationFor(LivingEntity entity, WeaponType type) {
        if (entity instanceof Player player) {
            // Matches the attack cooldown, so a fully charged hit always plays a full animation.
            // At least 11 ticks: quicker weapons chain hits by cross-fading rather than cramming a
            // whole cut into a couple of frames.
            return Mth.clamp(player.getCurrentItemAttackStrengthDelay() * 0.92F, 11, 22);
        }
        return duration(type);
    }

    private static State update(LivingEntity entity, float vanillaSwing, WeaponType type, float partialTick) {
        State state = STATES.computeIfAbsent(entity, e -> new State());
        double now = entity.tickCount + partialTick;
        // A new swing starts when vanilla's progress jumps back toward zero.
        if (vanillaSwing > 0 && (state.lastVanilla == 0 || vanillaSwing < state.lastVanilla - 0.05F)) {
            float elapsed = (float) (now - state.start);
            // Clicking again early in a cut doesn't restart it; past the strike it chains into the next hit.
            if (elapsed > state.duration * 0.55F) {
                boolean active = elapsed < state.duration;
                state.blending = active;
                if (active) {
                    System.arraycopy(state.last, 0, state.from, 0, AttackAnims.CHANNELS);
                }
                // Chains keep counting; a pause resets the combo to its first hit.
                boolean chained = now - Math.max(state.lastEnd, state.start + state.duration) < 12;
                state.step = chained ? (state.step + 1) % 3 : 0;
                int combo = com.steelstorm.arsenal.client.ClientCombatState.combo;
                if (entity == net.minecraft.client.Minecraft.getInstance().player && combo > 0) {
                    // Keep in step with the server's combo, so the heavy third hit is the finisher.
                    state.step = combo % 3;
                }
                state.duration = durationFor(entity, type);
                state.start = now - vanillaSwing * 6;
            }
        }
        state.lastVanilla = vanillaSwing;
        return state;
    }

    /** Progress 0..1 of the current attack, or 0 when not attacking. */
    public static float progress(LivingEntity entity, float vanillaSwing, WeaponType type, float partialTick) {
        State state = update(entity, vanillaSwing, type, partialTick);
        float t = (float) ((entity.tickCount + partialTick - state.start) / state.duration);
        if (t >= 1 && state.lastEnd < state.start) {
            state.lastEnd = state.start + state.duration;
        }
        return t >= 0 && t < 1 ? t : 0;
    }

    /** Samples the current attack's channels into `out`; returns false when not attacking. */
    public static boolean sample(LivingEntity entity, float vanillaSwing, WeaponType type, float partialTick, float[] out) {
        float t = progress(entity, vanillaSwing, type, partialTick);
        State state = STATES.get(entity);
        if (t <= 0 || state == null) {
            return false;
        }
        AttackAnims.get(type, Math.max(0, state.step)).sample(t, out);
        if (state.blending) {
            float b = (float) ((entity.tickCount + partialTick - state.start) / BLEND);
            if (b >= 1) {
                state.blending = false;
            } else {
                b = b * b * (3 - 2 * b);
                for (int i = 0; i < out.length; i++) {
                    out[i] = state.from[i] + (out[i] - state.from[i]) * b;
                }
            }
        }
        System.arraycopy(out, 0, state.last, 0, out.length);
        return true;
    }

    /** The attack currently playing (for trail timing). */
    public static AttackAnims.Attack attack(LivingEntity entity, WeaponType type) {
        State state = STATES.get(entity);
        return AttackAnims.get(type, state == null ? 0 : Math.max(0, state.step));
    }

    public static boolean finisher(LivingEntity entity) {
        State state = STATES.get(entity);
        return state != null && state.step == 2;
    }

    public static int cut(LivingEntity entity) {
        State state = STATES.get(entity);
        return state == null ? 0 : Math.max(0, state.step) % 2;
    }
}

package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.network.CombatSyncPayload;

/** What the HUD shows. Filled from server sync packets and counted down locally between them. */
public final class ClientCombatState {
    public static float stamina = 100;
    public static float maxStamina = 100;
    public static int specialCooldownLeft;
    public static int specialCooldownTotal = 1;
    public static int dodgeCooldownLeft;
    public static int combo;
    /** Ticks since the combo counter last changed, for its pop and fade animation. */
    public static int comboAge = 1000;
    /** Ticks the stamina bar keeps flashing after an action failed for lack of stamina. */
    public static int lowStaminaFlash;

    public static float shakeStrength;
    public static int shakeTicks;
    public static int shakeTotal = 1;

    static void apply(CombatSyncPayload payload) {
        if (payload.stamina() < stamina - 0.01F && payload.stamina() < 1) {
            lowStaminaFlash = 20;
        }
        stamina = payload.stamina();
        maxStamina = Math.max(1, payload.maxStamina());
        specialCooldownLeft = payload.specialCooldownLeft();
        specialCooldownTotal = Math.max(1, payload.specialCooldownTotal());
        dodgeCooldownLeft = payload.dodgeCooldownLeft();
        if (payload.combo() != combo) {
            comboAge = payload.combo() > combo ? 0 : comboAge;
            combo = payload.combo();
        }
    }

    static void shake(float strength, int ticks) {
        if (strength >= shakeStrength * (shakeTicks / (float) shakeTotal)) {
            shakeStrength = strength;
            shakeTicks = ticks;
            shakeTotal = Math.max(1, ticks);
        }
    }

    static void tick() {
        if (specialCooldownLeft > 0) {
            specialCooldownLeft--;
        }
        if (dodgeCooldownLeft > 0) {
            dodgeCooldownLeft--;
        }
        if (shakeTicks > 0) {
            shakeTicks--;
        }
        if (lowStaminaFlash > 0) {
            lowStaminaFlash--;
        }
        comboAge++;
    }

    public static void reset() {
        stamina = maxStamina;
        specialCooldownLeft = 0;
        dodgeCooldownLeft = 0;
        combo = 0;
        shakeTicks = 0;
    }

    private ClientCombatState() {
    }
}

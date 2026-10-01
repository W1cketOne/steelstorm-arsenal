package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.ability.AbilitySet;
import com.steelstorm.arsenal.network.CombatSyncPayload;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** What the HUD shows. Filled from server sync packets and counted down locally between them. */
public final class ClientCombatState {
    public static float stamina = 100;
    public static float maxStamina = 100;
    public static float ultimate;
    public static int dodgeCooldownLeft;
    public static int combo;
    /** The weapon set the cooldowns below belong to (as the server sees the held item). */
    public static String setId = "";
    public static final int[] cooldownLeft = new int[AbilitySet.SLOTS];
    public static final int[] cooldownTotal = new int[AbilitySet.SLOTS];
    /** Ticks since each slot came off cooldown, for the "ready" flash. */
    public static final int[] readyAge = {100, 100, 100, 100};
    /** Ticks since the ultimate became ready, for its glow. */
    public static int ultimateReadyAge = 1000;
    /** Ticks since the combo counter last changed, for its pop and fade animation. */
    public static int comboAge = 1000;
    /** Ticks the stamina bar keeps flashing after an action failed for lack of stamina. */
    public static int lowStaminaFlash;
    /** Ticks since a slot's key was pressed, for the press animation. */
    public static final int[] pressAge = {100, 100, 100, 100};

    public static float shakeStrength;
    public static int shakeTicks;
    public static int shakeTotal = 1;

    static void apply(CombatSyncPayload payload) {
        if (payload.stamina() < stamina - 0.01F && payload.stamina() < 1) {
            lowStaminaFlash = 20;
        }
        stamina = payload.stamina();
        maxStamina = Math.max(1, payload.maxStamina());
        if (payload.ultimate() >= 100 && ultimate < 100) {
            ultimateReadyAge = 0;
        }
        ultimate = payload.ultimate();
        dodgeCooldownLeft = payload.dodgeCooldownLeft();
        if (payload.combo() != combo) {
            comboAge = payload.combo() > combo ? 0 : comboAge;
            combo = payload.combo();
        }
        boolean sameSet = payload.setId().equals(setId);
        setId = payload.setId();
        for (int i = 0; i < AbilitySet.SLOTS; i++) {
            int left = i < payload.cooldownLeft().size() ? payload.cooldownLeft().get(i) : 0;
            if (!sameSet) {
                readyAge[i] = 100;
            }
            cooldownLeft[i] = left;
            cooldownTotal[i] = Math.max(1, i < payload.cooldownTotal().size() ? payload.cooldownTotal().get(i) : 1);
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
        for (int i = 0; i < AbilitySet.SLOTS; i++) {
            if (cooldownLeft[i] > 0 && --cooldownLeft[i] == 0) {
                readyAge[i] = 0;
                if (i < 3) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ABILITY_READY.get(), 1.0F, 0.35F));
                }
            }
            readyAge[i]++;
            pressAge[i]++;
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
        ultimateReadyAge++;
    }

    public static void reset() {
        stamina = maxStamina;
        ultimate = 0;
        dodgeCooldownLeft = 0;
        combo = 0;
        shakeTicks = 0;
        setId = "";
        for (int i = 0; i < AbilitySet.SLOTS; i++) {
            cooldownLeft[i] = 0;
            cooldownTotal[i] = 1;
        }
    }

    private ClientCombatState() {
    }
}

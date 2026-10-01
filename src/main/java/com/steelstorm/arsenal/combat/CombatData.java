package com.steelstorm.arsenal.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player combat state, stored as a NeoForge data attachment. Times are absolute game times
 * ({@code level.getGameTime()}), so cooldowns survive relogging.
 */
public class CombatData {
    public static final Codec<CombatData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("stamina").forGetter(d -> d.stamina),
            Codec.LONG.optionalFieldOf("special_cooldown_end", 0L).forGetter(d -> d.specialCooldownEnd),
            Codec.INT.optionalFieldOf("special_cooldown_total", 0).forGetter(d -> d.specialCooldownTotal),
            Codec.LONG.optionalFieldOf("dodge_cooldown_end", 0L).forGetter(d -> d.dodgeCooldownEnd)
    ).apply(i, CombatData::new));

    public float stamina;
    public long lastStaminaUse;
    public long specialCooldownEnd;
    public int specialCooldownTotal;
    public long dodgeCooldownEnd;
    public long invulnerableUntil;

    /** Consecutive successful hits; resets after {@link #COMBO_TIMEOUT} ticks without a hit. */
    public int combo;
    public long lastHitTime;
    /** Attack strength recorded the moment the player swung, before vanilla resets it. */
    public float lastAttackStrength = 1.0F;
    /** True for the hit currently being processed if it is a longsword-style combo finisher. */
    public boolean finisherPending;
    /** True for the hit currently being processed if it is a heavy (sneak) attack. */
    public boolean heavyPending;

    // What the client was last told, to avoid re-sending unchanged values every tick.
    public int syncedStamina = -1;
    public int syncedCombo = -1;
    public long syncedSpecialEnd = -1;
    public long syncedDodgeEnd = -1;

    public static final int COMBO_TIMEOUT = 50;

    public CombatData() {
        this(100.0F, 0L, 0, 0L);
    }

    private CombatData(float stamina, long specialCooldownEnd, int specialCooldownTotal, long dodgeCooldownEnd) {
        this.stamina = stamina;
        this.specialCooldownEnd = specialCooldownEnd;
        this.specialCooldownTotal = specialCooldownTotal;
        this.dodgeCooldownEnd = dodgeCooldownEnd;
    }

    public int comboAt(long now) {
        return now - lastHitTime <= COMBO_TIMEOUT ? combo : 0;
    }

    public void markDirty() {
        syncedStamina = -1;
    }
}

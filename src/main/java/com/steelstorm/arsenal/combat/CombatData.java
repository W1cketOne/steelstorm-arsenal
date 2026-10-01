package com.steelstorm.arsenal.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Per-player combat state, stored as a NeoForge data attachment. Times are absolute game times
 * ({@code level.getGameTime()}), so cooldowns survive relogging.
 */
public class CombatData {
    public static final Codec<CombatData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("stamina").forGetter(d -> d.stamina),
            Codec.FLOAT.optionalFieldOf("ultimate", 0.0F).forGetter(d -> d.ultimate),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(d -> d.cooldownEnd),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("cooldown_totals", Map.of()).forGetter(d -> d.cooldownTotal),
            Codec.LONG.optionalFieldOf("dodge_cooldown_end", 0L).forGetter(d -> d.dodgeCooldownEnd)
    ).apply(i, CombatData::new));

    public static final int COMBO_TIMEOUT = 50;
    public static final float ULTIMATE_MAX = 100.0F;

    public float stamina;
    public long lastStaminaUse;
    public long dodgeCooldownEnd;
    public long invulnerableUntil;

    /** Ability cooldowns keyed by ability id. */
    public final Map<String, Long> cooldownEnd = new HashMap<>();
    public final Map<String, Integer> cooldownTotal = new HashMap<>();
    /** Ultimate meter, 0..100. Filled by dealing damage, parrying and taking hits. */
    public float ultimate;
    /** While an ultimate is still playing out, its own damage doesn't refill the meter. */
    public long ultimateLockUntil;

    /** Consecutive successful hits; resets after {@link #COMBO_TIMEOUT} ticks without a hit. */
    public int combo;
    public long lastHitTime;
    /** Attack strength recorded the moment the player swung, before vanilla resets it. */
    public float lastAttackStrength = 1.0F;
    public boolean finisherPending;
    public boolean heavyPending;
    public boolean critPending;

    // Ability states
    /** Shadowstep: the next melee hit before this time is a backstab critical. */
    public long backstabUntil;
    /** Death Blossom / similar: a number of empowered hits with a damage multiplier. */
    public int empoweredHits;
    public float empoweredMultiplier = 1.0F;
    /** Venom Coat: knives thrown while this is above zero poison their target. */
    public int venomKnives;
    /** No fall damage until this time (leaps, dives). */
    public long noFallUntil;
    /** Greatsword Titan's Guard: until when damage is soaked up, and how much has been. */
    public long titanGuardUntil;
    public float titanStored;
    /** Last cosmetic swing broadcast, to rate-limit them. */
    public long lastSwingFx;

    // What the client was last told, to avoid re-sending unchanged values every tick.
    public int syncedStamina = -1;
    public int syncedCombo = -1;
    public int syncedUltimate = -1;
    public long syncedDodgeEnd = -1;
    @Nullable
    public String syncedSet;
    public long syncedCooldownHash = -1;

    public CombatData() {
        this(100.0F, 0.0F, Map.of(), Map.of(), 0L);
    }

    private CombatData(float stamina, float ultimate, Map<String, Long> cooldowns, Map<String, Integer> totals, long dodgeCooldownEnd) {
        this.stamina = stamina;
        this.ultimate = ultimate;
        this.cooldownEnd.putAll(cooldowns);
        this.cooldownTotal.putAll(totals);
        this.dodgeCooldownEnd = dodgeCooldownEnd;
    }

    public int comboAt(long now) {
        return now - lastHitTime <= COMBO_TIMEOUT ? combo : 0;
    }

    public long cooldownLeft(String abilityId, long now) {
        return Math.max(0, cooldownEnd.getOrDefault(abilityId, 0L) - now);
    }

    public void forceSync() {
        syncedStamina = -1;
        syncedSet = null;
    }
}

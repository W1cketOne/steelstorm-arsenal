package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/** Per-player transient combat state. Lives in a data attachment; the server copy is authoritative. */
public class CombatState {
    /** Ticks until stamina starts regenerating. */
    public int regenDelay;
    public int specialCooldown;
    public int specialCooldownMax;
    public int dodgeCooldown;
    /** Remaining dodge invulnerability ticks. */
    public int iFrames;

    /** Consecutive hits landed; resets after {@link #COMBO_TIMEOUT} ticks without a hit or when hurt. */
    public int combo;
    public int comboTimer;
    public static final int COMBO_TIMEOUT = 50;

    /** Set by the attack event for the swing currently being resolved. */
    public boolean heavyPending;
    public boolean critPending;
    public int pendingTarget = -1;
    /** True while a special move is dealing its damage, so per-swing logic is skipped. */
    public boolean inSpecial;

    public long guardStart = -1000;
    public long lastGuardEnd = -1000;

    // Multi-tick special moves.
    public int flurryTarget = -1;
    public int flurryHits;
    public int flurryTimer;
    public int whirlwindTicks;
    public int quakeTicks;
    public int vortexTicks;

    /** Set whenever something the HUD shows changed. */
    public boolean dirty = true;
    public float lastSentStamina = -1;

    public static CombatState of(Player player) {
        return player.getData(ModAttachments.COMBAT);
    }

    public void resetCombo() {
        if (combo != 0) {
            combo = 0;
            dirty = true;
        }
        comboTimer = 0;
    }
}

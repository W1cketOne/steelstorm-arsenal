package com.steelstorm.arsenal.anim;

import java.util.HashMap;
import java.util.Map;

/**
 * Whole-body animations a player (or boss) plays: one for each kind of ability, plus the dodge
 * roll. The server picks one and tells every nearby client, which animates the arms, body and
 * held weapon for {@link #duration} ticks.
 */
public enum CastPose {
    NONE(0),
    /** Weapon raised overhead, gathering power (ultimates, buffs). */
    RAISE(16),
    /** Lunge with the weapon pointed forward (dashes). */
    THRUST(8),
    /** The whole body spins (whirlwinds). */
    SPIN(14),
    /** Weapon brought up, then slammed into the ground. */
    SLAM(12),
    /** Arm drawn back, then flung forward. */
    THROW(9),
    /** One wide horizontal sweep. */
    SWEEP(10),
    /** Weapon held high while airborne. */
    LEAP(14),
    /** A guarded, ready stance. */
    STANCE(20),
    /** Arms thrown wide, head back. */
    ROAR(16),
    /** A burst of rapid stabs. */
    FLURRY(12),
    /** A lightning-fast sideways draw cut. */
    DRAW(8),
    /** A cut from low to high. */
    RISING(9),
    /** The dodge roll: a full somersault. */
    DODGE(10);

    public final int duration;

    CastPose(int duration) {
        this.duration = duration;
    }

    private static final Map<String, CastPose> BY_ABILITY = new HashMap<>();

    private static void map(CastPose pose, String... ids) {
        for (String id : ids) {
            BY_ABILITY.put(id, pose);
        }
    }

    static {
        map(RISING, "longsword_rising_slash");
        map(STANCE, "longsword_riposte", "throwing_knife_venom", "greatsword_titans_guard");
        map(THRUST, "longsword_blade_dash", "dual_daggers_shadowstep", "spear_impale");
        map(SLAM, "warhammer_earthquake", "warhammer_stone_prison", "greatsword_colossus_strike", "earthshaker_tectonic_spiral");
        map(SWEEP, "greatsword_crescent_wave", "spear_sweeping_arc");
        map(SPIN, "dual_daggers_fan_of_knives", "scythe_reap", "battleaxe_whirlwind");
        map(DRAW, "katana_flash_step", "katana_iaido", "katana_wind_scar");
        map(FLURRY, "dual_daggers_flurry");
        map(LEAP, "spear_vault_leap", "spear_dragon_dive", "warhammer_cataclysm", "kingsbane_regicide");
        map(THROW, "warhammer_hammer_throw", "greatsword_sword_sanctum", "battleaxe_chain_hook", "scythe_deaths_crescent", "chakram_sawblade", "chakram_twin_throw",
                "throwing_knife_volley", "throwing_knife_blink");
        map(ROAR, "battleaxe_war_cry", "battleaxe_berserker_rage", "bloodfang_hemorrhage");
    }

    /** The pose for an ability; anything not listed (mostly ultimates) raises the weapon. */
    public static CastPose forAbility(String abilityId) {
        return BY_ABILITY.getOrDefault(abilityId, RAISE);
    }

    public static CastPose byId(int id) {
        CastPose[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}

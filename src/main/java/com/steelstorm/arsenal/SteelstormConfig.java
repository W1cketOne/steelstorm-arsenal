package com.steelstorm.arsenal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (gameplay + world generation) settings. Values are read on the logical server,
 * which is authoritative for every combat number; clients get what they need over the network.
 */
public final class SteelstormConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- Stamina ----
    static {
        BUILDER.comment("Stamina: spent by dodging, heavy attacks, guarding and special moves").push("stamina");
    }

    public static final ModConfigSpec.DoubleValue MAX_STAMINA = BUILDER
            .comment("Maximum stamina.")
            .defineInRange("maxStamina", 100.0, 10.0, 1000.0);
    public static final ModConfigSpec.IntValue REGEN_DELAY = BUILDER
            .comment("Ticks without spending stamina before it starts to regenerate (20 ticks = 1 second).")
            .defineInRange("regenDelayTicks", 20, 0, 200);
    public static final ModConfigSpec.DoubleValue REGEN_PER_TICK = BUILDER
            .comment("Stamina regained per tick while regenerating.")
            .defineInRange("regenPerTick", 1.25, 0.05, 100.0);
    public static final ModConfigSpec.DoubleValue DODGE_COST = BUILDER
            .comment("Stamina cost of a dodge roll.")
            .defineInRange("dodgeCost", 20.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue HEAVY_ATTACK_COST = BUILDER
            .comment("Stamina cost of a heavy attack (sneak + attack).")
            .defineInRange("heavyAttackCost", 15.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue GUARD_COST_PER_DAMAGE = BUILDER
            .comment("Stamina spent per point of damage absorbed while guarding.")
            .defineInRange("guardCostPerDamage", 2.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue SPECIAL_COST_MULTIPLIER = BUILDER
            .comment("Multiplier for every special move's stamina cost.")
            .defineInRange("specialCostMultiplier", 1.0, 0.0, 10.0);

    static {
        BUILDER.pop();
        BUILDER.comment("Combat tuning").push("combat");
    }

    public static final ModConfigSpec.IntValue DODGE_COOLDOWN = BUILDER
            .comment("Ticks between dodge rolls.")
            .defineInRange("dodgeCooldownTicks", 20, 0, 200);
    public static final ModConfigSpec.IntValue DODGE_IFRAMES = BUILDER
            .comment("Ticks of invulnerability at the start of a dodge roll.")
            .defineInRange("dodgeInvulnerabilityTicks", 8, 0, 40);
    public static final ModConfigSpec.IntValue PARRY_WINDOW = BUILDER
            .comment("A hit landing within this many ticks of raising your guard is a perfect parry.")
            .defineInRange("perfectParryWindowTicks", 5, 0, 40);
    public static final ModConfigSpec.DoubleValue GUARD_REDUCTION = BUILDER
            .comment("Fraction of damage blocked by a normal (non-perfect) guard.")
            .defineInRange("guardDamageReduction", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue HEAVY_ATTACK_MULTIPLIER = BUILDER
            .comment("Damage multiplier for heavy attacks.")
            .defineInRange("heavyAttackMultiplier", 1.5, 1.0, 10.0);
    public static final ModConfigSpec.DoubleValue WEAPON_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier applied to all melee damage dealt with Steelstorm weapons.")
            .defineInRange("weaponDamageMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue SPECIAL_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier applied to special move damage.")
            .defineInRange("specialDamageMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue SPECIAL_COOLDOWN_MULTIPLIER = BUILDER
            .comment("Multiplier for every special move's cooldown.")
            .defineInRange("specialCooldownMultiplier", 1.0, 0.0, 10.0);

    // ---- World ----
    static {
        BUILDER.pop();
        BUILDER.comment("World generation").push("structures");
    }

    public static final ModConfigSpec.BooleanValue STARTER_OUTPOST = BUILDER
            .comment("Place the Warrior's Outpost next to the spawn point when a new world is created.")
            .define("starterOutpost", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SteelstormConfig() {
    }

    /** Per-player visual preferences. */
    public static final class Client {
        private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue SCREEN_SHAKE = CLIENT
                .comment("Shake the camera on heavy hits, slams and parries.")
                .define("screenShake", true);
        public static final ModConfigSpec.DoubleValue SCREEN_SHAKE_INTENSITY = CLIENT
                .comment("Strength of the screen shake.")
                .defineInRange("screenShakeIntensity", 1.0, 0.0, 3.0);
        public static final ModConfigSpec.BooleanValue SHOW_HUD = CLIENT
                .comment("Show the stamina bar, special cooldown and combo counter.")
                .define("showCombatHud", true);

        public static final ModConfigSpec SPEC = CLIENT.build();

        private Client() {
        }
    }
}

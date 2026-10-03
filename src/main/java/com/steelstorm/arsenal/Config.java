package com.steelstorm.arsenal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (server-authoritative) settings live in {@link #SPEC}; purely visual settings live in
 * {@link #CLIENT_SPEC} so each player can change them without affecting the server.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Stamina
    public static final ModConfigSpec.DoubleValue MAX_STAMINA;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN_PER_TICK;
    public static final ModConfigSpec.IntValue STAMINA_REGEN_DELAY;
    public static final ModConfigSpec.DoubleValue DODGE_COST;
    public static final ModConfigSpec.DoubleValue HEAVY_ATTACK_COST;
    public static final ModConfigSpec.DoubleValue GUARD_COST_PER_DAMAGE;
    public static final ModConfigSpec.DoubleValue SPECIAL_COST_MULTIPLIER;

    // Combat
    public static final ModConfigSpec.DoubleValue WEAPON_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEAVY_ATTACK_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPECIAL_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue GUARD_DAMAGE_REDUCTION;
    public static final ModConfigSpec.IntValue PERFECT_PARRY_TICKS;
    public static final ModConfigSpec.IntValue DODGE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue DODGE_INVULNERABILITY_TICKS;
    public static final ModConfigSpec.DoubleValue SPECIAL_COOLDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ULTIMATE_CHARGE_MULTIPLIER;

    // World
    public static final ModConfigSpec.DoubleValue ARMORY_CHANCE;
    public static final ModConfigSpec.DoubleValue BANDIT_CAMP_CHANCE;
    public static final ModConfigSpec.DoubleValue COLOSSEUM_CHANCE;
    public static final ModConfigSpec.DoubleValue PROVING_GROUNDS_CHANCE;
    public static final ModConfigSpec.DoubleValue KNIGHTS_CRYPT_CHANCE;
    public static final ModConfigSpec.DoubleValue STORM_SHRINE_CHANCE;
    public static final ModConfigSpec.DoubleValue BLACKSMITH_CHANCE;
    public static final ModConfigSpec.DoubleValue WATCHTOWER_CHANCE;
    public static final ModConfigSpec.DoubleValue STORMSTEEL_MINE_CHANCE;
    public static final ModConfigSpec.DoubleValue BOSS_LAIR_CHANCE;
    public static final ModConfigSpec.BooleanValue STARTER_OUTPOST;

    static {
        BUILDER.push("stamina");
        MAX_STAMINA = BUILDER.comment("Maximum stamina").defineInRange("maxStamina", 100.0, 10.0, 1000.0);
        STAMINA_REGEN_PER_TICK = BUILDER.comment("Stamina regenerated per tick once regeneration starts (20 ticks = 1 second)")
                .defineInRange("regenPerTick", 1.0, 0.0, 50.0);
        STAMINA_REGEN_DELAY = BUILDER.comment("Ticks without spending stamina before it starts regenerating")
                .defineInRange("regenDelayTicks", 20, 0, 200);
        DODGE_COST = BUILDER.comment("Stamina cost of a dodge roll").defineInRange("dodgeCost", 20.0, 0.0, 1000.0);
        HEAVY_ATTACK_COST = BUILDER.comment("Stamina cost of a heavy (sneak) attack").defineInRange("heavyAttackCost", 15.0, 0.0, 1000.0);
        GUARD_COST_PER_DAMAGE = BUILDER.comment("Stamina spent per point of damage absorbed while guarding")
                .defineInRange("guardCostPerDamage", 2.0, 0.0, 100.0);
        SPECIAL_COST_MULTIPLIER = BUILDER.comment("Multiplier on every special ability's stamina cost")
                .defineInRange("specialCostMultiplier", 1.0, 0.0, 10.0);
        BUILDER.pop();

        BUILDER.push("combat");
        WEAPON_DAMAGE_MULTIPLIER = BUILDER.comment("Multiplier on all melee damage dealt with Steelstorm weapons")
                .defineInRange("weaponDamageMultiplier", 1.0, 0.0, 10.0);
        HEAVY_ATTACK_MULTIPLIER = BUILDER.comment("Damage multiplier of a heavy attack").defineInRange("heavyAttackMultiplier", 1.5, 1.0, 10.0);
        SPECIAL_DAMAGE_MULTIPLIER = BUILDER.comment("Multiplier on damage dealt by special abilities")
                .defineInRange("specialDamageMultiplier", 1.0, 0.0, 10.0);
        GUARD_DAMAGE_REDUCTION = BUILDER.comment("Fraction of damage blocked while guarding outside the perfect parry window")
                .defineInRange("guardDamageReduction", 0.5, 0.0, 1.0);
        PERFECT_PARRY_TICKS = BUILDER.comment("How many ticks after raising your guard count as a perfect parry")
                .defineInRange("perfectParryTicks", 5, 0, 40);
        DODGE_COOLDOWN_TICKS = BUILDER.comment("Cooldown between dodge rolls, in ticks").defineInRange("dodgeCooldownTicks", 20, 0, 200);
        DODGE_INVULNERABILITY_TICKS = BUILDER.comment("Invulnerability window at the start of a dodge roll, in ticks")
                .defineInRange("dodgeInvulnerabilityTicks", 8, 0, 40);
        SPECIAL_COOLDOWN_MULTIPLIER = BUILDER.comment("Multiplier on every ability's cooldown")
                .defineInRange("specialCooldownMultiplier", 1.0, 0.0, 10.0);
        ULTIMATE_CHARGE_MULTIPLIER = BUILDER.comment("How fast the ultimate meter fills from dealing and taking damage (1.0 = normal)")
                .defineInRange("ultimateChargeMultiplier", 1.0, 0.0, 10.0);
        BUILDER.pop();

        BUILDER.comment("Structures only generate in newly explored chunks.").push("structures");
        ARMORY_CHANCE = BUILDER.comment("Chance (0-1) that each possible Abandoned Armory spot actually gets one. 0 disables them.")
                .defineInRange("abandonedArmoryChance", 0.75, 0.0, 1.0);
        BANDIT_CAMP_CHANCE = BUILDER.comment("Chance (0-1) that each possible Bandit Camp spot actually gets one. 0 disables them.")
                .defineInRange("banditCampChance", 0.6, 0.0, 1.0);
        COLOSSEUM_CHANCE = BUILDER.comment("Chance (0-1) that each possible Ruined Colosseum spot actually gets one. 0 disables them.")
                .defineInRange("ruinedColosseumChance", 0.7, 0.0, 1.0);
        PROVING_GROUNDS_CHANCE = BUILDER.comment("Chance (0-1) that a possible Proving Grounds location actually gets one")
                .defineInRange("provingGroundsChance", 0.7, 0.0, 1.0);
        KNIGHTS_CRYPT_CHANCE = BUILDER.comment("Chance (0-1) that a possible Knight's Crypt location actually gets one")
                .defineInRange("knightsCryptChance", 0.7, 0.0, 1.0);
        STORM_SHRINE_CHANCE = BUILDER.comment("Chance (0-1) that a possible Storm Shrine location actually gets one")
                .defineInRange("stormShrineChance", 0.8, 0.0, 1.0);
        BLACKSMITH_CHANCE = BUILDER.comment("Chance (0-1) that a possible Blacksmith's Forge location actually gets one")
                .defineInRange("blacksmithChance", 0.75, 0.0, 1.0);
        WATCHTOWER_CHANCE = BUILDER.comment("Chance (0-1) that a possible Watchtower location actually gets one")
                .defineInRange("watchtowerChance", 0.7, 0.0, 1.0);
        STORMSTEEL_MINE_CHANCE = BUILDER.comment("Chance (0-1) that a possible Stormsteel Mine location actually gets one")
                .defineInRange("stormsteelMineChance", 0.7, 0.0, 1.0);
        BOSS_LAIR_CHANCE = BUILDER.comment("Chance (0-1) that a possible boss lair (Colossus Forge, Moonlit Sanctum) location actually gets one")
                .defineInRange("bossLairChance", 0.8, 0.0, 1.0);
        STARTER_OUTPOST = BUILDER.comment("Build the Warrior's Outpost next to spawn when a new world is created")
                .define("starterOutpost", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.DoubleValue SCREEN_SHAKE_STRENGTH;
    public static final ModConfigSpec.BooleanValue SHOW_COMBO_COUNTER;
    public static final ModConfigSpec.BooleanValue SHOW_DAMAGE_NUMBERS;

    static {
        CLIENT_BUILDER.push("feedback");
        SCREEN_SHAKE = CLIENT_BUILDER.comment("Shake the camera slightly on heavy hits, parries and specials")
                .define("screenShake", true);
        SCREEN_SHAKE_STRENGTH = CLIENT_BUILDER.comment("Screen shake strength").defineInRange("screenShakeStrength", 1.0, 0.0, 3.0);
        SHOW_COMBO_COUNTER = CLIENT_BUILDER.comment("Show the combo counter next to the crosshair").define("showComboCounter", true);
        SHOW_DAMAGE_NUMBERS = CLIENT_BUILDER.comment("Show damage numbers popping off enemies you hit").define("showDamageNumbers", true);
        CLIENT_BUILDER.pop();
    }

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private Config() {
    }
}

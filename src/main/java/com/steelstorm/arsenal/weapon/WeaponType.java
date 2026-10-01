package com.steelstorm.arsenal.weapon;

/**
 * The shared weapon type system. Every tiered melee weapon is one {@link WeaponType} combined
 * with one {@link WeaponTier}; the registry loops over both instead of having a class per weapon.
 *
 * <p>Stats are relative to a vanilla sword of the same tier: a vanilla sword adds
 * {@code 3 + tierBonus} attack damage and has 1.6 attack speed.</p>
 */
public enum WeaponType {
    LONGSWORD("longsword", "Longsword", 0.0F, 1.6F, 0.5, 25, 120),
    GREATSWORD("greatsword", "Greatsword", 3.0F, 0.8F, 1.0, 35, 160),
    KATANA("katana", "Katana", 1.0F, 1.8F, 0.5, 25, 100),
    DUAL_DAGGERS("dual_daggers", "Dual Daggers", -2.0F, 3.0F, 0.0, 30, 140),
    SPEAR("spear", "Spear", 0.0F, 1.2F, 2.0, 25, 120),
    WARHAMMER("warhammer", "Warhammer", 4.0F, 0.7F, 0.0, 40, 180),
    SCYTHE("scythe", "Scythe", 2.0F, 1.0F, 1.0, 35, 160),
    BATTLEAXE("battleaxe", "Battleaxe", 3.0F, 0.9F, 0.0, 35, 160);

    /** Vanilla sword base: the item adds 3 damage on top of the player's base 1. */
    public static final float SWORD_BASE_DAMAGE = 3.0F;
    /** Player base attack speed that item modifiers are added to. */
    public static final float PLAYER_BASE_SPEED = 4.0F;

    private final String id;
    private final String displayName;
    private final float damageDelta;
    private final float attackSpeed;
    private final double reachBonus;
    private final int specialStaminaCost;
    private final int specialCooldownTicks;

    WeaponType(String id, String displayName, float damageDelta, float attackSpeed, double reachBonus,
               int specialStaminaCost, int specialCooldownTicks) {
        this.id = id;
        this.displayName = displayName;
        this.damageDelta = damageDelta;
        this.attackSpeed = attackSpeed;
        this.reachBonus = reachBonus;
        this.specialStaminaCost = specialStaminaCost;
        this.specialCooldownTicks = specialCooldownTicks;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public float damageDelta() {
        return damageDelta;
    }

    public float attackSpeed() {
        return attackSpeed;
    }

    public double reachBonus() {
        return reachBonus;
    }

    public int specialStaminaCost() {
        return specialStaminaCost;
    }

    public int specialCooldownTicks() {
        return specialCooldownTicks;
    }

    public String passiveKey() {
        return "weapon.steelstorm." + id + ".passive";
    }

    public String specialNameKey() {
        return "weapon.steelstorm." + id + ".special";
    }

    public String specialDescKey() {
        return "weapon.steelstorm." + id + ".special.desc";
    }

    public String itemId(WeaponTier tier) {
        return tier.prefix() + "_" + id;
    }
}

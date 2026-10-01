package com.steelstorm.arsenal.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Every melee weapon archetype. Stats are relative to a vanilla sword of the same tier
 * (vanilla sword = +3 damage on top of the tier bonus, 1.6 attack speed, 3.0 reach).
 * Items are registered for every type x {@link WeaponTier} in a loop, so adding a type here
 * is all it takes to get the full tier lineup, recipes, models and lang.
 */
public enum WeaponType {
    //            id              dmg   speed  reach  sweep  sweepBox  large  pattern
    LONGSWORD("longsword", 0.0f, 1.6f, 0.5f, true, 1.0, false, " M ", " M ", "MSM"),
    GREATSWORD("greatsword", 3.0f, 0.8f, 1.0f, true, 2.25, true, " MM", "MMM", "SM "),
    KATANA("katana", 1.0f, 1.8f, 0.5f, true, 1.0, false, "  M", " M ", "S  "),
    DUAL_DAGGERS("dual_daggers", -2.0f, 3.0f, 0.0f, false, 0.0, false, "M M", "S S"),
    SPEAR("spear", 0.0f, 1.2f, 2.0f, false, 0.0, true, "  M", " S ", "S  ");

    private final String id;
    private final float damageBonus;
    private final float attackSpeed;
    private final float reachBonus;
    private final boolean sweeps;
    private final double sweepInflate;
    private final boolean large;
    private final String[] pattern;

    WeaponType(String id, float damageBonus, float attackSpeed, float reachBonus, boolean sweeps, double sweepInflate,
               boolean large, String... pattern) {
        this.id = id;
        this.damageBonus = damageBonus;
        this.attackSpeed = attackSpeed;
        this.reachBonus = reachBonus;
        this.sweeps = sweeps;
        this.sweepInflate = sweepInflate;
        this.large = large;
        this.pattern = pattern;
    }

    public String id() {
        return id;
    }

    public float damageBonus() {
        return damageBonus;
    }

    public float attackSpeed() {
        return attackSpeed;
    }

    public float reachBonus() {
        return reachBonus;
    }

    /** Whether a fully charged grounded hit performs a sweep attack. */
    public boolean sweeps() {
        return sweeps;
    }

    /** Horizontal inflation of the sweep hit box around the target (vanilla is 1.0). */
    public double sweepInflate() {
        return sweepInflate;
    }

    /** Large weapons use a bigger held model. */
    public boolean large() {
        return large;
    }

    public boolean throwable() {
        return this == SPEAR;
    }

    public String[] pattern() {
        return pattern;
    }

    public String translationKey() {
        return "weapon_type.steelstorm." + id;
    }

    public MutableComponent displayName() {
        return Component.translatable(translationKey());
    }

    public MutableComponent passiveName() {
        return Component.translatable("weapon.steelstorm." + id + ".passive");
    }

    public MutableComponent passiveDescription() {
        return Component.translatable("weapon.steelstorm." + id + ".passive.desc").withStyle(ChatFormatting.GRAY);
    }
}

package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.resources.ResourceLocation;

/**
 * One weapon ability. Abilities are plain data plus an action; the {@link AbilityManager} checks
 * stamina, cooldown and ultimate charge on the server before running the action.
 */
public final class Ability {
    @FunctionalInterface
    public interface Action {
        /** Returns false if nothing happened (no stamina or cooldown is spent). */
        boolean run(AbilityContext ctx);
    }

    private final String id;
    private final String name;
    private final String description;
    private final int staminaCost;
    private final int cooldown;
    private final boolean ultimate;
    private final Action action;
    private final int charges;

    private Ability(String id, String name, String description, int staminaCost, int cooldown, boolean ultimate, Action action) {
        this(id, name, description, staminaCost, cooldown, ultimate, action, 1);
    }

    private Ability(String id, String name, String description, int staminaCost, int cooldown, boolean ultimate, Action action,
                    int charges) {
        this.charges = charges;
        this.id = id;
        this.name = name;
        this.description = description;
        this.staminaCost = staminaCost;
        this.cooldown = cooldown;
        this.ultimate = ultimate;
        this.action = action;
    }

    public static Ability of(String id, String name, String description, int staminaCost, int cooldownTicks, Action action) {
        return new Ability(id, name, description, staminaCost, cooldownTicks, false, action);
    }

    public static Ability ultimate(String id, String name, String description, int cooldownTicks, Action action) {
        return new Ability(id, name, description, 0, cooldownTicks, true, action);
    }

    /**
     * A copy that can be used `charges` times in a row. Each use spends a charge; charges come back
     * one at a time, each taking the full cooldown.
     */
    public Ability withCharges(int charges) {
        return new Ability(id, name, description, staminaCost, cooldown, ultimate, action, charges);
    }

    public int charges() {
        return charges;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public int staminaCost() {
        return staminaCost;
    }

    public int cooldown() {
        return cooldown;
    }

    public boolean isUltimate() {
        return ultimate;
    }

    public boolean run(AbilityContext ctx) {
        return action.run(ctx);
    }

    public String nameKey() {
        return "ability.steelstorm." + id;
    }

    public String descKey() {
        return "ability.steelstorm." + id + ".desc";
    }

    public ResourceLocation icon() {
        return SteelstormArsenal.id("textures/gui/ability/" + id + ".png");
    }
}

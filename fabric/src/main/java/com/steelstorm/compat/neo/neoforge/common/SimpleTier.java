package com.steelstorm.compat.neo.neoforge.common;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public final class SimpleTier implements Tier {
    private final TagKey<Block> incorrect;
    private final int uses, enchant;
    private final float speed, damage;
    private final Supplier<Ingredient> repair;
    public SimpleTier(TagKey<Block> incorrect, int uses, float speed, float damage, int enchant, Supplier<Ingredient> repair) {
        this.incorrect = incorrect; this.uses = uses; this.speed = speed; this.damage = damage; this.enchant = enchant;
        this.repair = Suppliers.memoize(repair::get);
    }
    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrect; }
    @Override public int getEnchantmentValue() { return enchant; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
}

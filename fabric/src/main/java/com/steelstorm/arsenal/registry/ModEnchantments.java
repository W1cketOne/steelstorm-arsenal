package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Enchantments are data-driven JSON in 1.21.1 (data/steelstorm/enchantment). Their combat effects
 * are applied in code by looking up the enchantment level on the weapon.
 */
public final class ModEnchantments {
    /** Chance to cause Bleed on hit. */
    public static final ResourceKey<Enchantment> LACERATE = key("lacerate");
    /** Bonus damage against enemies below 35% health. */
    public static final ResourceKey<Enchantment> EXECUTIONER = key("executioner");
    /** Restores stamina when you land a hit. */
    public static final ResourceKey<Enchantment> MOMENTUM = key("momentum");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, SteelstormArsenal.id(name));
    }

    public static int level(LivingEntity holder, ItemStack stack, ResourceKey<Enchantment> key) {
        return holder.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holderRef -> EnchantmentHelper.getItemEnchantmentLevel(holderRef, stack))
                .orElse(0);
    }

    private ModEnchantments() {
    }
}

package com.steelstorm.arsenal.client.render;

import com.steelstorm.arsenal.weapon.ThrowingKnifeItem;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.world.item.ItemStack;

/**
 * Sizes of the 3D weapon models (see tools/gen_models.py), measured from the centre the item
 * renderer rotates around. Used to stand summoned weapons on their tips.
 */
public final class WeaponModels {
    /** Distance from the model centre to the tip, in blocks at scale 1. */
    public static float tip(ItemStack stack) {
        if (stack.getItem() instanceof WeaponItem weapon) {
            return switch (weapon.type()) {
                case LONGSWORD, KATANA -> 1.41F;
                case GREATSWORD -> 1.475F;
                case DUAL_DAGGERS -> 0.61F;
                case SPEAR -> 1.5F;
                case WARHAMMER -> 1.16F;
                case SCYTHE -> 1.19F;
                case BATTLEAXE -> 1.125F;
            };
        }
        return stack.getItem() instanceof ThrowingKnifeItem ? 0.16F : 0.5F;
    }

    /** Distance from the model centre to the far (pommel) end, in blocks at scale 1. */
    public static float tail(ItemStack stack) {
        if (stack.getItem() instanceof WeaponItem weapon) {
            return switch (weapon.type()) {
                case LONGSWORD, DUAL_DAGGERS -> 0.59F;
                case KATANA -> 0.69F;
                case GREATSWORD -> 0.78F;
                case SPEAR -> 1.44F;
                case WARHAMMER -> 1.22F;
                case SCYTHE -> 1.375F;
                case BATTLEAXE -> 1.09F;
            };
        }
        return stack.getItem() instanceof ThrowingKnifeItem ? 0.58F : 0.5F;
    }

    private WeaponModels() {
    }
}

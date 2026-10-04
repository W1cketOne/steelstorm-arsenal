package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.fx.Fx;
import net.minecraft.world.item.ItemStack;

/** Colours and sizes for a weapon's slash trails and ability effects. Shared by client and server. */
public final class WeaponLooks {
    /** The colour of a weapon's trails: its element for legendaries, its rune if it has one, otherwise its metal. */
    public static int trailColor(ItemStack stack) {
        if (stack.getItem() instanceof LegendaryWeaponItem legendary) {
            return legendaryColor(legendary.legendary());
        }
        Rune rune = stack.get(com.steelstorm.arsenal.registry.ModDataComponents.RUNE.get());
        if (rune != null) {
            return rune.color();
        }
        if (stack.getItem() instanceof WeaponItem weapon && weapon.weaponTier() != null) {
            return switch (weapon.weaponTier()) {
                case STONE -> 0xC8C4BC;
                case IRON -> Fx.STEEL;
                case GOLD -> Fx.GOLD;
                case DIAMOND -> 0x8FF7EE;
                case NETHERITE -> 0xB9A2C8;
                case STORMSTEEL -> Fx.STORM;
            };
        }
        return Fx.STEEL;
    }

    /** The colour of a weapon's ability effects. Legendaries use their element. */
    public static int abilityColor(ItemStack stack) {
        if (stack.getItem() instanceof LegendaryWeaponItem legendary) {
            return legendaryColor(legendary.legendary());
        }
        if (stack.getItem() instanceof WeaponItem weapon) {
            return weapon.isStormsteel() ? Fx.STORM : typeColor(weapon.type());
        }
        if (stack.getItem() instanceof ChakramItem) {
            return 0x67E8F9;
        }
        return 0xC7D2FE;
    }

    public static int typeColor(WeaponType type) {
        return switch (type) {
            case LONGSWORD -> Fx.STEEL;
            case GREATSWORD -> 0xFFB347;
            case KATANA -> 0xFF7FB0;
            case DUAL_DAGGERS -> 0xA78BFA;
            case SPEAR -> 0x6EE7B7;
            case WARHAMMER -> 0xFF9A3C;
            case SCYTHE -> 0xC084FC;
            case BATTLEAXE -> 0xFF6B6B;
        };
    }

    public static int legendaryColor(LegendaryWeaponItem.Legendary legendary) {
        return switch (legendary) {
            case TEMPEST_EDGE -> Fx.STORM;
            case RIMECLEAVER -> Fx.FROST;
            case VOIDREAVER -> Fx.VOID;
            case EARTHSHAKER -> 0xFF9A3C;
            case BLOODFANG -> 0xFF3B4E;
            case SKYPIERCER -> 0x9AD8FF;
            case MOONVEIL -> 0xD6DEFF;
            case KINGSBANE -> Fx.GOLD;
            case SOLARIS -> 0xFFB01F;
            case WORLDSPLITTER -> 0x22E0C8;
            case ECLIPSE -> 0x7A2CFF;
            case STARFALL -> 0xFF7A1A;
            case SOULREAPER -> 0x5BFFB0;
            case VENOMFANG -> 0x7CFF3A;
            case DRAGONSPINE -> 0xFF5A1F;
            case TITANBREAKER -> 0xFFD24A;
        };
    }

    /** Size of a swing's slash trail. */
    public static float trailScale(WeaponType type) {
        return switch (type) {
            case DUAL_DAGGERS -> 0.55F;
            case LONGSWORD, KATANA -> 0.85F;
            case SPEAR -> 0.75F;
            case WARHAMMER, BATTLEAXE -> 1.05F;
            case GREATSWORD -> 1.25F;
            case SCYTHE -> 1.35F;
        };
    }

    /** How far in front of the eyes a swing's trail appears. */
    public static double trailDistance(WeaponType type) {
        return switch (type) {
            case DUAL_DAGGERS -> 1.3;
            case SPEAR -> 2.2;
            case GREATSWORD, SCYTHE -> 2.0;
            default -> 1.7;
        };
    }

    private WeaponLooks() {
    }
}

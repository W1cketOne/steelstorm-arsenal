package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.ability.sets.BattleaxeAbilities;
import com.steelstorm.arsenal.ability.sets.DaggerAbilities;
import com.steelstorm.arsenal.ability.sets.GreatswordAbilities;
import com.steelstorm.arsenal.ability.sets.HammerAbilities;
import com.steelstorm.arsenal.ability.sets.KatanaAbilities;
import com.steelstorm.arsenal.ability.sets.LegendaryUltimates;
import com.steelstorm.arsenal.ability.sets.LongswordAbilities;
import com.steelstorm.arsenal.ability.sets.ScytheAbilities;
import com.steelstorm.arsenal.ability.sets.SpearAbilities;
import com.steelstorm.arsenal.ability.sets.ThrownAbilities;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;

/**
 * Which abilities each weapon gives. Every weapon type has its own set; legendary weapons keep
 * their type's three abilities but bring their own ultimate. Shared by both sides: the client
 * uses it for the HUD and tooltips, the server to run the abilities.
 */
public final class Abilities {
    private static final Map<WeaponType, AbilitySet> BY_TYPE = new EnumMap<>(WeaponType.class);
    private static final Map<LegendaryWeaponItem.Legendary, AbilitySet> BY_LEGENDARY = new EnumMap<>(LegendaryWeaponItem.Legendary.class);
    private static final AbilitySet CHAKRAM;
    private static final AbilitySet THROWING_KNIFE;
    private static final Map<String, Ability> ALL = new LinkedHashMap<>();

    static {
        BY_TYPE.put(WeaponType.LONGSWORD, LongswordAbilities.create());
        BY_TYPE.put(WeaponType.GREATSWORD, GreatswordAbilities.create());
        BY_TYPE.put(WeaponType.KATANA, KatanaAbilities.create());
        BY_TYPE.put(WeaponType.DUAL_DAGGERS, DaggerAbilities.create());
        BY_TYPE.put(WeaponType.SPEAR, SpearAbilities.create());
        BY_TYPE.put(WeaponType.WARHAMMER, HammerAbilities.create());
        BY_TYPE.put(WeaponType.SCYTHE, ScytheAbilities.create());
        BY_TYPE.put(WeaponType.BATTLEAXE, BattleaxeAbilities.create());
        for (LegendaryWeaponItem.Legendary legendary : LegendaryWeaponItem.Legendary.values()) {
            BY_LEGENDARY.put(legendary, BY_TYPE.get(legendary.type()).withUltimate(legendary.id(), LegendaryUltimates.of(legendary)));
        }
        CHAKRAM = ThrownAbilities.chakram();
        THROWING_KNIFE = ThrownAbilities.throwingKnife();
        List<AbilitySet> sets = new ArrayList<>(BY_TYPE.values());
        sets.addAll(BY_LEGENDARY.values());
        sets.add(CHAKRAM);
        sets.add(THROWING_KNIFE);
        for (AbilitySet set : sets) {
            for (int slot = 0; slot < AbilitySet.SLOTS; slot++) {
                Ability ability = set.get(slot);
                Ability existing = ALL.putIfAbsent(ability.id(), ability);
                if (existing != null && existing != ability) {
                    throw new IllegalStateException("Duplicate ability id " + ability.id());
                }
            }
        }
    }

    /** The abilities the held stack gives, or null for anything that isn't a Steelstorm weapon. */
    @Nullable
    public static AbilitySet forStack(ItemStack stack) {
        if (stack.getItem() instanceof LegendaryWeaponItem legendary) {
            return BY_LEGENDARY.get(legendary.legendary());
        }
        if (stack.getItem() instanceof WeaponItem weapon && hasAbilities(weapon)) {
            return BY_TYPE.get(weapon.type());
        }
        return null;
    }

    /** Only the top tiers unlock abilities: Netherite and Stormsteel weapons (legendaries always have them). */
    public static boolean hasAbilities(WeaponItem weapon) {
        return weapon.weaponTier() == WeaponTier.NETHERITE || weapon.weaponTier() == WeaponTier.STORMSTEEL;
    }

    public static AbilitySet forType(WeaponType type) {
        return BY_TYPE.get(type);
    }

    /** Every ability, in a stable order (for the language file and the guide). */
    public static Map<String, Ability> all() {
        return Collections.unmodifiableMap(ALL);
    }

    private Abilities() {
    }
}

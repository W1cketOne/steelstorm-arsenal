package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;

public class ModLanguageProvider extends LanguageProvider {
    private static final Map<WeaponTier, String> TIER_NAMES = Map.of(
            WeaponTier.STONE, "Stone",
            WeaponTier.IRON, "Iron",
            WeaponTier.GOLD, "Golden",
            WeaponTier.DIAMOND, "Diamond",
            WeaponTier.STORMSTEEL, "Stormsteel",
            WeaponTier.NETHERITE, "Netherite");

    public ModLanguageProvider(PackOutput output) {
        super(output, SteelstormArsenal.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.steelstorm", "Steelstorm Arsenal");

        weapons();
        items();
        tooltips();
        book();
        misc();
    }

    private void weapons() {
        type(WeaponType.LONGSWORD, "Longsword", "Measured Rhythm", "Every 3rd hit in a row deals +30% damage.");
        type(WeaponType.GREATSWORD, "Greatsword", "Broad Arc", "Sweeping blows reach much further around the target.");
        type(WeaponType.KATANA, "Katana", "Crimson Edge", "Critical hits cause Bleed.");
        type(WeaponType.DUAL_DAGGERS, "Dual Daggers", "Backstab", "Double damage when striking from behind.");
        type(WeaponType.SPEAR, "Spear", "Long Reach", "Strikes from 2 blocks further away and can be thrown.");

        for (WeaponTier tier : WeaponTier.values()) {
            add(tier.translationKey(), TIER_NAMES.get(tier));
        }
    }

    private void type(WeaponType type, String name, String passive, String passiveDesc) {
        add(type.translationKey(), name);
        add("weapon.steelstorm." + type.id() + ".passive", passive);
        add("weapon.steelstorm." + type.id() + ".passive.desc", passiveDesc);
        for (WeaponTier tier : WeaponTier.values()) {
            add(ModItems.weapon(type, tier).get(), TIER_NAMES.get(tier) + " " + name);
        }
    }

    private void items() {
        add(ModItems.STORMSTEEL_INGOT.get(), "Stormsteel Ingot");
        add(ModEntities.THROWN_SPEAR.get(), "Thrown Spear");
    }

    private void tooltips() {
        add("tooltip.steelstorm.type_tier", "%s · %s");
        add("tooltip.steelstorm.passive", "Passive: %s");
        add("tooltip.steelstorm.spear_throw", "Sneak + hold Use to throw");
        add("tooltip.steelstorm.stormsteel", "Stormsteel: hits may arc lightning to a nearby foe");
        add("tooltip.steelstorm.kills", "Foes slain: %s");
    }

    private void book() {
        page(1, "Welcome, Warrior",
                "This outpost is yours. Take the starter weapons from the chest and practise on the dummies in the yard.\n\n"
                        + "In Steelstorm, timing beats button mashing. Read on!");
        page(2, "Stamina",
                "The bar above your hunger is Stamina.\n\nDodging, heavy attacks and specials spend it. "
                        + "It refills once you stop using it for a moment, so pace yourself.");
        page(3, "Dodge Roll",
                "Press %1$s to dodge roll the way you are moving.\n\nFor a split second nothing can hurt you. "
                        + "Roll through the big, telegraphed attacks!");
        page(4, "Guard & Parry",
                "Hold %4$s with a melee weapon to guard and take less damage.\n\n"
                        + "Raise your guard just before a hit lands for a PERFECT PARRY: no damage, and your attacker staggers.");
        page(5, "Heavy Attacks",
                "Hold %5$s and press %3$s for a heavy attack: 1.5x damage and big knockback, for a little stamina.\n\n"
                        + "Spears: hold %5$s and %4$s, then let go to throw.");
        page(6, "Special Moves",
                "Press %2$s to unleash your weapon's special move. Every weapon type has its own; "
                        + "check the tooltip.\n\nSpecials cost stamina and need time to recharge.");
        page(7, "The Arsenal",
                "Longsword: 3-hit combos\nGreatsword: wide sweeps\nKatana: bleeding crits\nDaggers: backstabs\n"
                        + "Spear: reach and throws\nWarhammer: breaks armor\nScythe: life steal\nBattleaxe: crushes guards");
        page(8, "Stormsteel & Legends",
                "Stormsteel ore hides in the mountains. Its weapons crackle with lightning that leaps between foes.\n\n"
                        + "Legendary weapons can't be crafted. Seek them in ruins, or take one from the Warlord.");
        page(9, "Dangers Abroad",
                "Bandit camps dot the plains and forests. Iron Revenants walk at night.\n\n"
                        + "In a ruined colosseum the Fallen Warlord waits. Bring diamond gear, and remember to dodge.");
    }

    private void page(int index, String title, String body) {
        add("book.steelstorm.manual.page" + index + ".title", title);
        add("book.steelstorm.manual.page" + index + ".body", body);
    }

    private void misc() {
        add("message.steelstorm.outpost_welcome",
                "A Warrior's Outpost stands beside you. Read the Combat Manual on the lectern before your first fight!");
        add("sign.steelstorm.outpost.line1", "Warrior's");
        add("sign.steelstorm.outpost.line2", "Outpost");
        add("sign.steelstorm.outpost.line3", "Train hard,");
        add("sign.steelstorm.outpost.line4", "fight smart");

        add("steelstorm.configuration.title", "Steelstorm Arsenal Settings");
        add("steelstorm.configuration.section.steelstorm.common.toml", "Steelstorm Arsenal Settings");
        add("steelstorm.configuration.section.steelstorm.common.toml.title", "Steelstorm Arsenal Settings");
        add("steelstorm.configuration.structures", "World Generation");
        add("steelstorm.configuration.starterOutpost", "Starter Outpost");
    }
}

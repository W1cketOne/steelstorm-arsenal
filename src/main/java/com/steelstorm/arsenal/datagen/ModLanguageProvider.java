package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, SteelstormArsenal.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.steelstorm.arsenal", "Steelstorm Arsenal");

        for (WeaponType type : WeaponType.values()) {
            for (WeaponTier tier : WeaponTier.values()) {
                add(ModItems.weapon(type, tier).get(), tier.displayName() + " " + type.displayName());
            }
        }
        add(ModItems.STORMSTEEL_INGOT.get(), "Stormsteel Ingot");

        weapon(WeaponType.LONGSWORD, "Every 3rd hit in a row deals +30% damage",
                "Rising Slash", "Launches the enemy in front of you into the air");
        weapon(WeaponType.GREATSWORD, "Wide sweeping strikes hit enemies around your target",
                "Ground Cleave", "Smashes everything in a cone in front of you");
        weapon(WeaponType.KATANA, "Critical hits cause Bleed",
                "Flash Step", "Dash forward through enemies, cutting each one (stops at walls)");
        weapon(WeaponType.DUAL_DAGGERS, "Double damage when striking from behind",
                "Flurry", "Five lightning-fast strikes on the enemy in front of you");
        weapon(WeaponType.SPEAR, "Sneak + hold Use to throw it",
                "Impale", "Skewers the enemy in front of you and pins it in place");

        add("key.categories.steelstorm", "Steelstorm Arsenal");
        add("key.steelstorm.dodge", "Dodge Roll");
        add("key.steelstorm.special", "Special Ability");
        add("message.steelstorm.special_cooldown", "Special ready in %ss");
        add("message.steelstorm.no_stamina", "Not enough stamina!");
        add("message.steelstorm.no_target", "No target in front of you");
        add("message.steelstorm.perfect_parry", "Perfect Parry!");
        add("message.steelstorm.guard_broken", "Guard broken!");
        add(ModEffects.STAGGER.get(), "Stagger");

        config("stamina", "Stamina");
        config("maxStamina", "Max Stamina");
        config("regenPerTick", "Regen per Tick");
        config("regenDelayTicks", "Regen Delay (ticks)");
        config("dodgeCost", "Dodge Cost");
        config("heavyAttackCost", "Heavy Attack Cost");
        config("guardCostPerDamage", "Guard Cost per Damage");
        config("specialCostMultiplier", "Special Cost Multiplier");
        config("combat", "Combat");
        config("weaponDamageMultiplier", "Weapon Damage Multiplier");
        config("heavyAttackMultiplier", "Heavy Attack Multiplier");
        config("specialDamageMultiplier", "Special Damage Multiplier");
        config("guardDamageReduction", "Guard Damage Reduction");
        config("perfectParryTicks", "Perfect Parry Window (ticks)");
        config("dodgeCooldownTicks", "Dodge Cooldown (ticks)");
        config("dodgeInvulnerabilityTicks", "Dodge Invulnerability (ticks)");
        config("specialCooldownMultiplier", "Special Cooldown Multiplier");
        config("feedback", "Feedback");
        config("screenShake", "Screen Shake");
        config("screenShakeStrength", "Screen Shake Strength");
        config("showComboCounter", "Show Combo Counter");

        add("tooltip.steelstorm.stats", "Damage %s | Speed %s | Reach %s");
        add("tooltip.steelstorm.passive", "Passive: ");
        add("tooltip.steelstorm.special", "Special [%2$s]: %1$s");
        add("tooltip.steelstorm.special_cost", "  Costs %s stamina, %ss cooldown");
        add("tooltip.steelstorm.stormsteel", "Stormsteel: hits sometimes zap a second nearby enemy");
        add("tooltip.steelstorm.controls", "Hold Use to guard and parry. Sneak + attack for a heavy attack.");
    }

    private void config(String key, String name) {
        add(SteelstormArsenal.MODID + ".configuration." + key, name);
    }

    private void weapon(WeaponType type, String passive, String specialName, String specialDesc) {
        add(type.passiveKey(), passive);
        add(type.specialNameKey(), specialName);
        add(type.specialDescKey(), specialDesc);
    }
}

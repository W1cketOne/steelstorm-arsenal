package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Abilities;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
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
        add(ModItems.RAW_STORMSTEEL.get(), "Raw Stormsteel");
        add(ModItems.CHAKRAM.get(), "Chakram");
        add(ModItems.THROWING_KNIFE.get(), "Throwing Knife");
        add(ModItems.TARGET_DUMMY.get(), "Target Dummy");
        add(ModBlocks.STORMSTEEL_ORE.get(), "Stormsteel Ore");
        add(ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get(), "Deepslate Stormsteel Ore");
        add(ModBlocks.STORMSTEEL_BLOCK.get(), "Block of Stormsteel");
        add(ModBlocks.RAW_STORMSTEEL_BLOCK.get(), "Block of Raw Stormsteel");
        add(ModBlocks.WEAPON_RACK.get(), "Weapon Rack");
        add(ModEntities.TARGET_DUMMY.get(), "Target Dummy");
        add(ModEntities.THROWN_SPEAR.get(), "Thrown Spear");
        add(ModEntities.THROWING_KNIFE.get(), "Throwing Knife");
        add(ModEntities.CHAKRAM.get(), "Chakram");
        add(ModEffects.BLEED.get(), "Bleed");
        add(ModEffects.ARMOR_BREAK.get(), "Armor Break");
        enchantment("lacerate", "Lacerate", "Hits have a chance to cause Bleed.");
        enchantment("executioner", "Executioner", "Deal extra damage to enemies below 35% health.");
        enchantment("momentum", "Momentum", "Landing hits restores stamina.");
        for (LegendaryWeaponItem.Legendary legendary : LegendaryWeaponItem.Legendary.values()) {
            add(ModItems.legendary(legendary).get(), legendary.displayName());
        }
        add("weapon.steelstorm.tempest_edge.legendary", "Combo finishers call down lightning");
        add("weapon.steelstorm.rimecleaver.legendary", "Hits chill enemies, slowing them to a crawl");
        add("weapon.steelstorm.voidreaver.legendary", "Hits tear at the void, pulling nearby enemies toward the target");
        add("weapon.steelstorm.earthshaker.legendary", "Combo finishers send a shockwave through the ground");
        add("weapon.steelstorm.bloodfang.legendary", "Hits cause Bleed; striking a bleeding enemy heals you");
        add("weapon.steelstorm.skypiercer.legendary", "Double damage against enemies in the air");
        add("weapon.steelstorm.moonveil.legendary", "Critical hits release a crescent of moonlight");
        add("weapon.steelstorm.kingsbane.legendary", "+50% damage against enemies with more health than you");
        add("tooltip.steelstorm.legendary", "Legendary: ");
        add(ModItems.BANDIT_DUELIST_SPAWN_EGG.get(), "Bandit Duelist Spawn Egg");
        add(ModItems.BANDIT_ARCHER_SPAWN_EGG.get(), "Bandit Archer Spawn Egg");
        add(ModItems.IRON_REVENANT_SPAWN_EGG.get(), "Iron Revenant Spawn Egg");
        add(ModItems.FALLEN_WARLORD_SPAWN_EGG.get(), "Fallen Warlord Spawn Egg");
        add(ModEntities.BANDIT_DUELIST.get(), "Bandit Duelist");
        add(ModEntities.BANDIT_ARCHER.get(), "Bandit Archer");
        add(ModEntities.IRON_REVENANT.get(), "Iron Revenant");
        add(ModEntities.FALLEN_WARLORD.get(), "The Fallen Warlord");
        add("message.steelstorm.warlord_phase2", "The Fallen Warlord calls on his fallen knights!");
        add("message.steelstorm.warlord_phase3", "The Fallen Warlord is enraged!");
        config("structures", "Structures");
        config("abandonedArmoryChance", "Abandoned Armory Chance");
        config("banditCampChance", "Bandit Camp Chance");
        config("ruinedColosseumChance", "Ruined Colosseum Chance");
        config("starterOutpost", "Starter Outpost");
        add("death.attack.steelstorm.bleed", "%1$s bled out");
        add("death.attack.steelstorm.bleed.player", "%1$s bled out while fighting %2$s");
        add("death.attack.steelstorm.zap", "%1$s was zapped by %2$s");
        add("death.attack.steelstorm.zap.player", "%1$s was zapped by %2$s");
        add("death.attack.steelstorm.zap.item", "%1$s was zapped by %2$s using %3$s");
        add("tooltip.steelstorm.chakram", "Right-click to throw. Bounces between 3 enemies, then returns to you.");
        add("tooltip.steelstorm.throwing_knife", "Right-click to throw. 30% chance to cause Bleed.");
        add("tooltip.steelstorm.target_dummy", "Shows the damage of every hit. Sneak-punch with an empty hand to pick it up.");

        weapon(WeaponType.LONGSWORD, "Every 3rd hit in a row deals +30% damage");
        weapon(WeaponType.GREATSWORD, "Wide sweeping strikes hit enemies around your target");
        weapon(WeaponType.KATANA, "Critical hits cause Bleed");
        weapon(WeaponType.DUAL_DAGGERS, "Double damage when striking from behind");
        weapon(WeaponType.SPEAR, "Sneak + hold Use to throw it");
        weapon(WeaponType.WARHAMMER, "Hits cause Armor Break");
        weapon(WeaponType.SCYTHE, "Heals you for 10% of the damage you deal");
        weapon(WeaponType.BATTLEAXE, "Bonus damage against blocking enemies, and breaks shields");

        // Every weapon ability, straight from its definition.
        Abilities.all().values().forEach(ability -> {
            add(ability.nameKey(), ability.name());
            add(ability.descKey(), ability.description());
        });

        ModSounds.SUBTITLES.forEach((event, text) -> add("subtitles.steelstorm." + event, text));
        add("key.categories.steelstorm", "Steelstorm Arsenal");
        add("key.steelstorm.dodge", "Dodge Roll");
        add("key.steelstorm.ability_1", "Ability 1");
        add("key.steelstorm.ability_2", "Ability 2");
        add("key.steelstorm.ability_3", "Ability 3");
        add("key.steelstorm.ultimate", "Ultimate");
        add("message.steelstorm.ability_cooldown", "%s ready in %ss");
        add("message.steelstorm.ultimate_not_ready", "Ultimate charging: %s%%");
        add("message.steelstorm.ultimate_ready", "Ultimate ready!");
        add("message.steelstorm.riposte", "Riposte!");
        add("message.steelstorm.backstab", "Backstab!");
        add("message.steelstorm.no_stamina", "Not enough stamina!");
        add("message.steelstorm.no_target", "No target in front of you");
        add("message.steelstorm.perfect_parry", "Perfect Parry!");
        add("message.steelstorm.guard_broken", "Guard broken!");
        add(ModEffects.STAGGER.get(), "Stagger");
        add(ModEffects.FROZEN.get(), "Frozen");
        add(ModEffects.BERSERK.get(), "Berserk");
        add(ModEffects.RIPOSTE.get(), "Riposte Stance");
        add(ModEffects.SHADOW_VEIL.get(), "Shadow Veil");
        add(ModEffects.MARKED.get(), "Marked");
        add(ModEntities.THROWN_HAMMER.get(), "Thrown Hammer");
        add(ModEntities.GROUND_WAVE.get(), "Shockwave");
        add(ModEntities.EARTH_CHUNK.get(), "Earth Chunk");
        add(ModEntities.SLASH_WAVE.get(), "Slash Wave");
        add(ModEntities.SPECTRAL_WEAPON.get(), "Spectral Weapon");
        add(ModEntities.VORTEX.get(), "Vortex");
        add(ModEntities.ORBIT_BLADES.get(), "Orbiting Blades");

        config("stamina", "Stamina");
        config("maxStamina", "Max Stamina");
        config("regenPerTick", "Regen per Tick");
        config("regenDelayTicks", "Regen Delay (ticks)");
        config("dodgeCost", "Dodge Cost");
        config("heavyAttackCost", "Heavy Attack Cost");
        config("guardCostPerDamage", "Guard Cost per Damage");
        config("specialCostMultiplier", "Ability Stamina Cost Multiplier");
        config("combat", "Combat");
        config("weaponDamageMultiplier", "Weapon Damage Multiplier");
        config("heavyAttackMultiplier", "Heavy Attack Multiplier");
        config("specialDamageMultiplier", "Ability Damage Multiplier");
        config("guardDamageReduction", "Guard Damage Reduction");
        config("perfectParryTicks", "Perfect Parry Window (ticks)");
        config("dodgeCooldownTicks", "Dodge Cooldown (ticks)");
        config("dodgeInvulnerabilityTicks", "Dodge Invulnerability (ticks)");
        config("specialCooldownMultiplier", "Ability Cooldown Multiplier");
        config("ultimateChargeMultiplier", "Ultimate Charge Multiplier");
        config("feedback", "Feedback");
        config("screenShake", "Screen Shake");
        config("screenShakeStrength", "Screen Shake Strength");
        config("showComboCounter", "Show Combo Counter");

        add("tooltip.steelstorm.stats", "Damage %s | Speed %s | Reach %s");
        add("tooltip.steelstorm.passive", "Passive: ");
        add("tooltip.steelstorm.abilities", "Abilities:");
        add("tooltip.steelstorm.ability_cost", " - %s stamina, %ss");
        add("tooltip.steelstorm.ultimate_cost", " - ultimate, %ss");
        add("tooltip.steelstorm.hold_shift", "Hold Shift for ability details");
        add("tooltip.steelstorm.stormsteel", "Stormsteel: hits sometimes zap a second nearby enemy");
        add("tooltip.steelstorm.controls", "Hold Use to guard and parry. Sneak + attack for a heavy attack.");
    }

    private void enchantment(String id, String name, String description) {
        add("enchantment.steelstorm." + id, name);
        add("enchantment.steelstorm." + id + ".desc", description);
    }

    private void config(String key, String name) {
        add(SteelstormArsenal.MODID + ".configuration." + key, name);
    }

    private void weapon(WeaponType type, String passive) {
        add(type.passiveKey(), passive);
    }
}

package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Abilities;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.Rune;
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
        config("provingGroundsChance", "Proving Grounds Chance");
        config("knightsCryptChance", "Knight's Crypt Chance");
        config("stormShrineChance", "Storm Shrine Chance");
        config("blacksmithChance", "Blacksmith's Forge Chance");
        config("watchtowerChance", "Watchtower Chance");
        config("stormsteelMineChance", "Stormsteel Mine Chance");
        config("bossLairChance", "Boss Lair Chance");
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
        add("key.steelstorm.inspect", "Inspect Weapon");
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
        config("showDamageNumbers", "Show Damage Numbers");

        // Stage 3: interactive blocks, runes, armour and new enemies.
        add(ModBlocks.WHETSTONE.get(), "Whetstone");
        add(ModBlocks.RUNE_FORGE.get(), "Rune Forge");
        add(ModBlocks.ARENA_GONG.get(), "Arena Gong");
        add(ModBlocks.CHAMPIONS_COFFER.get(), "Champion's Coffer");
        add(ModBlocks.BANDIT_VAULT.get(), "Bandit Vault");
        add(ModBlocks.SARCOPHAGUS.get(), "Sarcophagus");
        add(ModBlocks.LEGENDARY_PEDESTAL.get(), "Legendary Pedestal");
        add(ModBlocks.STORM_ALTAR.get(), "Storm Altar");
        add(ModBlocks.SIGNAL_BRAZIER.get(), "Signal Brazier");
        for (Rune rune : Rune.values()) {
            add(ModItems.rune(rune).get(), rune.displayName() + " Rune");
            add(rune.nameKey(), rune.displayName());
            add(rune.effectKey(), rune.effect());
        }
        add(ModItems.VAULT_KEY.get(), "Vault Key");
        add(ModItems.EXPLORERS_COMPASS.get(), "Explorer's Compass");
        add("tooltip.steelstorm.compass_target", "Searching for: %s");
        add("tooltip.steelstorm.compass_use", "Use to search. Sneak + use to choose a structure.");
        add("message.steelstorm.compass_target", "Now searching for: %s");
        add("message.steelstorm.compass_none", "No %s found anywhere near");
        add("message.steelstorm.compass_found", "%s: %s blocks %s (x %s, z %s)");
        for (String[] d : new String[][]{{"north", "north"}, {"north_east", "north-east"}, {"east", "east"}, {"south_east", "south-east"},
                {"south", "south"}, {"south_west", "south-west"}, {"west", "west"}, {"north_west", "north-west"}}) {
            add("direction.steelstorm." + d[0], d[1]);
        }
        for (String[] st : new String[][]{{"knights_crypt", "Knight's Crypt"}, {"colossus_forge", "Colossus Forge (boss)"},
                {"moonlit_sanctum", "Moonlit Sanctum (boss)"}, {"storm_shrine", "Storm Shrine"}, {"ruined_colosseum", "Ruined Colosseum"},
                {"proving_grounds", "Proving Grounds"}, {"stormsteel_mine", "Stormsteel Mine"}, {"bandit_camp", "Bandit Camp"},
                {"blacksmith", "Blacksmith's Forge"}, {"watchtower", "Watchtower"}, {"abandoned_armory", "Abandoned Armory"}}) {
            add("structure.steelstorm." + st[0], st[1]);
        }
        add(ModItems.STORMSTEEL_HELMET.get(), "Stormsteel Helmet");
        add(ModItems.STORMSTEEL_CHESTPLATE.get(), "Stormsteel Chestplate");
        add(ModItems.STORMSTEEL_LEGGINGS.get(), "Stormsteel Leggings");
        add(ModItems.STORMSTEEL_BOOTS.get(), "Stormsteel Boots");
        add(ModItems.BANDIT_CAPTAIN_SPAWN_EGG.get(), "Bandit Captain Spawn Egg");
        add(ModItems.CRYPT_KNIGHT_SPAWN_EGG.get(), "Crypt Knight Spawn Egg");
        add(ModItems.STORM_HERALD_SPAWN_EGG.get(), "Storm Herald Spawn Egg");
        add(ModEntities.BANDIT_CAPTAIN.get(), "Bandit Captain");
        add(ModEntities.CRYPT_KNIGHT.get(), "Crypt Knight");
        add(ModEntities.STORM_HERALD.get(), "The Storm Herald");
        add(ModItems.FORGE_COLOSSUS_SPAWN_EGG.get(), "Forge Colossus Spawn Egg");
        add(ModItems.MOONBLADE_REVENANT_SPAWN_EGG.get(), "Moonblade Revenant Spawn Egg");
        add(ModEntities.FORGE_COLOSSUS.get(), "The Forge Colossus");
        add(ModEntities.MOONBLADE_REVENANT.get(), "The Moonblade Revenant");
        add("tooltip.steelstorm.rune_use", "Place it on a Rune Forge, then use a weapon on the forge");
        add("tooltip.steelstorm.rune", "Rune: %s");
        add("tooltip.steelstorm.sharpened", "Sharpened: +2 damage for %s more hits");
        add("tooltip.steelstorm.vault_key", "Opens a Bandit Vault. Carried by Bandit Captains.");
        add("tooltip.steelstorm.armor.helmet", "Storm Sight");
        add("tooltip.steelstorm.armor.helmet.desc", " Hostile mobs within 24 blocks glow through walls");
        add("tooltip.steelstorm.armor.chestplate", "Static Barrier");
        add("tooltip.steelstorm.armor.chestplate.desc", " Every 20 s, absorbs a hit and blasts attackers away");
        add("tooltip.steelstorm.armor.leggings", "Lightning Sprint");
        add("tooltip.steelstorm.armor.leggings.desc", " Sprint for 1.5 s to surge to Speed II");
        add("tooltip.steelstorm.armor.boots", "Thunder Step");
        add("tooltip.steelstorm.armor.boots.desc", " Jump again in mid-air; hard landings send out a shockwave");
        add(ModItems.WARLORD_HELMET.get(), "Ember Warlord Helm");
        add(ModItems.WARLORD_CHESTPLATE.get(), "Ember Warlord Cuirass");
        add(ModItems.WARLORD_LEGGINGS.get(), "Ember Warlord Greaves");
        add(ModItems.WARLORD_BOOTS.get(), "Ember Warlord Sabatons");
        add("tooltip.steelstorm.warlord.helmet", "Infernal Eyes");
        add("tooltip.steelstorm.warlord.helmet.desc", " See in the dark; blindness and darkness can't touch you");
        add("tooltip.steelstorm.warlord.chestplate", "Magma Heart");
        add("tooltip.steelstorm.warlord.chestplate.desc", " Melee attackers are scorched and set ablaze");
        add("tooltip.steelstorm.warlord.leggings", "Scorched Path");
        add("tooltip.steelstorm.warlord.leggings.desc", " Sprint into enemies to bowl them aside in flames");
        add("tooltip.steelstorm.warlord.boots", "Meteor Fall");
        add("tooltip.steelstorm.warlord.boots.desc", " Sneak in mid-air to slam down in a fiery shockwave");
        add("tooltip.steelstorm.warlord_set", "Warlord's Wrath (full set):");
        add("tooltip.steelstorm.warlord_set.desc", " Immune to fire and lava; your strikes set enemies ablaze");
        add("tooltip.steelstorm.stormsteel_set", "Stormcaller (full set):");
        add("tooltip.steelstorm.stormsteel_set.desc", " 10% faster, immune to lightning, melee attackers may be zapped");
        add("message.steelstorm.sharpened", "Sharpened! +2 damage for the next %s hits");
        add("message.steelstorm.already_sharp", "This blade is already razor sharp");
        add("message.steelstorm.dull", "Your blade has dulled");
        add("message.steelstorm.rune_inscribed", "%s rune burned into %s");
        add("message.steelstorm.rune_replaced", "%s rune burned into %s, replacing its old rune");
        add("message.steelstorm.rune_forge_hint", "Place a rune on the forge, then use a weapon on it");
        add("message.steelstorm.coffer_locked", "Sealed. Strike the Arena Gong and defeat every challenger.");
        add("message.steelstorm.vault_locked", "Locked. A Bandit Captain carries the key.");
        add("message.steelstorm.vault_unlocked", "The vault swings open!");
        add("message.steelstorm.gong_active", "The challenge is already underway!");
        add("message.steelstorm.gong_resting", "The gong is silent. Try again in %s min.");
        add("message.steelstorm.sarcophagus", "Something stirs inside...");
        add("message.steelstorm.pedestal_sealed", "Sealed by %s. Defeat the guardian first!");
        add("message.steelstorm.pedestal_tombs", "Sealed. The dead still sleep here: open every sarcophagus.");
        add("message.steelstorm.pedestal_claimed", "You claimed %s!");
        add("message.steelstorm.storm_rages", "The storm already rages. Defeat the Storm Herald first.");
        add("message.steelstorm.altar_charge", "The altar hums with power (%s/%s)");
        add("message.steelstorm.altar_hint", "Offer Stormsteel Ingots to call the storm (%s/%s)");
        add("message.steelstorm.brazier_lit", "The signal fire reveals %s hidden enemies");
        add("message.steelstorm.herald_phase2", "The Storm Herald unleashes the tempest!");
        add("boss.steelstorm.arena_wave", "Arena Challenge: Wave %s/%s");
        add("title.steelstorm.wave", "Wave %s");
        add("title.steelstorm.wave_sub", "Defeat every challenger");
        add("title.steelstorm.final_wave", "The Bandit Captain enters the arena!");
        add("title.steelstorm.wave_clear", "Wave Cleared");
        add("title.steelstorm.next_wave", "Brace yourself...");
        add("title.steelstorm.victory", "Victory!");
        add("title.steelstorm.victory_sub", "The crowd roars your name");
        add("title.steelstorm.coffer_open", "The Champion's Coffer is open");

        add("tooltip.steelstorm.stats", "Damage %s | Speed %s | Reach %s");
        add("tooltip.steelstorm.passive", "Passive: ");
        add("tooltip.steelstorm.abilities", "Abilities:");
        add("tooltip.steelstorm.ability_cost", " - %s stamina, %ss");
        add("tooltip.steelstorm.ultimate_cost", " - ultimate, %ss");
        add("tooltip.steelstorm.hold_shift", "Hold Shift for ability details");
        add("tooltip.steelstorm.charges", " x%s charges");
        add("tooltip.steelstorm.hold_ultimate", " Hold [%s] to charge the ultimate: up to +75%% damage");
        add("message.steelstorm.charging", "Charging... release to unleash!");
        add("message.steelstorm.overcharged", "OVERCHARGED!");
        add("tooltip.steelstorm.no_abilities", "Abilities unlock at Netherite and Stormsteel tier");
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

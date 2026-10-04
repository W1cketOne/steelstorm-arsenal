package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import com.steelstorm.compat.neo.neoforge.registries.DeferredHolder;
import com.steelstorm.compat.neo.neoforge.registries.DeferredRegister;

/**
 * Every sound is original and synthesized by tools/gen_sounds.py, which also writes sounds.json.
 * Event names here must match that file.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SteelstormArsenal.MODID);
    /** Subtitle text per event, used by the language provider. */
    public static final Map<String, String> SUBTITLES = new LinkedHashMap<>();

    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SWING = register("weapon.swing", "Weapon swings");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SWING_HEAVY = register("weapon.swing_heavy", "Heavy weapon swings");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HIT = register("weapon.hit", "Weapon hits");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HIT_METAL = register("weapon.hit_metal", "Steel clangs");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_PARRY = register("weapon.parry", "Attack blocked");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_PERFECT_PARRY = register("weapon.perfect_parry", "Perfect parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GUARD_BREAK = register("weapon.guard_break", "Guard breaks");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAYER_DODGE = register("player.dodge", "Player dodges");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_CAST = register("ability.cast", "Ability unleashed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_DASH = register("ability.dash", "Dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_READY = register("ability.ready", "Ability ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTIMATE_READY = register("ultimate.ready", "Ultimate ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTIMATE_CAST = register("ultimate.cast", "Ultimate unleashed");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS = register("music.boss", "Battle music");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTIMATE_RELEASE = register("ultimate.release", "Ultimate unleashed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ULTIMATE_CHARGE = register("ultimate.charge", "Power gathers");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_EPIC_IMPACT = register("ability.epic_impact", "Earth-shattering impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_LIGHTNING_STRIKE = register("ability.lightning_strike", "Lightning strikes");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BLADE_THROW = register("ability.blade_throw", "Blade flies");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BLADE_STORM = register("ability.blade_storm", "Blades sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_DARK_POWER = register("ability.dark_power", "Dark power surges");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_WIND_RUSH = register("ability.wind_rush", "Wind rushes");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_INFERNO = register("ability.inferno", "Fire roars");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMOR_DOUBLE_JUMP = register("armor.double_jump", "Thunder Step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMOR_BARRIER = register("armor.barrier", "Static Barrier absorbs a hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_SHOCKWAVE = register("ability.shockwave", "Shockwave");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_GROUND_CRACK = register("ability.ground_crack", "Ground cracks");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_RUMBLE = register("ability.rumble", "Ground rumbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_ZAP = register("ability.zap", "Lightning crackles");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_THUNDER = register("ability.thunder", "Thunder");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_FROST = register("ability.frost", "Ice shatters");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_VOID = register("ability.void", "Void surges");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_VOID_HUM = register("ability.void_hum", "Void hums");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BLOOD = register("ability.blood", "Blood splatters");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_SMOKE = register("ability.smoke", "Smoke bursts");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_SLASH_WAVE = register("ability.slash_wave", "Slash wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BLADE_FALL = register("ability.blade_fall", "Blades fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_KATANA_DRAW = register("ability.katana_draw", "Blade drawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_PETALS = register("ability.petals", "Petals swirl");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_WAR_CRY = register("ability.war_cry", "War cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BERSERK = register("ability.berserk", "Berserker rage");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_THROW = register("weapon.throw", "Weapon thrown");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CHAKRAM_SPIN = register("weapon.chakram_spin", "Chakram whirs");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HAMMER_THROW = register("weapon.hammer_throw", "Hammer thrown");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HAMMER_CATCH = register("weapon.hammer_catch", "Hammer caught");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_GONG = register("block.gong", "Gong rings");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_COFFER_UNLOCK = register("block.coffer_unlock", "Coffer unlocks");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_VAULT_OPEN = register("block.vault_open", "Vault opens");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SARCOPHAGUS_OPEN = register("block.sarcophagus_open", "Sarcophagus grinds open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ALTAR_CHARGE = register("block.altar_charge", "Altar charges");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ALTAR_SUMMON = register("block.altar_summon", "Something answers the altar");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WHETSTONE = register("block.whetstone", "Blade sharpened");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_RUNE_FORGE = register("block.rune_forge", "Rune forged");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_BRAZIER_IGNITE = register("block.brazier_ignite", "Brazier ignites");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WARLORD_ROAR = register("entity.warlord_roar", "Warlord roars");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_HERALD_CAST = register("entity.herald_cast", "Storm Herald casts");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CAPTAIN_TAUNT = register("entity.captain_taunt", "Bandit Captain taunts");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name, String subtitle) {
        SUBTITLES.put(name, subtitle);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(SteelstormArsenal.id(name)));
    }

    private ModSounds() {
    }
}

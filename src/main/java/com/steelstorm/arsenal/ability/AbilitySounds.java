package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

/**
 * The big signature sound layered on top of whatever an ability already plays: heavy slams
 * shake the ground, thrown blades sing, scythe powers growl, ultimates hit like a film trailer.
 */
public final class AbilitySounds {
    private record Layer(Holder<SoundEvent> sound, float volume, float pitch) {
    }

    private static final Map<String, Layer> LAYERS = new HashMap<>();

    static {
        map(ModSounds.ABILITY_EPIC_IMPACT, 1.4F, 1.0F, "warhammer_earthquake", "warhammer_cataclysm", "greatsword_colossus_strike",
                "spear_dragon_dive", "warhammer_stone_prison", "earthshaker_tectonic_spiral", "greatsword_titans_guard",
                "kingsbane_regicide", "spear_impale");
        map(ModSounds.ABILITY_LIGHTNING_STRIKE, 1.5F, 1.0F, "tempest_edge_thunder_verdict");
        map(ModSounds.ABILITY_BLADE_THROW, 1.0F, 1.0F, "longsword_sword_toss", "greatsword_crescent_wave", "scythe_deaths_crescent",
                "warhammer_hammer_throw", "dual_daggers_fan_of_knives", "katana_wind_scar", "battleaxe_chain_hook",
                "chakram_sawblade", "chakram_twin_throw", "throwing_knife_volley");
        map(ModSounds.ABILITY_BLADE_STORM, 1.3F, 1.0F, "longsword_judgment", "greatsword_sword_sanctum", "katana_thousand_cuts",
                "dual_daggers_death_blossom", "moonveil_moonfall", "throwing_knife_knife_storm", "rimecleaver_absolute_zero",
                "chakram_blade_tempest");
        map(ModSounds.ABILITY_DARK_POWER, 1.2F, 1.0F, "scythe_reap", "scythe_soul_harvest", "scythe_death_mark",
                "voidreaver_event_horizon", "dual_daggers_shadowstep", "bloodfang_hemorrhage");
        map(ModSounds.ABILITY_WIND_RUSH, 1.0F, 1.0F, "katana_flash_step", "longsword_blade_dash", "spear_vault_leap",
                "spear_sweeping_arc", "battleaxe_whirlwind", "skypiercer_updraft", "katana_iaido", "dual_daggers_flurry",
                "throwing_knife_blink");
        map(ModSounds.ABILITY_INFERNO, 1.3F, 1.0F, "battleaxe_war_cry", "battleaxe_berserker_rage");
    }

    private static void map(Holder<SoundEvent> sound, float volume, float pitch, String... ids) {
        for (String id : ids) {
            LAYERS.put(id, new Layer(sound, volume, pitch));
        }
    }

    /** Plays the ability's signature layer; `power` (charged ultimates) makes it louder and deeper. */
    public static void play(AbilityContext ctx, float power) {
        ServerPlayer player = ctx.player;
        Layer layer = LAYERS.get(ctx.ability.id());
        float rnd = 0.94F + player.getRandom().nextFloat() * 0.12F;
        if (layer != null) {
            Fx.sound(ctx.level, player.position(), layer.sound(), layer.volume() * power, layer.pitch() * rnd / (0.8F + 0.2F * power));
        }
        if (ctx.ability.isUltimate()) {
            Fx.sound(ctx.level, player.position(), ModSounds.ULTIMATE_RELEASE, 1.6F * power, rnd / (0.85F + 0.15F * power));
            if (ctx.weapon != null && ctx.weapon.isStormsteel()) {
                // Stormsteel ultimates go off with a thunderclap.
                Fx.sound(ctx.level, player.position(), ModSounds.ABILITY_LIGHTNING_STRIKE, 1.0F, 1.1F);
            }
        }
    }

    private AbilitySounds() {
    }
}

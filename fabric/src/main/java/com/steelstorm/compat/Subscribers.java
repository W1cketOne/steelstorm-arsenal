package com.steelstorm.compat;

/** Every class that used @EventBusSubscriber; client ones are only loaded on the client. */
public final class Subscribers {
    static final String[] COMMON = {
        "com.steelstorm.arsenal.ability.AbilityManager",
        "com.steelstorm.arsenal.combat.ArmorAbilities",
        "com.steelstorm.arsenal.combat.ArmorEffects",
        "com.steelstorm.arsenal.combat.CombatEvents",
        "com.steelstorm.arsenal.combat.KillEvents",
        "com.steelstorm.arsenal.combat.ServerScheduler",
        "com.steelstorm.arsenal.combat.VoidwalkerAbilities",
        "com.steelstorm.arsenal.combat.WarlordAbilities",
        "com.steelstorm.arsenal.combat.WeaponLight",
        "com.steelstorm.arsenal.network.ModNetwork",
        "com.steelstorm.arsenal.registry.ModEntities",
        "com.steelstorm.arsenal.world.MeteorShowers",
        "com.steelstorm.arsenal.world.ModTrades",
        "com.steelstorm.arsenal.world.OutpostEvents",
    };
    static final String[] CLIENT = {
        "com.steelstorm.arsenal.client.BossMusic",
        "com.steelstorm.arsenal.client.ClientArmorAbilities",
        "com.steelstorm.arsenal.client.ClientEvents",
        "com.steelstorm.arsenal.client.ClientRenderers",
        "com.steelstorm.arsenal.client.CombatHud",
        "com.steelstorm.arsenal.client.DamageNumbers",
        "com.steelstorm.arsenal.client.KillBanner",
        "com.steelstorm.arsenal.client.ModKeyMappings",
        "com.steelstorm.arsenal.client.StormsteelArmorRendering",
        "com.steelstorm.arsenal.client.TooltipTheme",
        "com.steelstorm.arsenal.client.UltimateCinematic",
        "com.steelstorm.arsenal.client.anim.AnimationEvents",
        "com.steelstorm.arsenal.client.anim.MovementAnims",
        "com.steelstorm.arsenal.client.anim.SwingTrails",
        "com.steelstorm.arsenal.client.boss.BossModels",
        "com.steelstorm.arsenal.client.particle.ModParticleProviders",
    };

    static void register(String[] names) {
        for (String name : names) {
            try {
                NeoBus.register(Class.forName(name));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void registerClient() {
        register(CLIENT);
    }

    private Subscribers() {
    }
}

package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/** Damage types are data-driven; the JSON lives in data/steelstorm/damage_type. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BLEED = ResourceKey.create(Registries.DAMAGE_TYPE, SteelstormArsenal.id("bleed"));
    public static final ResourceKey<DamageType> ZAP = ResourceKey.create(Registries.DAMAGE_TYPE, SteelstormArsenal.id("zap"));

    public static DamageSource bleed(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLEED));
    }

    public static DamageSource zap(ServerLevel level, net.minecraft.world.entity.Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZAP), attacker);
    }

    private ModDamageTypes() {
    }
}

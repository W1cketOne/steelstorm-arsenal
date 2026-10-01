package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.fx.FxParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom particles. Sprites come from tools/gen_fx.py; client renderers are in client.particle. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, SteelstormArsenal.MODID);

    /** Big crescent slash trail, oriented by yaw/pitch/roll. */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> SLASH = register("slash", true);
    /** Flat expanding ring on the ground; scale = final radius in blocks. */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> SHOCKWAVE = register("shockwave", true);
    /** Soft glowing mote (embers, frost mist, void wisps, magic). */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> GLOW = register("glow", false);
    /** Bright metal/electric spark that falls with gravity. */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> SPARK = register("spark", false);
    /** Star-shaped flash at the point of impact. */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> IMPACT = register("impact", true);
    public static final DeferredHolder<ParticleType<?>, FxParticleType> SMOKE = register("smoke", false);
    public static final DeferredHolder<ParticleType<?>, FxParticleType> PETAL = register("petal", false);
    public static final DeferredHolder<ParticleType<?>, FxParticleType> BLOOD = register("blood", false);
    public static final DeferredHolder<ParticleType<?>, FxParticleType> RUNE = register("rune", false);
    public static final DeferredHolder<ParticleType<?>, FxParticleType> FROST = register("frost", false);
    /** A twinkling four-pointed star. */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> SPARKLE = register("sparkle", true);
    /** A glowing hollow bubble that drifts and pulses. */
    public static final DeferredHolder<ParticleType<?>, FxParticleType> ORB = register("orb", true);

    private static DeferredHolder<ParticleType<?>, FxParticleType> register(String name, boolean alwaysShow) {
        return PARTICLES.register(name, () -> new FxParticleType(alwaysShow));
    }

    private ModParticles() {
    }
}

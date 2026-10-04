package com.steelstorm.arsenal.client.particle;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.fx.FxParticleOptions;
import com.steelstorm.arsenal.registry.ModParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class ModParticleProviders {
    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SLASH.get(), sprites -> oriented(sprites, false));
        event.registerSpriteSet(ModParticles.SHOCKWAVE.get(), sprites -> oriented(sprites, true));
        event.registerSpriteSet(ModParticles.GLOW.get(), sprites -> simple(sprites, FxParticle.Kind.GLOW));
        event.registerSpriteSet(ModParticles.SPARK.get(), sprites -> simple(sprites, FxParticle.Kind.SPARK));
        event.registerSpriteSet(ModParticles.IMPACT.get(), sprites -> simple(sprites, FxParticle.Kind.IMPACT));
        event.registerSpriteSet(ModParticles.SMOKE.get(), sprites -> simple(sprites, FxParticle.Kind.SMOKE));
        event.registerSpriteSet(ModParticles.PETAL.get(), sprites -> simple(sprites, FxParticle.Kind.PETAL));
        event.registerSpriteSet(ModParticles.BLOOD.get(), sprites -> simple(sprites, FxParticle.Kind.BLOOD));
        event.registerSpriteSet(ModParticles.RUNE.get(), sprites -> simple(sprites, FxParticle.Kind.RUNE));
        event.registerSpriteSet(ModParticles.FROST.get(), sprites -> simple(sprites, FxParticle.Kind.FROST));
        event.registerSpriteSet(ModParticles.SPARKLE.get(), sprites -> simple(sprites, FxParticle.Kind.SPARKLE));
        event.registerSpriteSet(ModParticles.ORB.get(), sprites -> simple(sprites, FxParticle.Kind.ORB));
    }

    private static ParticleProvider<FxParticleOptions> simple(SpriteSet sprites, FxParticle.Kind kind) {
        return (options, level, x, y, z, vx, vy, vz) -> new FxParticle(level, x, y, z, vx, vy, vz, options, sprites, kind);
    }

    private static ParticleProvider<FxParticleOptions> oriented(SpriteSet sprites, boolean ring) {
        return (options, level, x, y, z, vx, vy, vz) -> new OrientedParticle(level, x, y, z, vx, vy, vz, options, sprites, ring);
    }

    private ModParticleProviders() {
    }
}

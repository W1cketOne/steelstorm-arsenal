package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public class RegisterParticleProvidersEvent extends Event {
    public <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> type, ParticleFactoryRegistry.PendingParticleFactory<T> factory) {
        ParticleFactoryRegistry.getInstance().register(type, factory);
    }
}

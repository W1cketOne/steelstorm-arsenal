package com.steelstorm.arsenal.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class FxParticleType extends ParticleType<FxParticleOptions> {
    private final MapCodec<FxParticleOptions> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, FxParticleOptions> streamCodec;

    public FxParticleType(boolean alwaysShow) {
        super(alwaysShow);
        this.codec = FxParticleOptions.codec(this);
        this.streamCodec = FxParticleOptions.streamCodec(this);
    }

    public FxParticleOptions with(int color, float scale) {
        return new FxParticleOptions(this, color, scale, 0, 0, 0);
    }

    public FxParticleOptions oriented(int color, float scale, float yaw, float pitch, float roll) {
        return new FxParticleOptions(this, color, scale, yaw, pitch, roll);
    }

    @Override
    public MapCodec<FxParticleOptions> codec() {
        return codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FxParticleOptions> streamCodec() {
        return streamCodec;
    }
}

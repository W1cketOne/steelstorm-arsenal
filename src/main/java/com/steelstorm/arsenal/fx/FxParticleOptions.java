package com.steelstorm.arsenal.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Options shared by every Steelstorm particle: a tint colour, a size multiplier, and an
 * orientation (used by the oriented slash and flat shockwave particles, ignored by the rest).
 */
public record FxParticleOptions(FxParticleType type, int color, float scale, float yaw, float pitch, float roll)
        implements ParticleOptions {

    public static MapCodec<FxParticleOptions> codec(FxParticleType type) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(FxParticleOptions::color),
                Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(FxParticleOptions::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(FxParticleOptions::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(FxParticleOptions::pitch),
                Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(FxParticleOptions::roll)
        ).apply(i, (c, s, y, p, r) -> new FxParticleOptions(type, c, s, y, p, r)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, FxParticleOptions> streamCodec(FxParticleType type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, FxParticleOptions::color,
                ByteBufCodecs.FLOAT, FxParticleOptions::scale,
                ByteBufCodecs.FLOAT, FxParticleOptions::yaw,
                ByteBufCodecs.FLOAT, FxParticleOptions::pitch,
                ByteBufCodecs.FLOAT, FxParticleOptions::roll,
                (c, s, y, p, r) -> new FxParticleOptions(type, c, s, y, p, r));
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    public float red() {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (color & 0xFF) / 255.0F;
    }
}

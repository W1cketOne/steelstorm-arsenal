package com.steelstorm.arsenal.client.particle;

import com.steelstorm.arsenal.fx.FxParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/** The general-purpose Steelstorm particle: behaviour depends on its {@link Kind}. */
public class FxParticle extends TextureSheetParticle {
    public enum Kind { GLOW, SPARK, SMOKE, PETAL, BLOOD, RUNE, FROST, IMPACT, SPARKLE, ORB }

    private static final int FULL_BRIGHT = 0xF000F0;
    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;
    private final float spin;

    public FxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                      FxParticleOptions options, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        setColor(options.red(), options.green(), options.blue());
        float s = options.scale() <= 0 ? 1.0F : options.scale();
        this.hasPhysics = kind == Kind.BLOOD || kind == Kind.SPARK || kind == Kind.PETAL;
        switch (kind) {
            case GLOW -> { lifetime = 14 + random.nextInt(16); gravity = -0.012F; friction = 0.9F; baseSize = 0.16F * s; }
            case SPARK -> { lifetime = 8 + random.nextInt(10); gravity = 0.9F; friction = 0.95F; baseSize = 0.07F * s; }
            case SMOKE -> { lifetime = 22 + random.nextInt(14); gravity = -0.015F; friction = 0.94F; baseSize = 0.3F * s; alpha = 0.7F; }
            case PETAL -> { lifetime = 40 + random.nextInt(30); gravity = 0.035F; friction = 0.96F; baseSize = 0.11F * s; }
            case BLOOD -> { lifetime = 18 + random.nextInt(16); gravity = 1.0F; friction = 0.98F; baseSize = 0.08F * s; }
            case RUNE -> { lifetime = 26 + random.nextInt(16); gravity = -0.02F; friction = 0.9F; baseSize = 0.22F * s; }
            case FROST -> { lifetime = 28 + random.nextInt(22); gravity = 0.02F; friction = 0.95F; baseSize = 0.1F * s; }
            case SPARKLE -> { lifetime = 12 + random.nextInt(14); gravity = -0.004F; friction = 0.88F; baseSize = 0.13F * s; }
            case ORB -> { lifetime = 20 + random.nextInt(14); gravity = -0.01F; friction = 0.92F; baseSize = 0.14F * s; }
            default -> { lifetime = 6; gravity = 0; friction = 0; baseSize = 0.5F * s; }
        }
        this.quadSize = baseSize;
        this.spin = (random.nextFloat() - 0.5F) * (kind == Kind.PETAL || kind == Kind.FROST || kind == Kind.RUNE ? 0.25F : 0);
        this.roll = random.nextFloat() * Mth.TWO_PI * (spin != 0 ? 1 : 0);
        this.oRoll = roll;
        if (kind == Kind.SMOKE || kind == Kind.IMPACT) {
            setSpriteFromAge(sprites);
        } else {
            pickSprite(sprites);
        }
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        oRoll = roll;
        roll += spin;
        switch (kind) {
            case GLOW, FROST -> { alpha = 1 - life * life; quadSize = baseSize * (1 - life * 0.6F); }
            case SPARK -> quadSize = baseSize * (1 - life);
            case SMOKE -> { setSpriteFromAge(sprites); quadSize = baseSize * (1 + life); alpha = 0.7F * (1 - life); }
            case PETAL -> { xd += Mth.sin(age * 0.2F) * 0.004; zd += Mth.cos(age * 0.17F) * 0.004; alpha = life > 0.8F ? (1 - life) * 5 : 1; }
            case BLOOD -> { if (onGround) { xd = 0; zd = 0; } alpha = life > 0.7F ? (1 - life) / 0.3F : 1; }
            case RUNE -> alpha = life < 0.2F ? life * 5 : (1 - life) / 0.8F;
            case IMPACT -> setSpriteFromAge(sprites);
            case SPARKLE -> {
                // Twinkle: flare up, shimmer, fade.
                float flare = life < 0.2F ? life / 0.2F : 1 - (life - 0.2F) / 0.8F;
                quadSize = baseSize * flare * (0.8F + 0.4F * Mth.sin(age * 1.7F));
                roll += 0.08F;
            }
            case ORB -> { alpha = life < 0.15F ? life / 0.15F : 1 - (life - 0.15F) / 0.85F; quadSize = baseSize * (1 + 0.15F * Mth.sin(age * 0.6F)); }
            default -> { }
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        return switch (kind) {
            case GLOW, SPARK, RUNE, FROST, IMPACT, SPARKLE, ORB -> FULL_BRIGHT;
            default -> super.getLightColor(partialTick);
        };
    }

    @Override
    public ParticleRenderType getRenderType() {
        return switch (kind) {
            case GLOW, SPARK, SPARKLE, ORB, FROST, RUNE -> AdditiveParticles.ADDITIVE;
            default -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        };
    }
}

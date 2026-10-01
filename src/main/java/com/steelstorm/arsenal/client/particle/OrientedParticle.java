package com.steelstorm.arsenal.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.steelstorm.arsenal.fx.FxParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Particles drawn in world orientation instead of facing the camera: the crescent slash trail
 * (rotated by yaw/pitch/roll) and the flat shockwave ring that expands along the ground.
 */
public class OrientedParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean ring;
    private final float targetSize;
    private final Quaternionf orientation;

    public OrientedParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                            FxParticleOptions options, SpriteSet sprites, boolean ring) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.ring = ring;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.8F;
        this.gravity = 0;
        this.hasPhysics = false;
        setColor(options.red(), options.green(), options.blue());
        float s = options.scale() <= 0 ? 1 : options.scale();
        if (ring) {
            this.targetSize = s;
            this.lifetime = 10 + (int) (s * 1.2F);
            this.quadSize = 0.2F;
            this.orientation = new Quaternionf().rotateX(-Mth.HALF_PI);
            pickSprite(sprites);
        } else {
            this.targetSize = 1.3F * s;
            this.lifetime = 7;
            this.quadSize = targetSize;
            this.orientation = new Quaternionf()
                    .rotateY(-options.yaw() * Mth.DEG_TO_RAD)
                    .rotateX(options.pitch() * Mth.DEG_TO_RAD)
                    .rotateZ(options.roll() * Mth.DEG_TO_RAD);
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        if (ring) {
            float eased = 1 - (1 - life) * (1 - life) * (1 - life);
            quadSize = 0.2F + (targetSize - 0.2F) * eased;
            alpha = 1 - life;
        } else {
            setSpriteFromAge(sprites);
            alpha = life < 0.5F ? 1 : 1 - (life - 0.5F) * 2;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        renderRotatedQuad(buffer, camera, orientation, partialTicks);
        // Draw the back face too so it is visible from either side.
        renderRotatedQuad(buffer, camera, new Quaternionf(orientation).rotateY(Mth.PI), partialTicks);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}

package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A flaming meteor that streaks down from the night sky and smashes into the ground, leaving a
 * crater with a smouldering meteorite full of Stormsteel ore.
 */
public class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);

    /** False for meteors summoned by Starfall: they burn and blast, but leave the ground alone. */
    public boolean crater = true;
    /** Whoever called the meteor down (it never hurts them). */
    @org.jetbrains.annotations.Nullable
    public LivingEntity owner;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.6F);
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public void setSize(float size) {
        entityData.set(SIZE, size);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        if (v.lengthSqr() < 1e-4) {
            v = new Vec3(0.6, -1.6, 0.2);
            setDeltaMovement(v);
        }
        Vec3 next = position().add(v);
        if (level().isClientSide) {
            RandomSource r = random;
            for (int i = 0; i < 4; i++) {
                Vec3 p = position().add(v.scale(-r.nextDouble()));
                level().addParticle(ParticleTypes.FLAME, p.x + r.nextGaussian() * 0.3, p.y + r.nextGaussian() * 0.3, p.z + r.nextGaussian() * 0.3,
                        0, 0.02, 0);
                level().addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 0, 0.03, 0);
            }
            level().addParticle(ModParticles.GLOW.get().with(0xFF7A1A, 2.5F * size()), getX(), getY(), getZ(), 0, 0, 0);
            setPos(next);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > 400 || getY() < level.getMinBuildHeight()) {
            discard();
            return;
        }
        BlockPos at = BlockPos.containing(next);
        if (!level.isLoaded(at)) {
            discard();
            return;
        }
        if (!level.getBlockState(at).isAir() || !level.getFluidState(at).isEmpty()) {
            impact(level, BlockPos.containing(position()));
            return;
        }
        if (tickCount % 6 == 0) {
            Fx.sound(level, position(), ModSounds.ABILITY_DASH, 2.0F, 0.4F);
        }
        setPos(next);
    }

    private void impact(ServerLevel level, BlockPos center) {
        float size = size();
        Vec3 c = Vec3.atCenterOf(center);
        int radius = Math.round(2.5F + size);
        if (crater && Config.METEOR_CRATERS.get()) {
            // Blast a crater out of natural ground only (never player builds).
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                double d = Math.sqrt(p.distSqr(center));
                if (d > radius + random.nextDouble() * 0.6) {
                    continue;
                }
                BlockState state = level.getBlockState(p);
                if (natural(state)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            // The meteorite: a scorched lump studded with Stormsteel ore around a magma core.
            BlockPos core = center.below(radius - 1);
            for (BlockPos p : BlockPos.betweenClosed(core.offset(-2, -2, -2), core.offset(2, 2, 2))) {
                double d = Math.sqrt(p.distSqr(core));
                if (d > 1.9 || !level.getBlockState(p).isAir() && !natural(level.getBlockState(p))) {
                    continue;
                }
                BlockState pick = d < 0.6 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : random.nextFloat() < 0.35F ? ModBlocks.STORMSTEEL_ORE.get().defaultBlockState()
                        : random.nextFloat() < 0.5F ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
                level.setBlock(p.immutable(), pick, 3);
            }
            for (int i = 0; i < 6; i++) {
                BlockPos f = core.offset(random.nextInt(7) - 3, 2, random.nextInt(7) - 3);
                if (level.getBlockState(f).isAir() && level.getBlockState(f.below()).isFaceSturdy(level, f.below(), net.minecraft.core.Direction.UP)) {
                    level.setBlock(f, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
        // Shockwave: everything nearby is thrown and burned.
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius + 6))) {
            if (e == owner || (owner != null && e.isAlliedTo(owner))) {
                continue;
            }
            double d = e.position().distanceTo(c);
            float dmg = (float) Math.max(0, (owner != null ? 40 : 14) - d * 1.4);
            if (dmg > 0) {
                e.hurt(owner instanceof net.minecraft.world.entity.player.Player p ? level.damageSources().explosion(this, p)
                        : level.damageSources().explosion(this, null), dmg);
                e.igniteForSeconds(4);
                Shockwaves.throwUp(e, c, 0.6);
            }
        }
        Fx.impact(level, c, 0xFF7A1A, 3.0F);
        Fx.cracks(level, c.add(0, -radius + 1, 0), 0xFF5A10, radius + 3.0F);
        Fx.halo(level, c, 0xFF7A1A, Fx.GOLD, radius + 8.0F, 20);
        Fx.sunburst(level, c, 0xFF7A1A, Fx.WHITE, radius + 6.0F, 16, 20);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 40, radius * 0.6, 1, radius * 0.6, 0.2);
        Fx.sound(level, c, ModSounds.ABILITY_EPIC_IMPACT, 6.0F, 0.7F);
        Fx.sound(level, c, ModSounds.ABILITY_LIGHTNING_STRIKE, 4.0F, 0.6F);
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(c);
            if (d < 64) {
                com.steelstorm.arsenal.combat.Stamina.shake(p, (float) Math.max(0.2, 1.2 - d / 50), 20);
            }
        }
        discard();
    }

    private static boolean natural(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(BlockTags.LEAVES) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(BlockTags.FLOWERS) || s.is(Blocks.CLAY) || s.is(Blocks.TERRACOTTA);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Size")) {
            setSize(tag.getFloat("Size"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Size", size());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }
}

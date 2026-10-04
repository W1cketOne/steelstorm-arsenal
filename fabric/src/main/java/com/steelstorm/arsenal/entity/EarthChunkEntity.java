package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A copy of a block drawn in the world for a moment: either a single ground block that jumps up
 * and falls back (crater rims, eruptions), or a shell of ice/stone wrapped around a creature that
 * shatters when it disappears. Purely visual; nothing in the world changes.
 */
public class EarthChunkEntity extends Entity implements IEntityWithComplexSpawn {
    private BlockState state = Blocks.DIRT.defaultBlockState();
    private float height;
    private int lifetime = 12;
    private boolean shell;
    private float shellWidth = 1.0F;
    private float shellHeight = 1.0F;
    private float tiltX;
    private float tiltZ;

    public EarthChunkEntity(EntityType<? extends EarthChunkEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A ground block at `ground` that jumps `height` blocks up and lands again after `lifetime` ticks. */
    public EarthChunkEntity(Level level, BlockPos ground, BlockState state, float height, int lifetime, boolean tilted) {
        this(ModEntities.EARTH_CHUNK.get(), level);
        setPos(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
        this.state = state;
        this.height = height;
        this.lifetime = lifetime;
        if (tilted) {
            this.tiltX = (random.nextFloat() - 0.5F) * 50.0F;
            this.tiltZ = (random.nextFloat() - 0.5F) * 50.0F;
        }
    }

    /** A block shell around `target` (ice for freezing, for example) that shatters after `lifetime` ticks. */
    public static EarthChunkEntity shell(Level level, LivingEntity target, BlockState state, int lifetime) {
        EarthChunkEntity chunk = new EarthChunkEntity(ModEntities.EARTH_CHUNK.get(), level);
        chunk.setPos(target.getX(), target.getY(), target.getZ());
        chunk.state = state;
        chunk.lifetime = lifetime;
        chunk.shell = true;
        chunk.shellWidth = target.getBbWidth() + 0.35F;
        chunk.shellHeight = target.getBbHeight() + 0.25F;
        level.addFreshEntity(chunk);
        return chunk;
    }

    public BlockState state() {
        return state;
    }

    public boolean isShell() {
        return shell;
    }

    public float shellWidth() {
        return shellWidth;
    }

    public float shellHeight() {
        return shellHeight;
    }

    public float tiltX() {
        return tiltX;
    }

    public float tiltZ() {
        return tiltZ;
    }

    /** Height above its ground position at this moment. */
    public float offset(float partialTick) {
        if (shell) {
            return 0;
        }
        float t = Mth.clamp((tickCount + partialTick) / lifetime, 0, 1);
        return height * 4 * t * (1 - t);
    }

    /** Shells grow in quickly so they don't just pop into existence. */
    public float growth(float partialTick) {
        return shell ? Mth.clamp((tickCount + partialTick) / 4.0F, 0.2F, 1.0F) : 1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= lifetime) {
            discard();
        }
    }

    @Override
    public void onClientRemoval() {
        if (!shell) {
            return;
        }
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        int count = (int) (24 * shellHeight);
        for (int i = 0; i < count; i++) {
            level().addParticle(particle, getX() + (random.nextDouble() - 0.5) * shellWidth, getY() + random.nextDouble() * shellHeight,
                    getZ() + (random.nextDouble() - 0.5) * shellWidth, (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.3,
                    (random.nextDouble() - 0.5) * 0.4);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(Block.getId(state));
        buf.writeFloat(height);
        buf.writeVarInt(lifetime);
        buf.writeBoolean(shell);
        buf.writeFloat(shellWidth);
        buf.writeFloat(shellHeight);
        buf.writeFloat(tiltX);
        buf.writeFloat(tiltZ);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        state = Block.stateById(buf.readVarInt());
        height = buf.readFloat();
        lifetime = buf.readVarInt();
        shell = buf.readBoolean();
        shellWidth = buf.readFloat();
        shellHeight = buf.readFloat();
        tiltX = buf.readFloat();
        tiltZ = buf.readFloat();
    }
}

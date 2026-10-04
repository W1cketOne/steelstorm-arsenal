package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A ripple of earth. As the wave front passes, a copy of each ground block jumps up and settles
 * back down, either in an expanding ring or along a line running away from the start.
 *
 * <p>The server only spawns this entity and does the damage elsewhere. Every client reads the
 * ground from its own copy of the world and animates it, so a huge shockwave costs a single
 * spawn packet. Nothing in the world is changed.</p>
 */
public class GroundWaveEntity extends Entity implements IEntityWithComplexSpawn {
    /** Ticks a block takes to jump up, and to settle back down. */
    public static final float RISE_TICKS = 2.5F;
    public static final float FALL_TICKS = 7.0F;

    private boolean line;
    private float size = 6.0F;
    private float speed = 1.0F;
    private float halfWidth = 1.0F;
    private float height = 0.8F;
    private float yaw;
    private int color = Fx.EARTH;
    @Nullable
    private List<Column> columns;

    /** One ground block that takes part in the wave, `distance` blocks from the start. */
    public record Column(BlockPos pos, float distance, int seed) {
    }

    public GroundWaveEntity(EntityType<? extends GroundWaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /** An expanding ring of rippling ground, `speed` blocks per tick out to `radius`. */
    public static GroundWaveEntity ring(ServerLevel level, Vec3 center, float radius, float speed, float height, int color) {
        GroundWaveEntity wave = new GroundWaveEntity(ModEntities.GROUND_WAVE.get(), level);
        wave.setPos(center.x, center.y, center.z);
        wave.size = radius;
        wave.speed = speed;
        wave.height = height;
        wave.color = color;
        level.addFreshEntity(wave);
        return wave;
    }

    /** A line of erupting ground `length` blocks long and `2 * halfWidth` wide. */
    public static GroundWaveEntity line(ServerLevel level, Vec3 start, Vec3 dir, float length, float halfWidth, float speed,
                                        float height, int color) {
        GroundWaveEntity wave = new GroundWaveEntity(ModEntities.GROUND_WAVE.get(), level);
        wave.setPos(start.x, start.y, start.z);
        wave.line = true;
        wave.size = length;
        wave.halfWidth = halfWidth;
        wave.speed = speed;
        wave.height = height;
        wave.color = color;
        wave.yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        level.addFreshEntity(wave);
        return wave;
    }

    public boolean isLine() {
        return line;
    }

    public float height() {
        return height;
    }

    public Vec3 direction() {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    public int lifetime() {
        return Mth.ceil(size / speed + RISE_TICKS + FALL_TICKS) + 2;
    }

    /** How far a column `distance` blocks from the start is raised `time` ticks after the wave began. */
    public float raise(float distance, float time) {
        float since = time - distance / speed;
        if (since <= 0 || since >= RISE_TICKS + FALL_TICKS) {
            return 0;
        }
        float shape;
        if (since < RISE_TICKS) {
            float x = since / RISE_TICKS;
            shape = 1 - (1 - x) * (1 - x);
        } else {
            float x = (since - RISE_TICKS) / FALL_TICKS;
            // Fall back down with a small bounce at the end.
            shape = (1 - x) * (1 - x) + 0.18F * Mth.sin(x * Mth.PI) * x;
        }
        float falloff = line ? 1.0F - 0.3F * distance / size : 1.0F - 0.5F * distance / Math.max(1, size);
        return height * shape * falloff;
    }

    /** The columns in this wave, nearest first. Computed once per client from its copy of the world. */
    public List<Column> columns() {
        if (columns == null) {
            columns = computeColumns();
        }
        return columns;
    }

    private List<Column> computeColumns() {
        List<Column> out = new ArrayList<>();
        Level level = level();
        if (!line) {
            int r = Mth.ceil(size);
            int cx = Mth.floor(getX());
            int cz = Mth.floor(getZ());
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double x = cx + dx + 0.5;
                    double z = cz + dz + 0.5;
                    double d = Math.hypot(x - getX(), z - getZ());
                    if (d > size + 0.5 || d < 0.7) {
                        continue;
                    }
                    BlockPos g = ground(level, x, getY(), z);
                    if (g != null) {
                        out.add(new Column(g, (float) d, (int) Mth.getSeed(g)));
                    }
                }
            }
        } else {
            Vec3 dir = direction();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            Set<Long> seen = new HashSet<>();
            double hintY = getY();
            for (float t = 0.5F; t <= size; t += 0.5F) {
                for (float s = -halfWidth; s <= halfWidth + 0.01F; s += 0.5F) {
                    Vec3 p = position().add(dir.scale(t)).add(side.scale(s));
                    if (!seen.add(BlockPos.asLong(Mth.floor(p.x), 0, Mth.floor(p.z)))) {
                        continue;
                    }
                    BlockPos g = ground(level, p.x, hintY, p.z);
                    if (g != null) {
                        out.add(new Column(g, t, (int) Mth.getSeed(g)));
                        if (Math.abs(s) < 0.3F) {
                            hintY = g.getY() + 1;
                        }
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble(Column::distance));
        return out;
    }

    /** The top solid full block at (x, z) within a few blocks of height y, or null. */
    @Nullable
    public static BlockPos ground(BlockGetter level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y + 2, z).mutable();
        for (int i = 0; i < 8; i++) {
            if (isSolid(level, pos) && !isSolid(level, pos.above())) {
                return pos.immutable();
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    public static boolean isSolid(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && state.getRenderShape() == RenderShape.MODEL
                && state.isCollisionShapeFullBlock(level, pos);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
        } else if (tickCount > lifetime()) {
            discard();
        }
    }

    /** Debris and dust kicked up along the wave front. */
    private void clientTick() {
        float front = tickCount * speed;
        if (front > size + 1) {
            return;
        }
        for (Column c : columns()) {
            if (c.distance() < front - speed) {
                continue;
            }
            if (c.distance() > front) {
                break;
            }
            BlockPos p = c.pos();
            if (random.nextFloat() < 0.35F) {
                BlockState state = level().getBlockState(p);
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + random.nextDouble(),
                        p.getY() + 1.05, p.getZ() + random.nextDouble(), 0, 0.25 + random.nextDouble() * 0.2, 0);
            }
            if (random.nextFloat() < 0.12F) {
                level().addParticle(ModParticles.SMOKE.get().with(0x8A8378, 1.3F), p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5,
                        0, 0.03, 0);
            }
            if (random.nextFloat() < 0.1F) {
                level().addParticle(ModParticles.GLOW.get().with(color, 1.4F), p.getX() + random.nextDouble(), p.getY() + 1.1,
                        p.getZ() + random.nextDouble(), 0, 0.06, 0);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
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
        buf.writeBoolean(line);
        buf.writeFloat(size);
        buf.writeFloat(speed);
        buf.writeFloat(halfWidth);
        buf.writeFloat(height);
        buf.writeFloat(yaw);
        buf.writeInt(color);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        line = buf.readBoolean();
        size = buf.readFloat();
        speed = buf.readFloat();
        halfWidth = buf.readFloat();
        height = buf.readFloat();
        yaw = buf.readFloat();
        color = buf.readInt();
    }
}

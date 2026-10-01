package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * Spinning blades circling their owner: they cut whatever comes close and can knock incoming
 * arrows and other projectiles out of the air. Positions are computed from the owner and the
 * blade's age on both sides, so the orbit stays perfectly smooth.
 */
public class OrbitBladesEntity extends Entity implements IEntityWithComplexSpawn {
    private int ownerId = -1;
    private int count = 4;
    private float radius = 2.5F;
    private int duration = 100;
    private int color = 0x67E8F9;
    private float spin = 14.0F;
    private boolean blockProjectiles;
    private ItemStack stack = ItemStack.EMPTY;
    // Server only.
    private float damage;
    private final Int2LongOpenHashMap lastHit = new Int2LongOpenHashMap();

    public OrbitBladesEntity(EntityType<? extends OrbitBladesEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static OrbitBladesEntity spawn(ServerPlayer owner, ItemStack stack, int count, float radius, int duration, float spin,
                                          float damage, boolean blockProjectiles, int color) {
        OrbitBladesEntity e = new OrbitBladesEntity(ModEntities.ORBIT_BLADES.get(), owner.level());
        e.ownerId = owner.getId();
        e.stack = stack.copyWithCount(1);
        e.count = count;
        e.radius = radius;
        e.duration = duration;
        e.spin = spin;
        e.damage = damage;
        e.blockProjectiles = blockProjectiles;
        e.color = color;
        e.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
        owner.level().addFreshEntity(e);
        return e;
    }

    @Nullable
    public Entity owner() {
        return ownerId < 0 ? null : level().getEntity(ownerId);
    }

    public int count() {
        return count;
    }

    public int color() {
        return color;
    }

    public ItemStack stack() {
        return stack.isEmpty() ? new ItemStack(ModItems.CHAKRAM.get()) : stack;
    }

    /** Current orbit radius: the blades fly out at the start and back in at the end. */
    public float radiusAt(float age) {
        float out = Mth.clamp(age / 6.0F, 0, 1);
        float in = Mth.clamp((duration - age) / 6.0F, 0, 1);
        return radius * (1 - (1 - out) * (1 - out)) * in;
    }

    /** Offset of blade `i` from the owner's centre. */
    public Vec3 bladeOffset(int i, float age) {
        double a = Math.toRadians(age * spin) + i * Math.PI * 2 / count;
        float r = radiusAt(age);
        return new Vec3(Math.cos(a) * r, Mth.sin(age * 0.15F + i) * 0.15F, Math.sin(a) * r);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = owner();
        if (owner != null) {
            setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
        }
        if (level().isClientSide) {
            if (owner != null) {
                for (int i = 0; i < count; i++) {
                    Vec3 p = position().add(bladeOffset(i, tickCount));
                    level().addParticle(ModParticles.GLOW.get().with(color, 1.0F), p.x, p.y, p.z, 0, 0.01, 0);
                }
            }
            return;
        }
        if (!(owner instanceof ServerPlayer player) || !player.isAlive() || player.level() != level() || tickCount > duration) {
            discard();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        long now = level.getGameTime();
        if (tickCount % 8 == 0) {
            Fx.sound(level, position(), ModSounds.WEAPON_CHAKRAM_SPIN, 0.5F, 1.2F + level.random.nextFloat() * 0.3F);
        }
        float r = radiusAt(tickCount);
        AABB area = getBoundingBox().inflate(r + 1.5, 1.5, r + 1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> CombatUtil.isEnemyOf(player, e))) {
            if (now - lastHit.getOrDefault(e.getId(), -100L) < 10) {
                continue;
            }
            Vec3 c = e.getBoundingBox().getCenter();
            for (int i = 0; i < count; i++) {
                Vec3 p = position().add(bladeOffset(i, tickCount));
                if (c.distanceToSqr(p) < Math.pow(1.0 + e.getBbWidth() / 2, 2)) {
                    lastHit.put(e.getId(), now);
                    if (CombatUtil.specialHurt(player, e, damage)) {
                        Fx.sparks(level, color, p, 4, 0.4);
                        Fx.sound(level, p, ModSounds.WEAPON_HIT, 0.6F, 1.5F);
                    }
                    break;
                }
            }
        }
        if (blockProjectiles) {
            for (Projectile p : level.getEntitiesOfClass(Projectile.class, area, p -> p.getOwner() != player && !(p instanceof SlashWaveEntity))) {
                double d = p.position().subtract(position()).horizontalDistance();
                if (d > r - 1.6 && d < r + 1.6 && Math.abs(p.getY() - getY()) < 2.0) {
                    Fx.sparks(level, 0xFFFFFF, p.position(), 8, 0.5);
                    Fx.impact(level, p.position(), color, 0.8F);
                    Fx.sound(level, p.position(), ModSounds.WEAPON_PARRY, 0.8F, 1.4F);
                    p.discard();
                }
            }
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
        buf.writeVarInt(ownerId);
        buf.writeVarInt(count);
        buf.writeFloat(radius);
        buf.writeVarInt(duration);
        buf.writeInt(color);
        buf.writeFloat(spin);
        buf.writeBoolean(blockProjectiles);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        ownerId = buf.readVarInt();
        count = buf.readVarInt();
        radius = buf.readFloat();
        duration = buf.readVarInt();
        color = buf.readInt();
        spin = buf.readFloat();
        blockProjectiles = buf.readBoolean();
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }
}

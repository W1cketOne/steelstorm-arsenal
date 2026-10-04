package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A swirling rift that drags enemies in and grinds them down: the Scythe's Grim Eclipse (which
 * floats above its caster and follows them) and Voidreaver's Event Horizon (a black hole left in
 * place that finally collapses in a blast).
 */
public class VortexEntity extends Entity implements IEntityWithComplexSpawn {
    private int ownerId = -1;
    private boolean follow;
    private float radius = 7.0F;
    private float core = 1.0F;
    private int duration = 80;
    private int color = Fx.VOID;
    private float lift = 3.0F;
    // Server only.
    private float pulseDamage;
    private float implodeDamage;
    private float healPerHit;

    public VortexEntity(EntityType<? extends VortexEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /**
     * @param follow        stay above the caster instead of staying put
     * @param pulseDamage   damage to every enemy inside every half second
     * @param implodeDamage damage when it collapses at the end (0 for none)
     * @param healPerHit    health the caster gets back per enemy hit by a pulse
     */
    public static VortexEntity spawn(ServerPlayer owner, Vec3 center, boolean follow, float radius, float core, int duration, int color,
                                     float pulseDamage, float implodeDamage, float healPerHit) {
        VortexEntity v = new VortexEntity(ModEntities.VORTEX.get(), owner.level());
        v.ownerId = owner.getId();
        v.follow = follow;
        v.radius = radius;
        v.core = core;
        v.duration = duration;
        v.color = color;
        v.lift = follow ? 3.2F : 0.0F;
        v.pulseDamage = pulseDamage;
        v.implodeDamage = implodeDamage;
        v.healPerHit = healPerHit;
        v.setPos(center.x, center.y, center.z);
        owner.level().addFreshEntity(v);
        return v;
    }

    @Nullable
    public Entity owner() {
        return ownerId < 0 ? null : level().getEntity(ownerId);
    }

    public boolean follows() {
        return follow;
    }

    public float radius() {
        return radius;
    }

    public float core() {
        return core;
    }

    public int color() {
        return color;
    }

    public float lift() {
        return lift;
    }

    public int duration() {
        return duration;
    }

    /** Grows in over half a second, shrinks away in the last few ticks. */
    public float size(float partialTick) {
        float age = tickCount + partialTick;
        float grow = Mth.clamp(age / 10.0F, 0, 1);
        float end = Mth.clamp((duration - age) / 6.0F, 0, 1);
        return (1 - (1 - grow) * (1 - grow)) * end;
    }

    /** Where enemies are pulled to: the caster when following, otherwise the rift itself. */
    public Vec3 pullCenter() {
        Entity owner = owner();
        if (follow && owner != null) {
            return owner.position().add(0, 0.5, 0);
        }
        return position();
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = owner();
        if (follow && owner != null) {
            setPos(owner.getX(), owner.getY() + lift, owner.getZ());
        }
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!(owner instanceof ServerPlayer player) || !player.isAlive() || player.level() != level()) {
            discard();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 center = pullCenter();
        for (LivingEntity e : CombatUtil.around(player, center, radius)) {
            Vec3 to = center.subtract(e.position());
            double dist = to.horizontalDistance();
            if (dist < 0.8) {
                continue;
            }
            Vec3 flat = new Vec3(to.x, 0, to.z).normalize();
            Vec3 swirl = new Vec3(-flat.z, 0, flat.x).scale(0.18);
            double strength = (follow ? 0.22 : 0.32) * Shockwaves.resist(e);
            Vec3 pull = flat.scale(strength).add(swirl);
            double lift = !follow && e.getY() < center.y - 0.5 ? 0.08 : Math.max(e.getDeltaMovement().y, -0.1);
            e.setDeltaMovement(pull.x, lift, pull.z);
            e.hurtMarked = true;
        }
        if (tickCount % 10 == 0 && tickCount < duration) {
            int hits = 0;
            for (LivingEntity e : CombatUtil.around(player, center, radius)) {
                if (CombatUtil.specialHurt(player, e, pulseDamage)) {
                    hits++;
                    Fx.burst(level, ModParticles.GLOW.get(), color, 1.3F, e.getBoundingBox().getCenter(), 6, 0.3, 0.05);
                }
            }
            if (hits > 0 && healPerHit > 0) {
                player.heal(healPerHit * hits);
                Fx.burst(level, ModParticles.GLOW.get(), 0x7CFFB0, 1.2F, player.getBoundingBox().getCenter(), 4 + hits, 0.4, 0.04);
            }
            Fx.sound(level, position(), ModSounds.ABILITY_VOID_HUM, 0.8F, 0.7F + level.random.nextFloat() * 0.2F);
        }
        if (tickCount >= duration) {
            if (implodeDamage > 0) {
                implode(player, level);
            }
            discard();
        }
    }

    private void implode(ServerPlayer player, ServerLevel level) {
        Vec3 at = position();
        for (LivingEntity e : CombatUtil.around(player, at, radius * 0.8)) {
            if (CombatUtil.specialHurt(player, e, implodeDamage)) {
                Shockwaves.throwUp(e, at, 0.7);
            }
        }
        Fx.impact(level, at, color, 4.0F);
        Fx.ring(level, at.add(0, -0.5, 0), color, radius * 1.3F);
        Fx.ring(level, at.add(0, 0.5, 0), 0xFFFFFF, radius * 0.8F);
        for (int i = 0; i < 40; i++) {
            Vec3 v = new Vec3(level.random.nextGaussian(), level.random.nextGaussian() * 0.6, level.random.nextGaussian()).normalize().scale(0.9);
            Fx.shoot(level, ModParticles.GLOW.get(), i % 3 == 0 ? 0xFFFFFF : color, 2.0F, at, v);
        }
        Fx.sound(level, at, ModSounds.ABILITY_VOID, 1.6F, 0.6F);
        Fx.sound(level, at, ModSounds.ABILITY_SHOCKWAVE, 1.4F, 0.7F);
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(at);
            if (d < 24) {
                Stamina.shake(p, (float) (1.3 * (1 - d / 28)), 14);
            }
        }
    }

    private void clientTick() {
        Vec3 c = position();
        float s = size(0);
        int count = follow ? 3 : 6;
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = (follow ? radius * 0.9 : radius) * (0.4 + random.nextDouble() * 0.6) * s;
            Vec3 p = follow ? pullCenter().add(Math.cos(a) * r, random.nextDouble() * 1.5, Math.sin(a) * r)
                    : c.add(Math.cos(a) * r, (random.nextDouble() - 0.5) * 2, Math.sin(a) * r);
            Vec3 target = follow ? pullCenter() : c;
            Vec3 v = target.subtract(p).scale(0.08).add(new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.12));
            level().addParticle(ModParticles.GLOW.get().with(random.nextInt(4) == 0 ? 0xFFFFFF : color, 1.4F), p.x, p.y, p.z, v.x, v.y, v.z);
        }
        if (follow && random.nextInt(2) == 0) {
            level().addParticle(ModParticles.SMOKE.get().with(0x1A1026, 1.2F), c.x + (random.nextDouble() - 0.5) * core * 2, c.y,
                    c.z + (random.nextDouble() - 0.5) * core * 2, 0, -0.02, 0);
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
        buf.writeVarInt(ownerId);
        buf.writeBoolean(follow);
        buf.writeFloat(radius);
        buf.writeFloat(core);
        buf.writeVarInt(duration);
        buf.writeInt(color);
        buf.writeFloat(lift);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        ownerId = buf.readVarInt();
        follow = buf.readBoolean();
        radius = buf.readFloat();
        core = buf.readFloat();
        duration = buf.readVarInt();
        color = buf.readInt();
        lift = buf.readFloat();
    }
}

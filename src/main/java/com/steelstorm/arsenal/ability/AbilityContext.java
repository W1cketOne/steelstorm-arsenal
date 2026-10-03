package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.combat.CombatData;
import com.steelstorm.arsenal.combat.CombatUtil;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.effect.BleedEffect;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Everything an ability needs: the caster, its weapon, and helpers for targeting, damage and effects. */
public final class AbilityContext {
    public final ServerPlayer player;
    public final ServerLevel level;
    public final ItemStack stack;
    @Nullable
    public final WeaponItem weapon;
    public final Ability ability;
    /** Damage of one full-strength hit with this weapon. */
    public final float baseDamage;
    /** Damage scale from charging an ultimate (1 for a tap, up to 1.75 fully charged). */
    public float power = 1.0F;

    public AbilityContext(ServerPlayer player, ItemStack stack, Ability ability, float baseDamage) {
        this.player = player;
        this.level = player.serverLevel();
        this.stack = stack;
        this.weapon = stack.getItem() instanceof WeaponItem w ? w : null;
        this.ability = ability;
        this.baseDamage = baseDamage;
    }

    /** Whether the caster is still around to finish a delayed effect. */
    public boolean alive() {
        return player.isAlive() && !player.isRemoved() && player.level() == level;
    }

    public CombatData data() {
        return Stamina.data(player);
    }

    public long now() {
        return level.getGameTime();
    }

    public Vec3 pos() {
        return player.position();
    }

    public Vec3 eye() {
        return player.getEyePosition();
    }

    public Vec3 look() {
        return player.getLookAngle();
    }

    public Vec3 flatLook() {
        Vec3 l = player.getLookAngle();
        Vec3 flat = new Vec3(l.x, 0, l.z);
        return flat.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
    }

    public Vec3 side() {
        Vec3 f = flatLook();
        return new Vec3(-f.z, 0, f.x);
    }

    /** The colour of this weapon's ability effects. */
    public int color() {
        return WeaponLooks.abilityColor(stack);
    }

    /** Somewhere the caster fits at or just around `at`, or null if it's all wall. */
    @Nullable
    public Vec3 findSpot(Vec3 at) {
        for (double dy : new double[]{0, 0.5, 1.0, -0.5, 1.5}) {
            Vec3 candidate = at.add(0, dy, 0);
            if (level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /** Teleports the caster to `to` (if they fit), optionally turning them to face `lookAt`. */
    public boolean blinkTo(Vec3 to, @Nullable Vec3 lookAt) {
        Vec3 spot = findSpot(to);
        if (spot == null) {
            return false;
        }
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (lookAt != null) {
            Vec3 d = lookAt.subtract(spot.add(0, player.getEyeHeight(), 0));
            yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            pitch = (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()));
        }
        player.teleportTo(level, spot.x, spot.y, spot.z, yaw, pitch);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        return true;
    }

    /** The spot just behind `target`, where it isn't looking. */
    public Vec3 behind(LivingEntity target, double distance) {
        Vec3 facing = Vec3.directionFromRotation(0, target.getYHeadRot());
        return target.position().subtract(facing.scale(distance + target.getBbWidth() / 2));
    }

    /** A copy of the held item, for summoning spectral copies of it. */
    public ItemStack weaponCopy() {
        return stack.copyWithCount(1);
    }

    // ------------------------------------------------------------------ targeting

    public List<LivingEntity> cone(double range, double halfAngle) {
        return CombatUtil.inCone(player, range, halfAngle);
    }

    public List<LivingEntity> around(double radius) {
        return around(player.position(), radius);
    }

    public List<LivingEntity> around(Vec3 center, double radius) {
        return CombatUtil.around(player, center, radius);
    }

    @Nullable
    public LivingEntity front(double range) {
        return CombatUtil.firstInFront(player, range);
    }

    /** The enemy under the crosshair (generous aim assist), or the nearest one in a narrow cone. */
    @Nullable
    public LivingEntity aimed(double range) {
        Vec3 start = eye();
        Vec3 end = start.add(look().scale(range));
        AABB box = player.getBoundingBox().expandTowards(look().scale(range)).inflate(1.5);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
                e -> e instanceof LivingEntity && CombatUtil.isEnemyOf(player, e) && e.getBoundingBox().inflate(0.6).clip(start, end).isPresent(),
                range * range);
        if (hit != null && hit.getEntity() instanceof LivingEntity l) {
            return l;
        }
        List<LivingEntity> c = CombatUtil.inCone(player, range, 18);
        return c.isEmpty() ? null : c.get(0);
    }

    /** Where the player is looking: the block hit, or the point at `range` dropped to the ground. */
    public Vec3 aimPoint(double range) {
        Vec3 start = eye();
        Vec3 end = start.add(look().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 p = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
        var ground = Shockwaves.ground(level, p.x, p.y, p.z);
        return ground != null ? new Vec3(p.x, ground.getY() + 1, p.z) : p;
    }

    /** Enemies inside a box running along a line. */
    public List<LivingEntity> line(Vec3 from, Vec3 dir, double length, double width) {
        Vec3 d = dir.normalize();
        Vec3 to = from.add(d.scale(length));
        AABB box = new AABB(from, to).inflate(width + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : level.getEntities(player, box, e -> CombatUtil.isEnemyOf(player, e))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = c.subtract(from).dot(d);
            if (t < -0.5 || t > length + 0.5) {
                continue;
            }
            Vec3 closest = from.add(d.scale(t));
            if (closest.distanceTo(c) <= width + e.getBbWidth() / 2) {
                out.add((LivingEntity) e);
            }
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return out;
    }

    // ------------------------------------------------------------------ damage and control

    public float dmg(float multiplier) {
        return (float) (baseDamage * multiplier * power * Config.SPECIAL_DAMAGE_MULTIPLIER.get());
    }

    /** Hits a target for `multiplier` x the weapon's damage, with impact sparks and sound. */
    public boolean hit(LivingEntity target, float multiplier) {
        return hit(target, multiplier, Fx.STEEL);
    }

    public boolean hit(LivingEntity target, float multiplier, int color) {
        boolean ok = CombatUtil.specialHurt(player, target, dmg(multiplier));
        if (ok) {
            Vec3 at = target.getBoundingBox().getCenter();
            Fx.impact(level, at, color, 0.9F);
            Fx.sparks(level, color, at, 4, 0.35);
        }
        return ok;
    }

    public void launch(LivingEntity target, double up) {
        double r = Shockwaves.resist(target);
        Vec3 m = target.getDeltaMovement();
        target.setDeltaMovement(m.x * 0.2, up * r, m.z * 0.2);
        target.hurtMarked = true;
    }

    public void knockFrom(LivingEntity target, Vec3 from, double strength, double up) {
        Vec3 away = target.position().subtract(from).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? flatLook() : away.normalize();
        double r = Shockwaves.resist(target);
        target.setDeltaMovement(away.x * strength * r, up * r, away.z * strength * r);
        target.hurtMarked = true;
    }

    public void pull(LivingEntity target, Vec3 to, double strength) {
        Vec3 dir = to.subtract(target.position());
        double dist = dir.length();
        if (dist < 0.6) {
            return;
        }
        Vec3 v = dir.normalize().scale(Math.min(strength, dist * 0.5));
        target.setDeltaMovement(v.x, Math.max(target.getDeltaMovement().y, 0.05) + v.y * 0.2, v.z);
        target.hurtMarked = true;
    }

    public void stagger(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.STAGGER, ticks, 0));
    }

    public void bleed(LivingEntity target, int ticks, int stacks) {
        for (int i = 0; i < stacks; i++) {
            BleedEffect.apply(target, ticks);
        }
        Fx.burst(level, ModParticles.BLOOD.get(), Fx.WHITE, 1.0F, target.getBoundingBox().getCenter(), 6, 0.25, 0.25, 0.25, 0.12);
    }

    public void armorBreak(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.ARMOR_BREAK, ticks, 0));
    }

    public void freeze(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.FROZEN, ticks, 0));
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        Fx.burst(level, ModParticles.FROST.get(), Fx.FROST, 1.4F, target.getBoundingBox().getCenter(), 10, 0.4, 0.5, 0.4, 0.02);
    }

    public void slow(LivingEntity target, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier));
    }

    public void pin(LivingEntity target, int ticks) {
        stagger(target, ticks);
        slow(target, ticks, 6);
        target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ movement

    /**
     * Moves the player up to `maxDistance` along `dir` without passing through walls (it steps
     * up half-blocks). Returns the enemies the player passed through.
     */
    public List<LivingEntity> dash(Vec3 dir, double maxDistance) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize();
        Vec3 start = player.position();
        Vec3 end = start;
        for (double d = 0.25; d <= maxDistance; d += 0.25) {
            Vec3 next = start.add(flat.scale(d)).add(0, end.y - start.y, 0);
            AABB box = player.getBoundingBox().move(next.subtract(player.position()));
            if (!level.noCollision(player, box)) {
                Vec3 up = next.add(0, 0.6, 0);
                if (!level.noCollision(player, player.getBoundingBox().move(up.subtract(player.position())))) {
                    break;
                }
                next = up;
            }
            end = next;
        }
        List<LivingEntity> passed = new ArrayList<>();
        if (end.distanceToSqr(start) > 0.5) {
            AABB path = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.8);
            for (Entity e : level.getEntities(player, path, e -> CombatUtil.isEnemyOf(player, e))) {
                passed.add((LivingEntity) e);
            }
            for (double d = 0; d <= start.distanceTo(end); d += 0.5) {
                Vec3 p = start.add(flat.scale(d)).add(0, 1, 0);
                Fx.burst(level, ModParticles.GLOW.get(), 0xFFFFFF, 1.0F, p, 2, 0.15, 0.4, 0.15, 0.0);
            }
            player.teleportTo(end.x, end.y, end.z);
            player.setDeltaMovement(flat.scale(0.25));
            player.hurtMarked = true;
            player.resetFallDistance();
            data().invulnerableUntil = Math.max(data().invulnerableUntil, now() + 5);
        }
        return passed;
    }

    /** Throws the player with `velocity`; `onLand` runs when they touch the ground again. */
    public void leap(Vec3 velocity, @Nullable Consumer<AbilityContext> onLand) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();
        data().noFallUntil = now() + 80;
        if (onLand != null) {
            AbilityManager.onLanding(player, 70, () -> onLand.accept(this));
        }
    }

    // ------------------------------------------------------------------ scheduling and effects

    /** Runs later, only if the player is still alive and in the same level. */
    public void later(int ticks, Runnable r) {
        ServerScheduler.schedule(ticks, () -> {
            if (player.isAlive() && player.level() == level) {
                r.run();
            }
        });
    }

    public void sound(Holder<SoundEvent> sound, float volume, float pitch) {
        Fx.sound(level, player.position(), sound, volume, pitch);
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        Fx.sound(level, player.position(), sound, volume, pitch);
    }

    public void soundAt(Vec3 at, Holder<SoundEvent> sound, float volume, float pitch) {
        Fx.sound(level, at, sound, volume, pitch);
    }

    /** A crescent slash in front of the player; roll 0 = horizontal sweep, 90 = vertical. */
    public void slash(float roll, int color, float scale, double distance) {
        Fx.slashFrom(level, player, distance, roll, color, scale);
    }

    public void slashAt(Vec3 at, float yaw, float roll, int color, float scale) {
        Fx.slash(level, at, yaw, 0, roll, color, scale);
    }

    public void shake(float strength, int ticks) {
        Stamina.shake(player, strength, ticks);
    }

    /** Shakes every player within `radius` of `at`, scaled down with distance. */
    public void shakeNearby(Vec3 at, double radius, float strength, int ticks) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(at);
            if (d <= radius) {
                Stamina.shake(p, (float) (strength * (1 - d / (radius * 1.4))), ticks);
            }
        }
    }

    public void shockwave(Vec3 center, float radius, float speed, float damageMultiplier, double launch, int color,
                          @Nullable Consumer<LivingEntity> onHit) {
        Shockwaves.ring(level, player, center, radius, speed, dmg(damageMultiplier), launch, color, onHit);
    }

    public void fissure(Vec3 start, Vec3 dir, int length, double width, float damageMultiplier, double launch, int color,
                        @Nullable Consumer<LivingEntity> onHit) {
        Shockwaves.line(level, player, start, dir, length, width, dmg(damageMultiplier), launch, color, onHit);
    }
}

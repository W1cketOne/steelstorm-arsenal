package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.weapon.WeaponItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CombatUtil {
    /** Set while a special ability deals its damage, so on-hit passives don't double-apply. */
    private static int specialDepth;

    @Nullable
    public static WeaponItem weaponOf(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem weapon ? weapon : null;
    }

    @Nullable
    public static WeaponItem heldWeapon(LivingEntity entity) {
        return weaponOf(entity.getMainHandItem());
    }

    /** True for a plain melee swing by {@code attacker} (not arrows, thorns, or specials). */
    public static boolean isDirectMelee(DamageSource source) {
        return specialDepth == 0
                && source.getEntity() instanceof LivingEntity
                && source.getDirectEntity() == source.getEntity()
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK));
    }

    public static boolean isSpecialDamage() {
        return specialDepth > 0;
    }

    /** Things a player's abilities should hit: living, attackable, not themselves or their pets. */
    public static boolean isEnemyOf(LivingEntity attacker, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == attacker || !living.isAlive() || !living.isAttackable()) {
            return false;
        }
        if (entity instanceof ArmorStand stand && stand.isMarker()) {
            return false;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwner() == attacker) {
            return false;
        }
        if (entity instanceof Player other && (other.isSpectator() || other.isCreative()
                || (attacker instanceof Player p && !p.canHarmPlayer(other)))) {
            return false;
        }
        return !attacker.isAlliedTo(entity);
    }

    /** Enemies within {@code range} whose direction is within {@code halfAngle} degrees of where the attacker looks. */
    public static List<LivingEntity> inCone(LivingEntity attacker, double range, double halfAngle) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle().multiply(1, 0, 1).normalize();
        double cos = Math.cos(Math.toRadians(halfAngle));
        List<LivingEntity> result = new ArrayList<>();
        AABB box = attacker.getBoundingBox().inflate(range, 2.0, range);
        for (Entity e : attacker.level().getEntities(attacker, box, e -> isEnemyOf(attacker, e))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            Vec3 flat = to.multiply(1, 0, 1);
            double dist = flat.length();
            if (dist > range + e.getBbWidth() / 2 || Math.abs(to.y) > 3.0) {
                continue;
            }
            if (dist < 0.8 || flat.normalize().dot(look) >= cos) {
                result.add((LivingEntity) e);
            }
        }
        result.sort(Comparator.comparingDouble(attacker::distanceToSqr));
        return result;
    }

    public static List<LivingEntity> around(LivingEntity attacker, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius, 2.5, radius);
        List<LivingEntity> result = new ArrayList<>();
        for (Entity e : attacker.level().getEntities(attacker, box, e -> isEnemyOf(attacker, e))) {
            if (e.position().distanceToSqr(center) <= radius * radius + 1) {
                result.add((LivingEntity) e);
            }
        }
        return result;
    }

    @Nullable
    public static LivingEntity firstInFront(LivingEntity attacker, double range) {
        List<LivingEntity> targets = inCone(attacker, range, 35);
        return targets.isEmpty() ? null : targets.get(0);
    }

    /** Deals special-ability damage as the attacker's melee damage, ignoring the hurt cooldown. */
    public static boolean specialHurt(LivingEntity attacker, LivingEntity target, float amount) {
        DamageSource source = attacker instanceof Player player
                ? attacker.damageSources().playerAttack(player)
                : attacker.damageSources().mobAttack(attacker);
        target.invulnerableTime = 0;
        specialDepth++;
        try {
            return target.hurt(source, amount);
        } finally {
            specialDepth--;
        }
    }

    /** Is {@code source}'s position in front of {@code target} (within its 180 degree field of view)? */
    public static boolean isInFront(LivingEntity target, Vec3 sourcePos) {
        Vec3 look = target.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        Vec3 to = sourcePos.subtract(target.position()).multiply(1, 0, 1).normalize();
        return look.dot(to) > 0.0;
    }

    /** Is the attacker standing behind the target (outside a 120 degree frontal arc)? */
    public static boolean isBehind(LivingEntity target, Entity attacker) {
        Vec3 look = Vec3.directionFromRotation(0, target.getYHeadRot()).normalize();
        Vec3 to = attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        return look.dot(to) < -0.5;
    }

    private CombatUtil() {
    }
}

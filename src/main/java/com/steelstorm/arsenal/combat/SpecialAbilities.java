package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Special moves, one per weapon type. The server is the only side that runs these: the Special
 * keybind just sends a request packet, and this class checks cooldown and stamina first.
 */
public final class SpecialAbilities {
    public static void tryActivate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        WeaponItem weapon = CombatUtil.heldWeapon(player);
        if (weapon == null) {
            return;
        }
        WeaponType type = weapon.type();
        CombatData data = Stamina.data(player);
        long now = player.level().getGameTime();
        if (now < data.specialCooldownEnd) {
            float seconds = (data.specialCooldownEnd - now) / 20.0F;
            player.displayClientMessage(Component.translatable("message.steelstorm.special_cooldown",
                    String.format(java.util.Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        float cost = (float) (type.specialStaminaCost() * Config.SPECIAL_COST_MULTIPLIER.get());
        if (!Stamina.has(player, cost)) {
            player.displayClientMessage(Component.translatable("message.steelstorm.no_stamina").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.6F, 1.4F);
            return;
        }
        if (!perform(player, weapon)) {
            player.displayClientMessage(Component.translatable("message.steelstorm.no_target").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Stamina.tryConsume(player, cost);
        int cooldown = (int) Math.round(type.specialCooldownTicks() * Config.SPECIAL_COOLDOWN_MULTIPLIER.get());
        data.specialCooldownEnd = now + cooldown;
        data.specialCooldownTotal = cooldown;
        player.swing(InteractionHand.MAIN_HAND, true);
        player.getMainHandItem().hurtAndBreak(2, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        Stamina.sync(player, true);
    }

    /** Base damage of a special: the weapon's full hit, scaled by config. */
    static float power(ServerPlayer player, WeaponItem weapon, float multiplier) {
        return (float) (weapon.attackDamage() * multiplier * Config.SPECIAL_DAMAGE_MULTIPLIER.get() * Config.WEAPON_DAMAGE_MULTIPLIER.get());
    }

    /** Runs the ability. Returns false (and costs nothing) if a single-target special has no target. */
    static boolean perform(ServerPlayer player, WeaponItem weapon) {
        return switch (weapon.type()) {
            case LONGSWORD -> risingSlash(player, weapon);
            case GREATSWORD -> groundCleave(player, weapon);
            case KATANA -> flashStep(player, weapon);
            case DUAL_DAGGERS -> flurry(player, weapon);
            case SPEAR -> impale(player, weapon);
            case WARHAMMER -> earthquake(player, weapon);
            case SCYTHE -> reap(player, weapon);
            case BATTLEAXE -> whirlwind(player, weapon);
        };
    }

    private static boolean risingSlash(ServerPlayer player, WeaponItem weapon) {
        LivingEntity target = CombatUtil.firstInFront(player, 4.0 + weapon.type().reachBonus());
        if (target == null) {
            return false;
        }
        CombatUtil.specialHurt(player, target, power(player, weapon, 1.2F));
        launch(target, 0.0, 1.0);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
        sound(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
        Stamina.shake(player, 0.6F, 6);
        return true;
    }

    private static boolean groundCleave(ServerPlayer player, WeaponItem weapon) {
        List<LivingEntity> targets = CombatUtil.inCone(player, 5.5, 45);
        float damage = power(player, weapon, 1.3F);
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        for (LivingEntity target : targets) {
            CombatUtil.specialHurt(player, target, damage);
            target.knockback(0.8, -look.x, -look.z);
        }
        // A line of shattered ground fanning out in front of the player.
        ServerLevel level = player.serverLevel();
        for (double d = 1; d <= 5.5; d += 0.75) {
            for (double side = -d * 0.7; side <= d * 0.7; side += 0.9) {
                Vec3 p = player.position().add(look.scale(d)).add(-look.z * side, 0, look.x * side);
                BlockState ground = level.getBlockState(BlockPos.containing(p.x, p.y - 0.5, p.z));
                if (!ground.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), p.x, p.y + 0.1, p.z, 4, 0.2, 0.1, 0.2, 0.15);
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX() + look.x * 2, player.getY() + 0.2, player.getZ() + look.z * 2, 1, 0, 0, 0, 0);
        sound(player, SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.4F);
        sound(player, SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
        Stamina.shake(player, 1.2F, 10);
        return true;
    }

    private static boolean flashStep(ServerPlayer player, WeaponItem weapon) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 start = player.position();
        Vec3 end = start;
        // Step forward until something solid is in the way; never pass through walls.
        for (double d = 0.25; d <= 6.0; d += 0.25) {
            Vec3 next = start.add(look.scale(d));
            AABB box = player.getBoundingBox().move(next.subtract(player.position()));
            if (!level.noCollision(player, box)) {
                Vec3 stepped = next.add(0, 0.6, 0);
                AABB up = player.getBoundingBox().move(stepped.subtract(player.position()));
                if (!level.noCollision(player, up)) {
                    break;
                }
                next = stepped;
            }
            end = next;
        }
        if (end.distanceToSqr(start) < 1.0) {
            return false;
        }
        AABB path = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.6);
        float damage = power(player, weapon, 1.1F);
        Set<LivingEntity> hit = new HashSet<>();
        for (var e : level.getEntities(player, path, e -> CombatUtil.isEnemyOf(player, e))) {
            LivingEntity target = (LivingEntity) e;
            if (hit.add(target)) {
                CombatUtil.specialHurt(player, target, damage);
                WeaponEffects.onSpecialHit(player, weapon, target, damage);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            }
        }
        for (double d = 0; d <= start.distanceTo(end); d += 0.5) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1, p.z, 2, 0.1, 0.3, 0.1, 0.0);
        }
        player.teleportTo(end.x, end.y, end.z);
        player.setDeltaMovement(look.scale(0.3));
        player.hurtMarked = true;
        player.resetFallDistance();
        Stamina.data(player).invulnerableUntil = player.level().getGameTime() + 6;
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.8F);
        sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.4F, 1.8F);
        return true;
    }

    private static boolean flurry(ServerPlayer player, WeaponItem weapon) {
        LivingEntity target = CombatUtil.firstInFront(player, 3.5);
        if (target == null) {
            return false;
        }
        float damage = power(player, weapon, 0.55F);
        for (int i = 0; i < 5; i++) {
            final int hit = i;
            ServerScheduler.schedule(i * 3, () -> {
                if (!player.isAlive() || !target.isAlive() || player.distanceToSqr(target) > 36) {
                    return;
                }
                CombatUtil.specialHurt(player, target, damage);
                WeaponEffects.onSpecialHit(player, weapon, target, damage);
                player.swing(hit % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
                player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                        target.getZ(), 6, 0.3, 0.3, 0.3, 0.2);
                sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.4F + hit * 0.12F);
            });
        }
        return true;
    }

    private static boolean impale(ServerPlayer player, WeaponItem weapon) {
        LivingEntity target = CombatUtil.firstInFront(player, 4.5 + weapon.type().reachBonus());
        if (target == null) {
            return false;
        }
        CombatUtil.specialHurt(player, target, power(player, weapon, 1.3F));
        pin(target, 60);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 6, 0.2, 0.3, 0.2, 0.1);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.2, target.getZ(), 12, 0.3, 0.1, 0.3, 0.1);
        sound(player, SoundEvents.TRIDENT_HIT, 1.0F, 0.8F);
        sound(player, SoundEvents.CHAIN_PLACE, 1.0F, 0.6F);
        Stamina.shake(player, 0.5F, 5);
        return true;
    }

    private static boolean earthquake(ServerPlayer player, WeaponItem weapon) {
        ServerLevel level = player.serverLevel();
        float damage = power(player, weapon, 1.1F);
        for (LivingEntity target : CombatUtil.around(player, player.position(), 5.5)) {
            CombatUtil.specialHurt(player, target, damage);
            WeaponEffects.onSpecialHit(player, weapon, target, damage);
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
            double resist = Math.max(0.2, 1.0 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            target.setDeltaMovement(away.x * 0.6 * resist, 0.85 * resist, away.z * 0.6 * resist);
            target.hurtMarked = true;
        }
        // Shockwave rings of broken ground.
        for (int ring = 1; ring <= 5; ring++) {
            final int r = ring;
            ServerScheduler.schedule(ring, () -> {
                for (int i = 0; i < r * 8; i++) {
                    double angle = i * Math.PI * 2 / (r * 8);
                    double x = player.getX() + Math.cos(angle) * r;
                    double z = player.getZ() + Math.sin(angle) * r;
                    BlockState ground = level.getBlockState(BlockPos.containing(x, player.getY() - 0.5, z));
                    if (!ground.isAir()) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, player.getY() + 0.1, z, 3, 0.1, 0.2, 0.1, 0.2);
                    }
                }
            });
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
        sound(player, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.7F);
        sound(player, SoundEvents.ANVIL_LAND, 0.6F, 0.5F);
        Stamina.shake(player, 1.6F, 14);
        for (var nearby : level.players()) {
            if (nearby != player && nearby.distanceToSqr(player) < 100) {
                Stamina.shake(nearby, 0.8F, 10);
            }
        }
        return true;
    }

    private static boolean reap(ServerPlayer player, WeaponItem weapon) {
        ServerLevel level = player.serverLevel();
        float damage = power(player, weapon, 1.1F);
        for (LivingEntity target : CombatUtil.around(player, player.position(), 4.0)) {
            CombatUtil.specialHurt(player, target, damage);
            WeaponEffects.onSpecialHit(player, weapon, target, damage);
            target.knockback(0.5, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
        spinParticles(level, player, 3.2, ParticleTypes.SWEEP_ATTACK);
        if (weapon instanceof LegendaryWeaponItem legendary && legendary.legendary() == LegendaryWeaponItem.Legendary.VOIDREAVER) {
            WeaponEffects.voidVortex(player);
        }
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        sound(player, SoundEvents.WITHER_SHOOT, 0.3F, 1.6F);
        Stamina.shake(player, 0.6F, 6);
        return true;
    }

    private static boolean whirlwind(ServerPlayer player, WeaponItem weapon) {
        float damage = power(player, weapon, 0.75F);
        for (int spin = 0; spin < 3; spin++) {
            final int s = spin;
            ServerScheduler.schedule(spin * 6, () -> {
                if (!player.isAlive()) {
                    return;
                }
                for (LivingEntity target : CombatUtil.around(player, player.position(), 3.5)) {
                    CombatUtil.specialHurt(player, target, damage);
                    WeaponEffects.onSpecialHit(player, weapon, target, damage);
                    target.knockback(0.35, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
                player.swing(InteractionHand.MAIN_HAND, true);
                spinParticles(player.serverLevel(), player, 2.6, ParticleTypes.SWEEP_ATTACK);
                sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F + s * 0.15F);
            });
        }
        return true;
    }

    static void spinParticles(ServerLevel level, LivingEntity center, double radius, net.minecraft.core.particles.SimpleParticleType type) {
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            level.sendParticles(type, center.getX() + Math.cos(angle) * radius, center.getY(0.5), center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    /** Pins an entity in place: stagger plus heavy slowness. */
    public static void pin(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.STAGGER, ticks, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
        target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    public static void launch(LivingEntity target, double horizontal, double up) {
        double resist = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        double scale = Math.max(0.2, 1.0 - resist);
        Vec3 m = target.getDeltaMovement();
        target.setDeltaMovement(m.x * horizontal, up * scale, m.z * horizontal);
        target.hurtMarked = true;
    }

    static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private SpecialAbilities() {
    }
}

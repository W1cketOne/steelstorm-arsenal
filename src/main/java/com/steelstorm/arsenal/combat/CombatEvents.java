package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side combat rules: dodge invulnerability, parrying and guarding, heavy attacks, combos
 * and weapon passives. All of it runs on the logical server, so it works the same in single
 * player, LAN and on dedicated servers.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class CombatEvents {
    private static final float FULL_STRENGTH = 0.9F;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stamina.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stamina.sync(player, true);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stamina.data(player).stamina = Stamina.max();
            Stamina.sync(player, true);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stamina.sync(player, true);
        }
    }

    /** Records how charged the swing was before vanilla resets the attack timer. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.STAGGER)) {
            event.setCanceled(true);
            return;
        }
        CombatData data = Stamina.data(player);
        data.lastAttackStrength = player.getAttackStrengthScale(0.5F);
        data.heavyPending = false;
        data.finisherPending = false;
        data.critPending = false;
    }

    /** Remembers vanilla crits so the katana can make them Bleed. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCrit(CriticalHitEvent event) {
        Stamina.data(event.getEntity()).critPending = event.isCriticalHit();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (target.level().isClientSide()) {
            return;
        }
        boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);

        // Dodge roll invulnerability window.
        if (!bypass && target instanceof ServerPlayer player && DodgeHandler.isInvulnerable(player)) {
            event.setCanceled(true);
            return;
        }

        // Staggered attackers can't land melee hits.
        if (CombatUtil.isDirectMelee(source) && source.getEntity() instanceof LivingEntity attacker
                && attacker.hasEffect(ModEffects.STAGGER)) {
            event.setCanceled(true);
            return;
        }

        // Guarding and parrying with a melee weapon.
        if (!bypass && target instanceof Player player && isGuarding(player) && source.getSourcePosition() != null
                && !source.is(DamageTypeTags.BYPASSES_SHIELD) && !source.is(DamageTypeTags.IS_EXPLOSION)
                && CombatUtil.isInFront(player, source.getSourcePosition())) {
            if (player.getTicksUsingItem() <= Config.PERFECT_PARRY_TICKS.get()) {
                perfectParry(player, source);
                event.setCanceled(true);
                return;
            }
            event.setAmount(guard(player, event.getAmount()));
        }

        // Attacker bonuses for Steelstorm weapons.
        if (CombatUtil.isDirectMelee(source) && source.getEntity() instanceof Player attacker) {
            WeaponItem weapon = CombatUtil.heldWeapon(attacker);
            if (weapon != null) {
                event.setAmount(applyAttackBonuses(attacker, weapon, target, event.getAmount()));
            }
        }
    }

    public static boolean isGuarding(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof WeaponItem weapon && weapon.isGuarding(player.getUseItem());
    }

    private static float applyAttackBonuses(Player attacker, WeaponItem weapon, LivingEntity target, float amount) {
        CombatData data = Stamina.data(attacker);
        long now = attacker.level().getGameTime();
        boolean full = data.lastAttackStrength >= FULL_STRENGTH;
        amount *= Config.WEAPON_DAMAGE_MULTIPLIER.get().floatValue();

        if (attacker.isShiftKeyDown() && full && Stamina.tryConsume(attacker, Config.HEAVY_ATTACK_COST.get().floatValue())) {
            amount *= Config.HEAVY_ATTACK_MULTIPLIER.get().floatValue();
            data.heavyPending = true;
        }
        int nextCombo = full ? data.comboAt(now) + 1 : 0;
        data.finisherPending = full && nextCombo % 3 == 0;
        return WeaponEffects.modifyMeleeDamage(attacker, weapon, target, amount, data);
    }

    private static void perfectParry(Player player, DamageSource source) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = source.getSourcePosition();
        Vec3 spark = player.getEyePosition().add(at.subtract(player.getEyePosition()).normalize().scale(0.8)).add(0, -0.3, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, spark.x, spark.y, spark.z, 18, 0.15, 0.15, 0.15, 0.6);
        level.sendParticles(ParticleTypes.CRIT, spark.x, spark.y, spark.z, 10, 0.1, 0.1, 0.1, 0.5);
        level.sendParticles(ParticleTypes.FLASH, spark.x, spark.y, spark.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5F, 1.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.6F);
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
            attacker.addEffect(new MobEffectInstance(ModEffects.STAGGER, 40, 0));
            attacker.knockback(0.6, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
        }
        Stamina.restore(player, 10.0F);
        Stamina.shake(player, 0.5F, 5);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.steelstorm.perfect_parry")
                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
    }

    /** Guarding after the parry window: absorb part of the hit, paid for with stamina. */
    private static float guard(Player player, float amount) {
        float absorbed = amount * Config.GUARD_DAMAGE_REDUCTION.get().floatValue();
        float cost = absorbed * Config.GUARD_COST_PER_DAMAGE.get().floatValue();
        float spent = Stamina.drain(player, cost);
        Level level = player.level();
        if (cost > 0 && spent < cost) {
            absorbed *= spent / cost;
            Item guardItem = player.getUseItem().getItem();
            player.stopUsingItem();
            player.getCooldowns().addCooldown(guardItem, 30);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.steelstorm.guard_broken")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        return amount - absorbed;
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (target.level().isClientSide() || event.getNewDamage() <= 0) {
            return;
        }
        // Getting hit breaks your own combo.
        if (target instanceof Player hurt && source.getEntity() != hurt) {
            Stamina.data(hurt).combo = 0;
        }
        if (!CombatUtil.isDirectMelee(source) || !(source.getEntity() instanceof Player attacker)) {
            return;
        }
        WeaponItem weapon = CombatUtil.heldWeapon(attacker);
        if (weapon == null) {
            return;
        }
        CombatData data = Stamina.data(attacker);
        long now = attacker.level().getGameTime();
        boolean full = data.lastAttackStrength >= FULL_STRENGTH;
        // Only well-timed swings build a combo; spam-clicking resets it.
        data.combo = full ? data.comboAt(now) + 1 : 0;
        data.lastHitTime = now;

        ServerLevel level = (ServerLevel) attacker.level();
        if (data.heavyPending) {
            data.heavyPending = false;
            Vec3 push = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
            target.knockback(0.9, -push.x, -push.z);
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 15, 0.4, 0.4, 0.4, 0.4);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.25F, 0.7F);
            Stamina.shake(attacker, 0.8F, 6);
            if (target instanceof Player victim) {
                Stamina.shake(victim, 0.6F, 6);
            }
        }
        if (data.finisherPending && weapon.type() == WeaponType.LONGSWORD) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        WeaponEffects.onMeleeHit(attacker, weapon, target, event.getNewDamage(), full, data);
        data.finisherPending = false;
        Stamina.sync((ServerPlayer) attacker, false);
    }

    private CombatEvents() {
    }
}

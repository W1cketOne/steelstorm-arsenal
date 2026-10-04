package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.AbilityManager;
import com.steelstorm.arsenal.ability.sets.LongswordAbilities;
import com.steelstorm.arsenal.entity.ChakramEntity;
import com.steelstorm.arsenal.entity.ThrowingKnifeEntity;
import com.steelstorm.arsenal.entity.ThrownSpear;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side combat rules: dodge invulnerability, parrying and guarding, heavy attacks, combos,
 * weapon passives, ability states (riposte, backstab, berserk...) and the ultimate meter. All of
 * it runs on the logical server, so it works the same in single player, LAN and on dedicated servers.
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
            Stamina.data(player).forceSync();
            Stamina.sync(player, true);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatData data = Stamina.data(player);
            data.stamina = Stamina.max();
            data.empoweredHits = 0;
            data.backstabUntil = 0;
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
        if (player.hasEffect(ModEffects.STAGGER) || player.hasEffect(ModEffects.FROZEN)) {
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

        // Dodge rolls and blink abilities make you briefly untouchable.
        if (!bypass && target instanceof ServerPlayer player && DodgeHandler.isInvulnerable(player)) {
            event.setCanceled(true);
            return;
        }

        // Staggered or frozen attackers can't land melee hits.
        if (CombatUtil.isDirectMelee(source) && source.getEntity() instanceof LivingEntity attacker
                && (attacker.hasEffect(ModEffects.STAGGER) || attacker.hasEffect(ModEffects.FROZEN))) {
            event.setCanceled(true);
            return;
        }

        // Longsword Riposte stance: turn the attack aside and strike back.
        if (!bypass && target instanceof ServerPlayer player && player.hasEffect(ModEffects.RIPOSTE)
                && source.getEntity() instanceof LivingEntity attacker && attacker != player
                && !source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
            LongswordAbilities.counter(player, attacker);
            return;
        }

        // Greatsword Titan's Guard: soak up three quarters of every blow, to be released later.
        if (!bypass && target instanceof ServerPlayer player) {
            CombatData guard = Stamina.data(player);
            if (guard.titanGuardUntil > player.level().getGameTime()) {
                float soaked = event.getAmount() * 0.75F;
                guard.titanStored += soaked;
                event.setAmount(event.getAmount() - soaked);
                Fx.sparks(player.serverLevel(), WeaponLooks.abilityColor(player.getMainHandItem()), player.position().add(0, 1.2, 0), 8, 0.5);
            }
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

        // Marked enemies (Soul Harvest) take extra damage from players.
        if (target.hasEffect(ModEffects.MARKED) && source.getEntity() instanceof Player) {
            event.setAmount(event.getAmount() * 1.15F);
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
        ServerLevel level = (ServerLevel) attacker.level();

        if (attacker.isShiftKeyDown() && full && Stamina.tryConsume(attacker, Config.HEAVY_ATTACK_COST.get().floatValue())) {
            amount *= Config.HEAVY_ATTACK_MULTIPLIER.get().floatValue();
            data.heavyPending = true;
        }
        // Shadowstep: the first hit after stepping behind someone is a backstab.
        if (now < data.backstabUntil) {
            data.backstabUntil = 0;
            amount *= 2.5F;
            Vec3 at = target.getBoundingBox().getCenter();
            Fx.slash(level, at, attacker.getYRot(), 0, 60, 0xA78BFA, 1.0F);
            Fx.burst(level, ModParticles.BLOOD.get(), 0xFFFFFF, 1.3F, at, 14, 0.3, 0.2);
            Fx.sound(level, at, ModSounds.ABILITY_BLOOD, 1.0F, 1.2F);
            attacker.displayClientMessage(Component.translatable("message.steelstorm.backstab").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        // Death Blossom: a few empowered strikes.
        if (data.empoweredHits > 0) {
            data.empoweredHits--;
            amount *= data.empoweredMultiplier;
            Vec3 at = target.getBoundingBox().getCenter();
            Fx.slash(level, at, attacker.getYRot(), 0, 45, 0xA78BFA, 1.1F);
            Fx.slash(level, at, attacker.getYRot(), 0, -45, 0x3A2E5C, 1.1F);
            Fx.burst(level, ModParticles.PETAL.get(), 0xA78BFA, 1.3F, at, 12, 0.4, 0.15);
            Fx.sound(level, at, ModSounds.ABILITY_SMOKE, 0.8F, 1.5F);
            if (data.empoweredHits == 0) {
                attacker.removeEffect(ModEffects.SHADOW_VEIL);
            }
        }
        int nextCombo = full ? data.comboAt(now) + 1 : 0;
        data.finisherPending = full && nextCombo % 3 == 0;
        return WeaponEffects.modifyMeleeDamage(attacker, weapon, target, amount, data);
    }

    private static void perfectParry(Player player, DamageSource source) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = source.getSourcePosition();
        Vec3 spark = player.getEyePosition().add(at.subtract(player.getEyePosition()).normalize().scale(0.8)).add(0, -0.3, 0);
        Fx.impact(level, spark, Fx.GOLD, 1.3F);
        Fx.sparks(level, Fx.GOLD, spark, 18, 0.7);
        Fx.sparks(level, Fx.WHITE, spark, 8, 0.5);
        Fx.sound(level, player.position(), ModSounds.WEAPON_PERFECT_PARRY, 1.2F, 1.0F);
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
            attacker.addEffect(new MobEffectInstance(ModEffects.STAGGER, 40, 0));
            attacker.knockback(0.6, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
        }
        Stamina.restore(player, 10.0F);
        Stamina.shake(player, 0.5F, 5);
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityManager.addUltimate(serverPlayer, 15);
        }
        player.displayClientMessage(Component.translatable("message.steelstorm.perfect_parry").withStyle(ChatFormatting.GOLD), true);
    }

    /** Guarding after the parry window: absorb part of the hit, paid for with stamina. */
    private static float guard(Player player, float amount) {
        float absorbed = amount * Config.GUARD_DAMAGE_REDUCTION.get().floatValue();
        float cost = absorbed * Config.GUARD_COST_PER_DAMAGE.get().floatValue();
        float spent = Stamina.drain(player, cost);
        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.7)).add(0, -0.3, 0);
        if (cost > 0 && spent < cost) {
            absorbed *= spent / cost;
            Item guardItem = player.getUseItem().getItem();
            player.stopUsingItem();
            player.getCooldowns().addCooldown(guardItem, 30);
            Fx.sound(level, player.position(), ModSounds.WEAPON_GUARD_BREAK, 1.1F, 1.0F);
            Fx.sparks(level, 0xFF6B6B, at, 12, 0.6);
            player.displayClientMessage(Component.translatable("message.steelstorm.guard_broken").withStyle(ChatFormatting.RED), true);
        } else {
            Fx.sound(level, player.position(), ModSounds.WEAPON_PARRY, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
            Fx.sparks(level, Fx.STEEL, at, 6, 0.4);
        }
        return amount - absorbed;
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();
        if (target.level().isClientSide() || damage <= 0) {
            return;
        }
        float charge = Config.ULTIMATE_CHARGE_MULTIPLIER.get().floatValue();
        // Getting hit breaks your own combo but builds your ultimate.
        if (target instanceof ServerPlayer hurt && source.getEntity() != hurt) {
            Stamina.data(hurt).combo = 0;
            AbilityManager.addUltimate(hurt, damage * 0.5F * charge);
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == target) {
            return;
        }
        boolean melee = CombatUtil.isDirectMelee(source);
        boolean crit = melee && attacker.fallDistance > 0 && !attacker.onGround();
        net.minecraft.world.phys.Vec3 toward = attacker.position().subtract(target.position()).multiply(1, 0, 1);
        toward = toward.lengthSqr() < 1e-4 ? net.minecraft.world.phys.Vec3.ZERO : toward.normalize().scale(target.getBbWidth() * 0.6 + 0.2);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(attacker, new com.steelstorm.arsenal.network.DamageNumberPayload(
                (float) (target.getX() + toward.x), (float) (target.getY() + target.getBbHeight() * 0.9), (float) (target.getZ() + toward.z), damage,
                crit ? 1 : (melee ? 0 : 2)));
        // Soul Harvest marks heal whoever hits them.
        if (target.hasEffect(ModEffects.MARKED) && attacker.getHealth() < attacker.getMaxHealth()) {
            attacker.heal(1.5F);
            Fx.shoot((ServerLevel) attacker.level(), ModParticles.GLOW.get(), 0xC084FC, 1.3F, target.getBoundingBox().getCenter(),
                    attacker.getBoundingBox().getCenter().subtract(target.getBoundingBox().getCenter()).scale(0.1));
        }
        if (CombatUtil.isSpecialDamage()) {
            WeaponItem weapon = CombatUtil.heldWeapon(attacker);
            if (weapon != null) {
                WeaponEffects.onSpecialHit(attacker, weapon, target, damage);
            }
            AbilityManager.addUltimate(attacker, Math.max(10.0F, damage * 0.4F) * charge);
            return;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof ThrowingKnifeEntity || direct instanceof ChakramEntity || direct instanceof ThrownSpear) {
            AbilityManager.addUltimate(attacker, damage * 0.6F * charge);
            return;
        }
        if (!CombatUtil.isDirectMelee(source)) {
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
        // Three solid hits fill the ultimate.
        AbilityManager.addUltimate(attacker, (full ? 40.0F : 15.0F) * charge);

        ServerLevel level = (ServerLevel) attacker.level();
        Vec3 at = target.getBoundingBox().getCenter();
        int color = WeaponLooks.trailColor(attacker.getMainHandItem());
        boolean armored = target.getArmorValue() >= 8;
        Fx.sound(level, at, armored ? ModSounds.WEAPON_HIT_METAL : ModSounds.WEAPON_HIT, full ? 1.0F : 0.6F,
                0.9F + level.random.nextFloat() * 0.2F);
        if (full) {
            Fx.impact(level, at, color, 0.8F);
            Fx.sparks(level, armored ? 0xFFD166 : color, at, armored ? 6 : 3, 0.35);
        }
        if (data.heavyPending) {
            data.heavyPending = false;
            Vec3 push = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
            target.knockback(0.9, -push.x, -push.z);
            Fx.impact(level, at, Fx.WHITE, 1.6F);
            Fx.sparks(level, color, at, 12, 0.6);
            Fx.burst(level, ModParticles.SMOKE.get(), 0x8A8378, 1.2F, at, 6, 0.3, 0.03);
            Fx.sound(level, at, ModSounds.WEAPON_SWING_HEAVY, 0.8F, 0.6F);
            Fx.sound(level, at, ModSounds.ABILITY_SHOCKWAVE, 0.5F, 1.6F);
            Stamina.shake(attacker, 0.8F, 6);
            if (target instanceof ServerPlayer victim) {
                Stamina.shake(victim, 0.6F, 6);
            }
        }
        if (data.finisherPending && weapon.type() == WeaponType.LONGSWORD) {
            Fx.slash(level, at, attacker.getYRot(), 0, 30, Fx.GOLD, 1.0F);
            Fx.sparks(level, Fx.GOLD, at, 10, 0.5);
            Fx.sound(level, at, ModSounds.WEAPON_HIT_METAL, 1.0F, 1.4F);
        }
        if (attacker.hasEffect(ModEffects.BERSERK)) {
            berserkHit(attacker, target, damage);
        }
        WeaponEffects.onMeleeHit(attacker, weapon, target, damage, full, data);
        data.finisherPending = false;
        Stamina.sync(attacker, false);
    }

    /** Berserker Rage: every blow heals and bursts into a wave of blood around the target. */
    private static void berserkHit(ServerPlayer attacker, LivingEntity target, float damage) {
        ServerLevel level = attacker.serverLevel();
        attacker.heal(damage * 0.15F);
        Vec3 at = target.getBoundingBox().getCenter();
        Fx.burst(level, ModParticles.BLOOD.get(), 0xFFFFFF, 1.3F, at, 12, 0.4, 0.2);
        Fx.ring(level, target.position(), 0xD0182C, 2.8F);
        for (LivingEntity other : CombatUtil.around(attacker, target.position(), 2.8)) {
            if (other != target) {
                CombatUtil.specialHurt(attacker, other, damage * 0.4F);
            }
        }
        Fx.sound(level, at, ModSounds.ABILITY_BLOOD, 0.7F, 0.9F);
    }

    /** Berserkers shrug off Stagger. */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect().is(ModEffects.STAGGER) && event.getEntity().hasEffect(ModEffects.BERSERK)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Monsters can't pick a Shadow Veiled player as a target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && player.hasEffect(ModEffects.SHADOW_VEIL)) {
            event.setCanceled(true);
        }
    }

    private CombatEvents() {
    }
}

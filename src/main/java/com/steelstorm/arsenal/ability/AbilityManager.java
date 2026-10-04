package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.anim.CastPose;
import com.steelstorm.arsenal.network.PlayerAnimPayload;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.combat.CombatData;
import com.steelstorm.arsenal.combat.ServerScheduler;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import com.steelstorm.arsenal.registry.ModParticles;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Runs abilities requested by the keybinds, after checking cooldown, stamina and ultimate charge. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class AbilityManager {
    private record Landing(Runnable action, long expires, boolean[] airborne) {
    }

    private record Charge(long start, String setId) {
    }

    private static final Map<UUID, Landing> LANDINGS = new HashMap<>();
    private static final Map<UUID, Charge> CHARGING = new HashMap<>();
    /** Ticks of holding the ultimate key for a full charge, and the bonus damage it gives. */
    public static final int FULL_CHARGE = 40;
    public static final float MAX_POWER_BONUS = 0.75F;
    private static final int CHARGE_GAP = 6;

    public static void tryActivate(ServerPlayer player, int slot) {
        tryActivate(player, slot, 1.0F);
    }

    /** Whether the ultimate could be used right now; shows why not otherwise. */
    private static boolean ultimateReady(ServerPlayer player, Ability ability) {
        CombatData data = Stamina.data(player);
        long left = data.cooldownLeft(ability.id(), player.level().getGameTime());
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.steelstorm.ability_cooldown",
                    Component.translatable(ability.nameKey()), String.format(java.util.Locale.ROOT, "%.1f", left / 20.0F))
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (data.ultimate < CombatData.ULTIMATE_MAX && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.steelstorm.ultimate_not_ready", (int) data.ultimate)
                    .withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        return true;
    }

    /** The ultimate key went down: start charging (released by {@link #releaseCharge}). */
    public static void startCharge(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.hasEffect(ModEffects.STAGGER) || CHARGING.containsKey(player.getUUID())) {
            return;
        }
        AbilitySet set = Abilities.forStack(player.getMainHandItem());
        if (set == null || !ultimateReady(player, set.get(3))) {
            return;
        }
        CHARGING.put(player.getUUID(), new Charge(player.level().getGameTime(), set.id()));
        PlayerAnimPayload.send(player, CastPose.STANCE);
        Fx.sound(player.level(), player.position(), ModSounds.ABILITY_CAST, 0.8F, 0.6F);
        player.displayClientMessage(Component.translatable("message.steelstorm.charging").withStyle(ChatFormatting.GOLD), true);
    }

    /** The ultimate key came up: unleash it, stronger the longer it was held. */
    public static void releaseCharge(ServerPlayer player) {
        Charge charge = CHARGING.remove(player.getUUID());
        if (charge == null) {
            return;
        }
        float frac = chargeFraction(player, charge);
        if (frac >= 1.0F) {
            player.displayClientMessage(Component.translatable("message.steelstorm.overcharged").withStyle(ChatFormatting.GOLD,
                    ChatFormatting.BOLD), true);
        }
        tryActivate(player, 3, 1.0F + MAX_POWER_BONUS * frac);
    }

    private static float chargeFraction(ServerPlayer player, Charge charge) {
        return Math.min(1.0F, (player.level().getGameTime() - charge.start()) / (float) FULL_CHARGE);
    }

    private static void tickCharge(ServerPlayer player) {
        Charge charge = CHARGING.get(player.getUUID());
        if (charge == null) {
            return;
        }
        AbilitySet set = Abilities.forStack(player.getMainHandItem());
        if (!player.isAlive() || set == null || !set.id().equals(charge.setId())) {
            CHARGING.remove(player.getUUID());
            return;
        }
        long held = player.level().getGameTime() - charge.start();
        if (held > FULL_CHARGE + 60) {
            releaseCharge(player);
            return;
        }
        float frac = chargeFraction(player, charge);
        ServerLevel level = player.serverLevel();
        int color = WeaponLooks.abilityColor(player.getMainHandItem());
        Vec3 c = player.position().add(0, 1.0, 0);
        // Motes spiral in from a shrinking ring while the charge builds.
        double radius = 3.2 - 2.2 * frac;
        for (int i = 0; i < 3; i++) {
            double a = held * 0.45 + i * (Math.PI * 2 / 3);
            Vec3 at = c.add(Math.cos(a) * radius, (i - 1) * 0.5 + Math.sin(held * 0.3) * 0.3, Math.sin(a) * radius);
            Fx.burst(level, ModParticles.GLOW.get(), i == 0 ? Fx.GOLD : color, 1.0F + frac, at, 1, 0.02, 0.0);
        }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 4, 1,
                true, false, false));
        if (held % 8 == 0 && held <= FULL_CHARGE) {
            // The caster hears a rising charge sound on their own client; everyone else gets these pulses.
            Fx.soundForOthers(player, ModSounds.ABILITY_CAST, 0.5F + frac * 0.4F, 0.6F + frac * 1.0F);
        }
        if (held == FULL_CHARGE) {
            Fx.sound(level, player.position(), ModSounds.ULTIMATE_READY, 1.0F, 1.3F);
            Fx.halo(level, player.position(), Fx.GOLD, color, 3.0F, 14);
            Fx.sparkles(level, c, Fx.GOLD, 20, 0.8);
            Stamina.shake(player, 0.4F, 6);
        } else if (held > FULL_CHARGE && held % 6 == 0) {
            Fx.sparkles(level, c, Fx.GOLD, 4, 0.7);
        }
    }

    public static void tryActivate(ServerPlayer player, int slot, float power) {
        if (!player.isAlive() || player.isSpectator() || player.hasEffect(ModEffects.STAGGER)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        AbilitySet set = Abilities.forStack(stack);
        if (set == null) {
            return;
        }
        Ability ability = set.get(slot);
        if (ability == null) {
            return;
        }
        CombatData data = Stamina.data(player);
        long now = player.level().getGameTime();
        long left = data.cooldownLeft(ability.id(), now);
        boolean creative = player.getAbilities().instabuild;
        boolean charged = ability.charges() > 1 && !ability.isUltimate();
        if (charged) {
            if (now < data.nextUse.getOrDefault(ability.id(), 0L)) {
                return;
            }
            if (data.chargesLeft(ability, now) <= 0) {
                left = data.cooldownLeft(ability.id(), now);
                player.displayClientMessage(Component.translatable("message.steelstorm.ability_cooldown",
                        Component.translatable(ability.nameKey()), String.format(java.util.Locale.ROOT, "%.1f", left / 20.0F))
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }
        } else if (left > 0) {
            player.displayClientMessage(Component.translatable("message.steelstorm.ability_cooldown",
                    Component.translatable(ability.nameKey()), String.format(java.util.Locale.ROOT, "%.1f", left / 20.0F))
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        float cost = (float) (ability.staminaCost() * Config.SPECIAL_COST_MULTIPLIER.get());
        if (ability.isUltimate()) {
            if (data.ultimate < CombatData.ULTIMATE_MAX && !creative) {
                player.displayClientMessage(Component.translatable("message.steelstorm.ultimate_not_ready", (int) data.ultimate)
                        .withStyle(ChatFormatting.GOLD), true);
                return;
            }
        } else if (!Stamina.has(player, cost)) {
            player.displayClientMessage(Component.translatable("message.steelstorm.no_stamina").withStyle(ChatFormatting.RED), true);
            Fx.sound(player.level(), player.position(), net.minecraft.sounds.SoundEvents.PLAYER_BREATH, 0.6F, 1.4F);
            data.forceSync();
            return;
        }
        AbilityContext ctx = new AbilityContext(player, stack, ability, baseDamage(stack));
        // Ultimates hit three times as hard as their base design.
        ctx.power = ability.isUltimate() ? power * 3.0F : power;
        if (ability.isUltimate()) {
            data.ultimateLockUntil = now + 120;
        }
        if (!ability.run(ctx)) {
            player.displayClientMessage(Component.translatable("message.steelstorm.no_target").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        AbilitySounds.play(ctx, power);
        if (ability.isUltimate()) {
            if (!creative) {
                data.ultimate = 0;
            }
            Fx.ring(player.serverLevel(), player.position(), Fx.GOLD, 4.0F);
            // The ultimate flare: a magic circle underfoot, a pillar of light and orbs whirling round.
            int c = ctx.color();
            ServerLevel level = player.serverLevel();
            Fx.circle(level, player.position(), player, c, Fx.GOLD, 3.2F, 40);
            Fx.pillar(level, player.position(), c, Fx.WHITE, 0.9F, 9.0F, 24);
            Fx.orbit(level, player.position(), player, c, Fx.lighten(c), 10, 1.7F, 1.0F, 50);
            Fx.halo(level, player.position(), Fx.GOLD, c, 7.0F * power, 22);
            Fx.sparkles(level, player.position().add(0, 1, 0), c, (int) (30 * power), 1.2);
            if (power > 1.5F) {
                // A fully charged release: a second, bigger shell of light and a heavier thump.
                Fx.sunburst(level, player.position().add(0, 0.2, 0), Fx.GOLD, c, 9.0F, 14, 18);
                Fx.ring(level, player.position(), c, 8.0F);
                Fx.sound(level, player.position(), ModSounds.ABILITY_SHOCKWAVE, 1.2F, 0.7F);
            }
            Stamina.shake(player, 0.8F + 0.4F * power, 10);
            nova(ctx, power);
        } else {
            Stamina.tryConsume(player, cost);
            Fx.sparkles(player.serverLevel(), player.position().add(0, 1.1, 0), ctx.color(), 8, 0.5);
        }
        int cooldown = (int) Math.round(ability.cooldown() * Config.SPECIAL_COOLDOWN_MULTIPLIER.get());
        if (charged) {
            int used = data.chargesUsed.getOrDefault(ability.id(), 0) + 1;
            data.chargesUsed.put(ability.id(), used);
            if (used == 1) {
                data.cooldownEnd.put(ability.id(), now + cooldown);
                data.cooldownTotal.put(ability.id(), Math.max(1, cooldown));
            }
            data.nextUse.put(ability.id(), now + CHARGE_GAP);
        } else {
            data.cooldownEnd.put(ability.id(), now + cooldown);
            data.cooldownTotal.put(ability.id(), Math.max(1, cooldown));
        }
        player.displayClientMessage(Component.translatable(ability.nameKey())
                .withStyle(ability.isUltimate() ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        player.swing(InteractionHand.MAIN_HAND, true);
        PlayerAnimPayload.send(player, CastPose.forAbility(ability.id()));
        if (!creative && stack.isDamageableItem()) {
            stack.hurtAndBreak(ability.isUltimate() ? 5 : 2, player, EquipmentSlot.MAINHAND);
        }
        Stamina.sync(player, true);
    }

    /**
     * Every ultimate ends in a Nova: a huge shockwave, lightning called down on the nearest enemies,
     * and a few seconds of Overdrive (strength and speed) for the caster.
     */
    private static void nova(AbilityContext ctx, float power) {
        ServerPlayer player = ctx.player;
        ServerLevel level = ctx.level;
        int c = ctx.color();
        ServerScheduler.schedule(14, () -> {
            if (!ctx.alive()) {
                return;
            }
            Vec3 at = player.position();
            Shockwaves.ring(level, player, at, 12.0F * Math.min(1.5F, power), 1.2F, ctx.dmg(2.0F), 0.9, c, null);
            Fx.sunburst(level, at.add(0, 0.3, 0), c, Fx.WHITE, 12.0F, 20, 22);
            Fx.sound(level, at, ModSounds.ABILITY_EPIC_IMPACT, 2.0F, 0.6F);
            int struck = 0;
            for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    player.getBoundingBox().inflate(16), e -> Shockwaves.canHit(player, e))) {
                if (struck++ >= 6) {
                    break;
                }
                net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(e.getX(), e.getY(), e.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                e.invulnerableTime = 0;
                com.steelstorm.arsenal.combat.CombatUtil.specialHurt(player, e, ctx.dmg(1.5F));
            }
        });
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 140, 1, false, true, true));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 140, 1, false, true, true));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 140, 1, false, true, true));
        Fx.orbit(level, player.position(), player, Fx.GOLD, c, 8, 1.4F, 1.0F, 140);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("OVERDRIVE!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
    }

    /** Damage of one full hit with whatever the player holds (weapons, thrown weapons, or fists). */
    public static float baseDamage(ItemStack stack) {
        float mult = Config.WEAPON_DAMAGE_MULTIPLIER.get().floatValue();
        if (stack.getItem() instanceof WeaponItem weapon) {
            return weapon.attackDamage() * mult;
        }
        if (stack.getItem() instanceof com.steelstorm.arsenal.weapon.ChakramItem) {
            return 6.0F * mult;
        }
        if (stack.getItem() instanceof com.steelstorm.arsenal.weapon.ThrowingKnifeItem) {
            return 4.5F * mult;
        }
        return 2.0F * mult;
    }

    /** Adds ultimate charge, announcing it once the meter fills up. */
    public static void addUltimate(ServerPlayer player, float amount) {
        CombatData data = Stamina.data(player);
        if (amount <= 0 || player.level().getGameTime() < data.ultimateLockUntil || data.ultimate >= CombatData.ULTIMATE_MAX) {
            return;
        }
        data.ultimate = Math.min(CombatData.ULTIMATE_MAX, data.ultimate + amount);
        if (data.ultimate >= CombatData.ULTIMATE_MAX) {
            player.playNotifySound(ModSounds.ULTIMATE_READY.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            player.displayClientMessage(Component.translatable("message.steelstorm.ultimate_ready").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        }
    }

    /** Runs `action` when the player next lands (or after `timeoutTicks`). */
    public static void onLanding(ServerPlayer player, int timeoutTicks, Runnable action) {
        LANDINGS.put(player.getUUID(), new Landing(action, player.level().getGameTime() + timeoutTicks, new boolean[]{false}));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        tickCharge(player);
        Landing landing = LANDINGS.get(player.getUUID());
        if (landing == null) {
            return;
        }
        if (!player.onGround()) {
            landing.airborne()[0] = true;
        }
        boolean landed = landing.airborne()[0] && player.onGround();
        if (landed || player.level().getGameTime() >= landing.expires() || !player.isAlive()) {
            LANDINGS.remove(player.getUUID());
            if (player.isAlive()) {
                landing.action().run();
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getGameTime() < Stamina.data(player).noFallUntil) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        LANDINGS.clear();
        CHARGING.clear();
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LANDINGS.remove(event.getEntity().getUUID());
        CHARGING.remove(event.getEntity().getUUID());
    }

    private AbilityManager() {
    }
}

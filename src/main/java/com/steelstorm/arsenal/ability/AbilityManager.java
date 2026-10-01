package com.steelstorm.arsenal.ability;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.combat.CombatData;
import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEffects;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

    private static final Map<UUID, Landing> LANDINGS = new HashMap<>();

    public static void tryActivate(ServerPlayer player, int slot) {
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
        if (left > 0) {
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
        if (ability.isUltimate()) {
            data.ultimateLockUntil = now + 120;
        }
        if (!ability.run(ctx)) {
            player.displayClientMessage(Component.translatable("message.steelstorm.no_target").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (ability.isUltimate()) {
            if (!creative) {
                data.ultimate = 0;
            }
            Fx.sound(player.level(), player.position(), ModSounds.ULTIMATE_CAST, 1.2F, 1.0F);
            Fx.ring(player.serverLevel(), player.position(), Fx.GOLD, 4.0F);
            Stamina.shake(player, 1.0F, 10);
        } else {
            Stamina.tryConsume(player, cost);
        }
        int cooldown = (int) Math.round(ability.cooldown() * Config.SPECIAL_COOLDOWN_MULTIPLIER.get());
        data.cooldownEnd.put(ability.id(), now + cooldown);
        data.cooldownTotal.put(ability.id(), Math.max(1, cooldown));
        player.displayClientMessage(Component.translatable(ability.nameKey())
                .withStyle(ability.isUltimate() ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        player.swing(InteractionHand.MAIN_HAND, true);
        if (!creative && stack.isDamageableItem()) {
            stack.hurtAndBreak(ability.isUltimate() ? 5 : 2, player, EquipmentSlot.MAINHAND);
        }
        Stamina.sync(player, true);
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
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LANDINGS.remove(event.getEntity().getUUID());
    }

    private AbilityManager() {
    }
}

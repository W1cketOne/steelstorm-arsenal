package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Abilities;
import com.steelstorm.arsenal.network.AbilityPayload;
import com.steelstorm.arsenal.network.DodgePayload;
import com.steelstorm.arsenal.network.SwingPayload;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.ClientPlayerNetworkEvent;
import com.steelstorm.compat.neo.neoforge.client.event.ClientTickEvent;
import com.steelstorm.compat.neo.neoforge.client.event.InputEvent;
import com.steelstorm.compat.neo.neoforge.client.event.ViewportEvent;
import com.steelstorm.compat.neo.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Alternating swing directions so consecutive slashes don't look identical. */
    private static final float[] SWING_ROLLS = {-25, 200, 15, 160};
    private static int swingIndex;
    @org.jetbrains.annotations.Nullable
    private static net.minecraft.client.resources.sounds.SoundInstance chargeSound;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ClientCombatState.tick();
        // Keys only send requests; the server checks stamina, cooldowns and charge.
        while (ModKeyMappings.INSPECT.consumeClick()) {
            if (mc.screen == null && player.getMainHandItem().getItem() instanceof com.steelstorm.arsenal.weapon.WeaponItem
                    && com.steelstorm.arsenal.client.anim.ClientAnims.get(player, 0) == null) {
                com.steelstorm.arsenal.client.anim.ClientAnims.start(player.getId(), com.steelstorm.arsenal.anim.CastPose.INSPECT,
                        com.steelstorm.arsenal.anim.CastPose.INSPECT.duration);
            }
        }
        while (ModKeyMappings.SUIT_ULTIMATE.consumeClick()) {
            if (mc.screen == null && !player.isSpectator()) {
                PacketDistributor.sendToServer(new com.steelstorm.arsenal.network.SuitUltPayload());
            }
        }
        while (ModKeyMappings.DODGE.consumeClick()) {
            if (mc.screen == null && !player.isSpectator()) {
                PacketDistributor.sendToServer(new DodgePayload(player.input.forwardImpulse, player.input.leftImpulse));
            }
        }
        for (int slot = 0; slot < ModKeyMappings.ABILITIES.length; slot++) {
            boolean ultimate = slot == 3;
            while (ModKeyMappings.ABILITIES[slot].consumeClick()) {
                if (mc.screen == null && !player.isSpectator() && Abilities.forStack(player.getMainHandItem()) != null) {
                    ClientCombatState.pressAge[slot] = 0;
                    if (ultimate && ClientCombatState.ultimateCharge < 0 && ultimateReady(player)) {
                        // Ultimates charge while the key is held and go off when it is released.
                        ClientCombatState.ultimateCharge = 0;
                        PacketDistributor.sendToServer(new AbilityPayload(slot, AbilityPayload.CHARGE_START));
                        chargeSound = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                com.steelstorm.arsenal.registry.ModSounds.ULTIMATE_CHARGE.get(), 1.0F, 0.9F);
                        mc.getSoundManager().play(chargeSound);
                    } else if (!ultimate || ClientCombatState.ultimateCharge < 0) {
                        PacketDistributor.sendToServer(new AbilityPayload(slot));
                    }
                }
            }
        }
        if (ClientCombatState.ultimateCharge >= 0 && (!ModKeyMappings.ULTIMATE.isDown() || mc.screen != null
                || ClientCombatState.ultimateCharge > com.steelstorm.arsenal.ability.AbilityManager.FULL_CHARGE + 60)) {
            ClientCombatState.ultimateCharge = -1;
            PacketDistributor.sendToServer(new AbilityPayload(3, AbilityPayload.CHARGE_RELEASE));
            UltimateCinematic.start();
            if (chargeSound != null) {
                mc.getSoundManager().stop(chargeSound);
                chargeSound = null;
            }
        }
    }

    /**
     * A full-strength swing with a Steelstorm weapon leaves a slash trail and a whoosh. They're
     * shown right away here, and the server repeats them for everyone else nearby.
     */
    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND || player == null || mc.level == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof WeaponItem weapon) || player.getAttackStrengthScale(0.5F) < 0.9F) {
            return;
        }
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }
        boolean heavy = player.isShiftKeyDown();
        float roll = SWING_ROLLS[swingIndex++ % SWING_ROLLS.length] + (player.getRandom().nextFloat() - 0.5F) * 20;
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                (heavy ? ModSounds.WEAPON_SWING_HEAVY : ModSounds.WEAPON_SWING).get(), SoundSource.PLAYERS, heavy ? 1.0F : 0.8F,
                0.9F + player.getRandom().nextFloat() * 0.2F, false);
        PacketDistributor.sendToServer(new SwingPayload(heavy, roll));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCombatState.reset();
        SuitGlow.clear();
    }

    /** Small, decaying camera shake on heavy hits. Can be turned off in the client config. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ClientCombatState.shakeTicks <= 0 || !Config.SCREEN_SHAKE.get()) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float progress = (ClientCombatState.shakeTicks - partial) / ClientCombatState.shakeTotal;
        float amount = ClientCombatState.shakeStrength * Math.max(0, progress) * Config.SCREEN_SHAKE_STRENGTH.get().floatValue();
        float time = (Minecraft.getInstance().player.tickCount + partial) * 2.7F;
        event.setYaw(event.getYaw() + Mth.sin(time) * amount * 0.9F);
        event.setPitch(event.getPitch() + Mth.cos(time * 1.3F) * amount * 0.7F);
        event.setRoll(event.getRoll() + Mth.sin(time * 0.7F) * amount * 1.2F);
    }

    private static boolean ultimateReady(LocalPlayer player) {
        boolean synced = ClientCombatState.setId.equals(Abilities.forStack(player.getMainHandItem()).id());
        return (ClientCombatState.ultimate >= 100 || player.getAbilities().instabuild) && (!synced || ClientCombatState.cooldownLeft[3] <= 0);
    }

    private ClientEvents() {
    }
}

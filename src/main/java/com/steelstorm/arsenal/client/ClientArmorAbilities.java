package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.combat.ArmorAbilities;
import com.steelstorm.arsenal.network.DoubleJumpPayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The client half of the Stormsteel piece abilities: the mid-air jump and Storm Sight outlines. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class ClientArmorAbilities {
    private static final double SIGHT_RANGE = 24.0;
    private static final IntSet OUTLINED = new IntOpenHashSet();
    private static boolean jumpWasDown;
    private static boolean airJumped;
    private static int airTicks;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            OUTLINED.clear();
            return;
        }
        tickJump(mc, player);
        tickSight(mc, player);
    }

    private static void tickJump(Minecraft mc, LocalPlayer player) {
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !jumpWasDown;
        jumpWasDown = down;
        if (player.onGround() || player.isInWater() || player.getAbilities().flying || player.onClimbable()) {
            airJumped = false;
            airTicks = 0;
            return;
        }
        // The ground jump itself happens earlier in this same tick, so only count presses once airborne.
        if (++airTicks > 2 && pressed && !airJumped && mc.screen == null && !player.isSpectator() && !player.isFallFlying()
                && ArmorAbilities.wearing(player, ArmorItem.Type.BOOTS)) {
            airJumped = true;
            Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
            Vec3 v = player.getDeltaMovement();
            double push = player.input.forwardImpulse > 0 ? 0.32 : 0.0;
            player.setDeltaMovement(v.x * 0.7 + look.x * push, 0.62, v.z * 0.7 + look.z * push);
            player.fallDistance = 0;
            PacketDistributor.sendToServer(new DoubleJumpPayload());
        }
    }

    private static void tickSight(Minecraft mc, LocalPlayer player) {
        boolean active = ArmorAbilities.wearing(player, ArmorItem.Type.HELMET);
        IntSet now = new IntOpenHashSet();
        if (active) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof Enemy && e instanceof LivingEntity living && living.isAlive() && e.distanceToSqr(player) < SIGHT_RANGE * SIGHT_RANGE) {
                    // Only the shared flag on this client is set, so other players don't see the outline.
                    e.setSharedFlag(6, true);
                    now.add(e.getId());
                }
            }
        }
        for (int id : OUTLINED) {
            if (!now.contains(id)) {
                Entity e = mc.level.getEntity(id);
                if (e instanceof LivingEntity living) {
                    e.setSharedFlag(6, living.hasEffect(MobEffects.GLOWING));
                }
            }
        }
        OUTLINED.clear();
        OUTLINED.addAll(now);
    }

    private ClientArmorAbilities() {
    }
}

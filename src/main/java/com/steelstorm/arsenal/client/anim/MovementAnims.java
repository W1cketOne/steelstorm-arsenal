package com.steelstorm.arsenal.client.anim;

import com.steelstorm.arsenal.SteelstormArsenal;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Smoothed movement state for every player in view: how much they are sprinting, whether they are
 * in the air, and a short impact pulse when they land. The run, jump and landing animations in
 * {@link WeaponAnimator} and {@link AnimationEvents} read it.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class MovementAnims {
    public static final class State {
        float sprint;
        float sprintO;
        float air;
        float airO;
        float land;
        float landO;
        int airTicks;
        boolean wasGround = true;

        public float sprint(float partial) {
            return Mth.lerp(partial, sprintO, sprint);
        }

        public float air(float partial) {
            return Mth.lerp(partial, airO, air);
        }

        /** 1 right on landing, fading to 0 over a few ticks. */
        public float land(float partial) {
            return Mth.lerp(partial, landO, land);
        }
    }

    private static final State NONE = new State();
    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();

    public static State get(LivingEntity entity) {
        State s = STATES.get(entity.getId());
        return s == null ? NONE : s;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            if (mc.level == null) {
                STATES.clear();
            }
            return;
        }
        STATES.int2ObjectEntrySet().removeIf(e -> !(mc.level.getEntity(e.getIntKey()) instanceof Player p) || !p.isAlive());
        for (Player p : mc.level.players()) {
            State s = STATES.computeIfAbsent(p.getId(), id -> new State());
            s.sprintO = s.sprint;
            s.airO = s.air;
            s.landO = s.land;
            boolean ground = p.onGround() || p.isInWater() || p.getAbilities().flying || p.isPassenger() || p.onClimbable();
            boolean running = p.isSprinting() && !p.isSwimming() && !p.isFallFlying();
            s.sprint = Mth.approach(s.sprint, running ? 1 : 0, running ? 0.2F : 0.14F);
            s.air = Mth.approach(s.air, ground ? 0 : 1, ground ? 0.34F : 0.22F);
            s.airTicks = ground ? 0 : s.airTicks + 1;
            if (ground && !s.wasGround && s.airO > 0.6F) {
                s.land = 1.0F;
            } else {
                s.land = Math.max(0, s.land - 0.2F);
            }
            s.wasGround = ground;
        }
    }

    private MovementAnims() {
    }
}

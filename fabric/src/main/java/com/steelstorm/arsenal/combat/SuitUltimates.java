package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.entity.MeteorEntity;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.entity.VortexEntity;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.CelestialArmorItem;
import com.steelstorm.arsenal.item.DragonscaleArmorItem;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import com.steelstorm.arsenal.item.VoidwalkerArmorItem;
import com.steelstorm.arsenal.item.WarlordArmorItem;
import com.steelstorm.arsenal.network.SuitGlowPayload;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.entity.player.PlayerEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * Suit ultimates: a full set of Steelstorm armour unlocks one extra ultimate on its own key (B by
 * default), usable whatever you hold. The wielder is untouchable while it runs and the armour blazes
 * with light.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class SuitUltimates {
    public enum Suit {
        STORMSTEEL("Thunderstorm", 0x7FD8FF, 100, StormsteelArmorItem.class::isInstance),
        WARLORD("Hellfire Eruption", 0xFF6A14, 90, WarlordArmorItem.class::isInstance),
        VOIDWALKER("Phantom Rift", 0xB15CFF, 60, VoidwalkerArmorItem.class::isInstance),
        CELESTIAL("Heaven's Judgment", 0xFFE08A, 110, CelestialArmorItem.class::isInstance),
        DRAGONSCALE("Dragon's Breath", 0xFF4A1A, 110, DragonscaleArmorItem.class::isInstance);

        public final String title;
        public final int color;
        public final int duration;
        private final Predicate<Item> piece;

        Suit(String title, int color, int duration, Predicate<Item> piece) {
            this.title = title;
            this.color = color;
            this.duration = duration;
            this.piece = piece;
        }
    }

    /** Cooldown between suit ultimates, in ticks. */
    public static final int COOLDOWN = 900;
    private static final Map<UUID, Long> READY = new HashMap<>();

    public static Suit suitOf(LivingEntity e) {
        for (Suit suit : Suit.values()) {
            boolean all = true;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (!suit.piece.test(e.getItemBySlot(slot).getItem())) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return suit;
            }
        }
        return null;
    }

    /** Ticks until this player's suit ultimate is ready (0 when ready). */
    public static int cooldownLeft(ServerPlayer player) {
        return (int) Math.max(0, READY.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
    }

    public static void activate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        Suit suit = suitOf(player);
        if (suit == null) {
            player.displayClientMessage(Component.translatable("message.steelstorm.suit_needed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int left = player.getAbilities().instabuild ? 0 : cooldownLeft(player);
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.steelstorm.suit_cooldown", (left + 19) / 20)
                    .withStyle(ChatFormatting.GOLD), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        READY.put(player.getUUID(), level.getGameTime() + COOLDOWN);
        UltGuard.protect(player, suit.duration + 20);
        SuitGlowPayload.send(player, suit.duration + 20, suit.color);
        player.displayClientMessage(Component.literal("SUIT ULTIMATE: " + suit.title.toUpperCase())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        Vec3 at = player.position();
        Fx.pillar(level, at, suit.color, Fx.WHITE, 1.2F, 12.0F, 30);
        Fx.halo(level, at, suit.color, Fx.GOLD, 8.0F, 24);
        Fx.circle(level, at, player, suit.color, Fx.GOLD, 3.5F, suit.duration);
        Fx.sound(level, at, ModSounds.ULTIMATE_READY, 1.6F, 0.7F);
        Fx.sound(level, at, ModSounds.ABILITY_EPIC_IMPACT, 1.4F, 1.1F);
        Stamina.shake(player, 1.0F, 12);
        switch (suit) {
            case DRAGONSCALE -> dragonsBreath(player, suit);
            case WARLORD -> hellfire(player, suit);
            case STORMSTEEL -> thunderstorm(player, suit);
            case VOIDWALKER -> phantomRift(player, suit);
            case CELESTIAL -> heavensJudgment(player, suit);
        }
    }

    private static boolean alive(ServerPlayer p) {
        return p.isAlive() && !p.isRemoved();
    }

    private static float dmg(ServerPlayer p, float base) {
        // Scales with the wielder's weapon, but even bare hands make it lethal.
        return Math.max(base, (float) com.steelstorm.arsenal.ability.AbilityManager.baseDamage(p.getMainHandItem()) * base / 8.0F);
    }

    // ------------------------------------------------------------------ Dragonscale

    /** A torrent of dragonfire pours from the wielder's mouth wherever they look. */
    private static void dragonsBreath(ServerPlayer p, Suit suit) {
        ServerLevel level = p.serverLevel();
        Fx.sound(level, p.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.2F, 1.3F);
        for (int t = 0; t < suit.duration; t += 2) {
            final int tick = t;
            ServerScheduler.schedule(t, () -> {
                if (!alive(p)) {
                    return;
                }
                Vec3 eye = p.getEyePosition().add(0, -0.3, 0);
                Vec3 look = p.getLookAngle();
                // Starts a little ahead of the face, so in first person the fire pours out in front
                // of you instead of filling the screen.
                Vec3 mouth = eye.add(look.scale(1.6));
                for (int i = 0; i < 10; i++) {
                    Vec3 spread = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).scale(0.12);
                    Vec3 v = look.add(spread).normalize().scale(0.8 + level.random.nextDouble() * 0.6);
                    level.sendParticles(i % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0,
                            v.x, v.y, v.z, 1.0);
                }
                Fx.shoot(level, ModParticles.GLOW.get(), suit.color, 2.4F, mouth, look.scale(0.9));
                if (tick % 6 == 0) {
                    Vec3 far = mouth.add(look.scale(4));
                    level.sendParticles(ParticleTypes.SMOKE, far.x, far.y, far.z, 4, 0.4, 0.4, 0.4, 0.02);
                    Fx.sound(level, mouth, SoundEvents.BLAZE_SHOOT, 0.9F, 0.6F + level.random.nextFloat() * 0.3F);
                }
                // Everything in a 10-block cone in front burns.
                if (tick % 4 == 0) {
                    for (LivingEntity e : CombatUtil.around(p, eye.add(look.scale(5)), 7)) {
                        Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                        double d = to.length();
                        if (d > 10.5 || to.normalize().dot(look) < 0.82) {
                            continue;
                        }
                        e.invulnerableTime = 0;
                        CombatUtil.specialHurt(p, e, dmg(p, 9.0F));
                        e.igniteForSeconds(8);
                        Vec3 push = look.multiply(1, 0, 1).normalize().scale(0.25);
                        e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.05, push.z));
                        e.hurtMarked = true;
                    }
                }
            });
        }
        ServerScheduler.schedule(suit.duration, () -> {
            if (alive(p)) {
                Shockwaves.ring(level, p, p.position(), 8.0F, 1.0F, dmg(p, 14.0F), 0.8, suit.color, e -> e.igniteForSeconds(10));
                Fx.impact(level, p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4)).add(0, 0.5, 0), suit.color, 3.0F);
                Fx.sound(level, p.position(), ModSounds.ABILITY_INFERNO, 1.6F, 0.6F);
            }
        });
    }

    // ------------------------------------------------------------------ Ember Warlord

    /** The ground erupts under every enemy around, and molten meteors fall. */
    private static void hellfire(ServerPlayer p, Suit suit) {
        ServerLevel level = p.serverLevel();
        Fx.cracks(level, p.position(), suit.color, 12.0F);
        Fx.sound(level, p.position(), ModSounds.ABILITY_INFERNO, 1.8F, 0.5F);
        for (int t = 0; t < suit.duration; t += 9) {
            final int tick = t;
            ServerScheduler.schedule(t, () -> {
                if (!alive(p)) {
                    return;
                }
                List<LivingEntity> foes = CombatUtil.around(p, p.position(), 14);
                List<Vec3> spots = new ArrayList<>();
                for (LivingEntity e : foes) {
                    spots.add(e.position());
                }
                while (spots.size() < 3) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    double r = 3 + level.random.nextDouble() * 9;
                    spots.add(p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
                }
                for (Vec3 at : spots.subList(0, Math.min(6, spots.size()))) {
                    Shockwaves.ring(level, p, at, 2.8F, 1.6F, dmg(p, 10.0F), 1.0, suit.color, e -> e.igniteForSeconds(6));
                    level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 10, 0.6, 0.2, 0.6, 0.3);
                    for (int k = 0; k < 6; k++) {
                        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2 + k * 0.5, at.z, 6, 0.3, 0.2, 0.3, 0.04);
                    }
                    Fx.pillar(level, at, suit.color, 0xFFE0A0, 0.6F, 5.0F, 10);
                }
                Fx.sound(level, p.position(), SoundEvents.GENERIC_EXPLODE, 1.0F, 0.7F);
                if (tick % 27 == 0) {
                    MeteorEntity m = ModEntities.METEOR.get().create(level);
                    if (m != null) {
                        Vec3 target = spots.get(0);
                        m.crater = false;
                        m.owner = p;
                        m.setSize(1.2F);
                        Vec3 vel = new Vec3(0.4, -1.8, 0.25);
                        m.setPos(target.subtract(vel.scale(16)));
                        m.setDeltaMovement(vel);
                        level.addFreshEntity(m);
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ Stormsteel

    /** A storm follows the wielder, striking enemies with chain lightning. */
    private static void thunderstorm(ServerPlayer p, Suit suit) {
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, suit.duration, 3, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, suit.duration, 2, false, false, true));
        Fx.sound(level, p.position(), ModSounds.ABILITY_THUNDER, 1.6F, 0.7F);
        for (int t = 0; t < suit.duration; t += 5) {
            ServerScheduler.schedule(t, () -> {
                if (!alive(p)) {
                    return;
                }
                Fx.gyro(level, p.position(), p, suit.color, Fx.LIGHTNING, 2.0F, 2.2F, 6);
                List<LivingEntity> foes = CombatUtil.around(p, p.position(), 16);
                if (foes.isEmpty()) {
                    return;
                }
                LivingEntity first = foes.get(level.random.nextInt(foes.size()));
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(first.getX(), first.getY(), first.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                first.invulnerableTime = 0;
                CombatUtil.specialHurt(p, first, dmg(p, 12.0F));
                // Chain to up to three more nearby.
                LivingEntity from = first;
                int chained = 0;
                for (LivingEntity e : CombatUtil.around(p, first.position(), 6)) {
                    if (e == first || chained >= 3) {
                        continue;
                    }
                    Vec3 a = from.getBoundingBox().getCenter();
                    Vec3 b = e.getBoundingBox().getCenter();
                    for (int k = 0; k <= 8; k++) {
                        Vec3 q = a.lerp(b, k / 8.0).add(level.random.nextGaussian() * 0.15, level.random.nextGaussian() * 0.15,
                                level.random.nextGaussian() * 0.15);
                        Fx.burst(level, ModParticles.SPARK.get(), Fx.LIGHTNING, 1.0F, q, 1, 0.01, 0.0);
                    }
                    e.invulnerableTime = 0;
                    CombatUtil.specialHurt(p, e, dmg(p, 7.0F));
                    from = e;
                    chained++;
                }
                Fx.sound(level, first.position(), ModSounds.ABILITY_ZAP, 1.0F, 1.2F);
            });
        }
    }

    // ------------------------------------------------------------------ Voidwalker

    /** Vanish, blink behind enemy after enemy cutting each down, then collapse into a void implosion. */
    private static void phantomRift(ServerPlayer p, Suit suit) {
        ServerLevel level = p.serverLevel();
        Vec3 origin = p.position();
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, suit.duration, 0, false, false, false));
        List<LivingEntity> foes = new ArrayList<>(CombatUtil.around(p, origin, 16));
        foes.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
        int strikes = Math.min(8, foes.size());
        Fx.sound(level, origin, ModSounds.ABILITY_VOID, 1.6F, 0.8F);
        for (int i = 0; i < strikes; i++) {
            final LivingEntity target = foes.get(i);
            ServerScheduler.schedule(4 + i * 6, () -> {
                if (!alive(p) || !target.isAlive()) {
                    return;
                }
                Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.4));
                Fx.burst(level, ModParticles.GLOW.get(), suit.color, 2.0F, p.position().add(0, 1, 0), 14, 0.4, 0.1);
                p.teleportTo(behind.x, target.getY(), behind.z);
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                target.invulnerableTime = 0;
                CombatUtil.specialHurt(p, target, dmg(p, 18.0F));
                Fx.slash(level, target.getBoundingBox().getCenter(), p.getYRot(), 0, level.random.nextFloat() * 180, suit.color, 1.8F);
                Fx.burst(level, ModParticles.GLOW.get(), suit.color, 1.8F, target.getBoundingBox().getCenter(), 20, 0.5, 0.15);
                Fx.sound(level, target.position(), ModSounds.ABILITY_VOID, 0.9F, 1.5F);
            });
        }
        ServerScheduler.schedule(Math.max(10, 6 + strikes * 6), () -> {
            if (!alive(p)) {
                return;
            }
            p.teleportTo(origin.x, origin.y, origin.z);
            p.removeEffect(MobEffects.INVISIBILITY);
            // The rift opens ahead of you, not around you (in first person it would swallow the screen).
            Vec3 ahead = origin.add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(5)).add(0, 1, 0);
            VortexEntity.spawn(p, ahead, false, 12.0F, 2.0F, 40, suit.color, dmg(p, 4.0F), dmg(p, 30.0F), 0);
            Fx.sound(level, origin, ModSounds.ABILITY_VOID_HUM, 1.6F, 0.5F);
        });
    }

    // ------------------------------------------------------------------ Celestial

    /** Rise into the sky and call down pillars of holy light on every enemy below. */
    private static void heavensJudgment(ServerPlayer p, Suit suit) {
        ServerLevel level = p.serverLevel();
        p.setDeltaMovement(0, 1.2, 0);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1, false, false, false));
        Fx.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.6F, 1.2F);
        for (int t = 20; t < suit.duration; t += 2) {
            ServerScheduler.schedule(t, () -> {
                if (alive(p)) {
                    // Hover in place, wreathed in light.
                    p.setDeltaMovement(p.getDeltaMovement().x * 0.5, 0.0, p.getDeltaMovement().z * 0.5);
                    p.hurtMarked = true;
                    p.fallDistance = 0;
                    level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 3, 0.5, 0.8, 0.5, 0.02);
                }
            });
        }
        for (int t = 16; t < suit.duration; t += 5) {
            ServerScheduler.schedule(t, () -> {
                if (!alive(p)) {
                    return;
                }
                for (LivingEntity e : CombatUtil.around(p, p.position(), 22)) {
                    if (level.random.nextFloat() > 0.6F) {
                        continue;
                    }
                    Vec3 at = e.position();
                    Fx.pillar(level, at, suit.color, Fx.WHITE, 0.8F, 14.0F, 10);
                    level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 3, at.z, 20, 0.3, 3.0, 0.3, 0.02);
                    e.invulnerableTime = 0;
                    float damage = dmg(p, 11.0F) * (e.getType().is(EntityTypeTags.UNDEAD) ? 2 : 1);
                    CombatUtil.specialHurt(p, e, damage);
                    p.heal(1.0F);
                }
                Fx.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.6F);
            });
        }
        ServerScheduler.schedule(suit.duration, () -> {
            if (!alive(p)) {
                return;
            }
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
            Shockwaves.ring(level, p, p.position().subtract(0, 4, 0), 12.0F, 1.2F, dmg(p, 16.0F), 1.0, suit.color, null);
            Fx.sunburst(level, p.position(), suit.color, Fx.WHITE, 12.0F, 20, 22);
            Fx.sound(level, p.position(), ModSounds.ABILITY_EPIC_IMPACT, 1.6F, 1.3F);
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        READY.remove(event.getEntity().getUUID());
    }

    private SuitUltimates() {
    }
}

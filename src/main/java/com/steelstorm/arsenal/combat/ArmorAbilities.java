package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.Shockwaves;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import com.steelstorm.arsenal.registry.ModDamageTypes;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Each Stormsteel piece has its own power, on top of the full-set Stormcaller bonus:
 * <ul>
 *   <li>Helmet, Storm Sight: hostile mobs within 24 blocks glow through walls (client side, see ClientArmorAbilities).</li>
 *   <li>Chestplate, Static Barrier: every 20 s the next hit is fully absorbed and the attacker is blasted away.</li>
 *   <li>Leggings, Lightning Sprint: after 1.5 s of sprinting you surge to Speed II and leave a spark trail.</li>
 *   <li>Boots, Thunder Step: jump again in mid-air, and landing from a height sends out a damaging shockwave
 *       instead of hurting you.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ArmorAbilities {
    public static final int BARRIER_COOLDOWN = 400;
    private static final String TAG = "steelstorm_armor";

    public static boolean wearing(LivingEntity entity, ArmorItem.Type type) {
        return entity.getItemBySlot(type.getSlot()).getItem() instanceof StormsteelArmorItem;
    }

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(TAG)) {
            root.put(TAG, new CompoundTag());
        }
        return root.getCompound(TAG);
    }

    /** Validates and plays a mid-air jump; the client already applied the motion. */
    public static void doubleJump(ServerPlayer player) {
        CompoundTag d = data(player);
        if (!wearing(player, ArmorItem.Type.BOOTS) || player.onGround() || player.getAbilities().flying || player.isInWater()
                || d.getBoolean("air_jumped")) {
            return;
        }
        d.putBoolean("air_jumped", true);
        player.fallDistance = 0;
        ServerLevel level = player.serverLevel();
        Vec3 feet = player.position();
        Fx.ring(level, feet, Fx.STORM, 1.6F);
        Fx.sparks(level, Fx.LIGHTNING, feet, 10, 0.4);
        Fx.burst(level, ModParticles.SPARK.get(), Fx.STORM, 1.0F, feet, 8, 0.4, 0.08);
        Fx.sound(level, feet, ModSounds.ABILITY_DASH, 0.8F, 1.5F);
        Fx.sound(level, feet, ModSounds.ABILITY_ZAP, 0.5F, 1.8F);
    }

    public static boolean barrierReady(Player player) {
        return player.level().getGameTime() >= data(player).getLong("barrier_at");
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag d = data(player);
        if (player.onGround() || player.isInWater() || player.getAbilities().flying) {
            d.putBoolean("air_jumped", false);
        }
        ServerLevel level = player.serverLevel();
        // Lightning Sprint.
        int sprint = player.isSprinting() && wearing(player, ArmorItem.Type.LEGGINGS) ? d.getInt("sprint") + 1 : 0;
        d.putInt("sprint", sprint);
        if (sprint >= 30) {
            if (sprint == 30) {
                Fx.sound(level, player.position(), ModSounds.ABILITY_ZAP, 0.6F, 1.4F);
                Fx.ring(level, player.position(), Fx.STORM, 1.4F);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12, 1, true, false, true));
            if (sprint % 2 == 0) {
                Fx.burst(level, ModParticles.SPARK.get(), Fx.LIGHTNING, 0.9F, player.position().add(0, 0.15, 0), 2, 0.2, 0.02);
                Fx.burst(level, ModParticles.GLOW.get(), Fx.STORM, 1.1F, player.position().add(0, 0.1, 0), 1, 0.1, 0.0);
            }
        }
        // Static Barrier: a faint crackle while it is charged.
        if (player.tickCount % 30 == 0 && wearing(player, ArmorItem.Type.CHESTPLATE) && barrierReady(player)) {
            Fx.sparkles(level, player.getBoundingBox().getCenter(), Fx.STORM, 3, 0.6);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() < 1.0F
                || !wearing(player, ArmorItem.Type.CHESTPLATE) || !barrierReady(player)
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setCanceled(true);
        data(player).putLong("barrier_at", player.level().getGameTime() + BARRIER_COOLDOWN);
        ServerLevel level = player.serverLevel();
        Vec3 c = player.getBoundingBox().getCenter();
        Fx.halo(level, c, Fx.STORM, Fx.LIGHTNING, 2.2F, 12);
        Fx.sparks(level, Fx.LIGHTNING, c, 24, 0.8);
        Fx.sound(level, c, ModSounds.ABILITY_THUNDER, 0.7F, 1.6F);
        Fx.sound(level, c, SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(4), e -> Shockwaves.canHit(player, e))) {
            Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize();
            e.invulnerableTime = 0;
            e.hurt(ModDamageTypes.zap(level, player), 3.0F);
            e.setDeltaMovement(away.x * 1.2 * Shockwaves.resist(e), 0.45, away.z * 1.2 * Shockwaves.resist(e));
            e.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !wearing(player, ArmorItem.Type.BOOTS) || event.getDistance() < 3.5F) {
            return;
        }
        float dist = event.getDistance();
        event.setDamageMultiplier(dist < 14 ? 0.0F : 0.4F);
        ServerLevel level = player.serverLevel();
        float radius = Math.min(7.0F, 2.5F + dist * 0.3F);
        float damage = Math.min(12.0F, 2.0F + dist * 0.7F);
        Shockwaves.ring(level, player, player.position(), radius, 0.9F, damage, 0.35, Fx.STORM, null);
        Fx.impact(level, player.position().add(0, 0.2, 0), Fx.STORM, Math.min(2.4F, 1.0F + dist * 0.08F));
        Fx.sparks(level, Fx.LIGHTNING, player.position(), 20, 1.0);
        Fx.sound(level, player.position(), ModSounds.ABILITY_SHOCKWAVE, 1.0F, 1.1F);
        Fx.sound(level, player.position(), ModSounds.ABILITY_THUNDER, 0.6F, 1.4F);
    }

    private ArmorAbilities() {
    }
}

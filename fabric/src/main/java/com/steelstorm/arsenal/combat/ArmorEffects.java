package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import com.steelstorm.arsenal.registry.ModDamageTypes;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.PlayerTickEvent;

/**
 * Stormcaller set bonus for a full suit of Stormsteel armour: 10% faster, immune to lightning,
 * and anyone who hits you in melee has a 30% chance to be struck by a static discharge.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ArmorEffects {
    private static final ResourceLocation SPEED = SteelstormArsenal.id("stormcaller_speed");

    public static boolean hasFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof StormsteelArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean full = hasFullSet(player);
        if (full && !speed.hasModifier(SPEED)) {
            speed.addTransientModifier(new AttributeModifier(SPEED, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!full && speed.hasModifier(SPEED)) {
            speed.removeModifier(SPEED);
        }
        if (full && player.tickCount % 40 == 0 && player.getRandom().nextInt(3) == 0) {
            Fx.burst(player.serverLevel(), ModParticles.SPARK.get(), Fx.STORM, 0.8F, player.getBoundingBox().getCenter(), 2, 0.3, 0.05);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !hasFullSet(player)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
            event.setCanceled(true);
            return;
        }
        if (CombatUtil.isDirectMelee(event.getSource()) && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != player && player.getRandom().nextFloat() < 0.3F && player.level() instanceof ServerLevel level) {
            attacker.invulnerableTime = 0;
            attacker.hurt(ModDamageTypes.zap(level, player), 4.0F);
            Vec3 a = player.getBoundingBox().getCenter();
            Vec3 b = attacker.getBoundingBox().getCenter();
            for (int i = 0; i <= 8; i++) {
                Fx.burst(level, ModParticles.SPARK.get(), Fx.LIGHTNING, 1.1F, a.lerp(b, i / 8.0), 1, 0.05, 0.0);
            }
            Fx.sound(level, b, ModSounds.ABILITY_ZAP, 0.8F, 1.3F);
        }
    }

    private ArmorEffects() {
    }
}

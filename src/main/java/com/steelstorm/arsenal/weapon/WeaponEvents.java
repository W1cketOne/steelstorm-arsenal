package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModDataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Weapon bookkeeping that is not part of combat maths. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class WeaponEvents {
    private WeaponEvents() {
    }

    /** Counts melee kills on the weapon (thrown spears count their own kills). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity().level().isClientSide || !(source.getEntity() instanceof Player player)
                || source.getDirectEntity() != player) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof WeaponItem) {
            stack.set(ModDataComponents.KILL_COUNT.get(), stack.getOrDefault(ModDataComponents.KILL_COUNT.get(), 0) + 1);
        }
    }
}

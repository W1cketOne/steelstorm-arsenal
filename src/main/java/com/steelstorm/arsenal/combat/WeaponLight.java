package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Glowing weapons light up the dark around you: an invisible vanilla light block follows the
 * player while they hold a Stormsteel, diamond, netherite or legendary weapon. It only ever goes
 * into empty air and is removed as soon as the player moves on, swaps weapons or leaves.
 */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class WeaponLight {
    private record Placed(ServerLevel level, BlockPos pos) {
    }

    private static final Map<UUID, Placed> PLACED = new HashMap<>();

    public static int lightLevel(ItemStack stack) {
        if (stack.getItem() instanceof LegendaryWeaponItem) {
            return 13;
        }
        if (stack.getItem() instanceof WeaponItem weapon && weapon.weaponTier() != null) {
            return switch (weapon.weaponTier()) {
                case STORMSTEEL -> 12;
                case NETHERITE -> 10;
                case DIAMOND -> 8;
                default -> 0;
            };
        }
        return 0;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 3 != 0) {
            return;
        }
        int level = player.isSpectator() ? 0 : lightLevel(player.getMainHandItem());
        BlockPos want = level > 0 ? BlockPos.containing(player.getEyePosition()) : null;
        Placed old = PLACED.get(player.getUUID());
        if (old != null && want != null && old.level() == player.serverLevel() && old.pos().equals(want)) {
            return;
        }
        clear(player.getUUID());
        if (want == null) {
            return;
        }
        ServerLevel world = player.serverLevel();
        BlockState there = world.getBlockState(want);
        if (!there.isAir()) {
            return;
        }
        world.setBlock(want, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level), 3);
        PLACED.put(player.getUUID(), new Placed(world, want));
    }

    private static void clear(UUID id) {
        Placed p = PLACED.remove(id);
        if (p != null && p.level().getBlockState(p.pos()).is(Blocks.LIGHT)) {
            p.level().setBlock(p.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (UUID id : PLACED.keySet().toArray(new UUID[0])) {
            clear(id);
        }
    }

    private WeaponLight() {
    }
}

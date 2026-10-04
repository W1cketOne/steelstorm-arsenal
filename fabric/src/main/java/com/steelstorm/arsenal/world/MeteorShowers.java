package com.steelstorm.arsenal.world;

import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.MeteorEntity;
import com.steelstorm.arsenal.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.tick.LevelTickEvent;

/** At night, meteors now and then streak across the sky and crash somewhere near a player. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class MeteorShowers {
    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) {
            return;
        }
        long day = level.getDayTime() % 24000;
        if (day < 13500 || day > 22500) {
            return;
        }
        double chance = Config.METEOR_CHANCE.get();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && level.random.nextDouble() < chance) {
                spawnNear(level, player);
            }
        }
    }

    /** Sends a meteor down at a spot 20-50 blocks from the player. */
    public static void spawnNear(ServerLevel level, ServerPlayer player) {
        RandomSource r = level.random;
        double a = r.nextDouble() * Math.PI * 2;
        double dist = 20 + r.nextDouble() * 30;
        int tx = (int) (player.getX() + Math.cos(a) * dist);
        int tz = (int) (player.getZ() + Math.sin(a) * dist);
        int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
        Vec3 target = new Vec3(tx + 0.5, ty, tz + 0.5);
        double b = r.nextDouble() * Math.PI * 2;
        Vec3 start = target.add(Math.cos(b) * 70, 110, Math.sin(b) * 70);
        Vec3 velocity = target.subtract(start).normalize().scale(1.8);
        MeteorEntity meteor = new MeteorEntity(ModEntities.METEOR.get(), level);
        meteor.setPos(start);
        meteor.setDeltaMovement(velocity);
        meteor.setSize(1.2F + r.nextFloat() * 1.0F);
        level.addFreshEntity(meteor);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(target) < 200 * 200) {
                p.displayClientMessage(Component.literal("☄ A meteor streaks across the sky!").withStyle(ChatFormatting.GOLD,
                        ChatFormatting.ITALIC), true);
            }
        }
    }

    private MeteorShowers() {
    }
}

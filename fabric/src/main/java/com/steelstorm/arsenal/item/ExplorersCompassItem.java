package com.steelstorm.arsenal.item;

import com.mojang.datafixers.util.Pair;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModParticles;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/**
 * Points the way to the nearest Steelstorm structure. Use it to search; sneak and use it to pick
 * which kind of structure to look for.
 */
public class ExplorersCompassItem extends Item {
    public static final List<String> TARGETS = List.of("knights_crypt", "colossus_forge", "moonlit_sanctum", "storm_shrine",
            "ruined_colosseum", "proving_grounds", "stormsteel_mine", "bandit_camp", "blacksmith", "watchtower", "abandoned_armory");

    public ExplorersCompassItem(Properties properties) {
        super(properties);
    }

    private static int target(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(ModDataComponents.COMPASS_TARGET.get(), 0), TARGETS.size());
    }

    private static Component name(int i) {
        return Component.translatable("structure.steelstorm." + TARGETS.get(i));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int next = (target(stack) + 1) % TARGETS.size();
            stack.set(ModDataComponents.COMPASS_TARGET.get(), next);
            player.displayClientMessage(Component.translatable("message.steelstorm.compass_target", name(next)).withStyle(ChatFormatting.AQUA), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level instanceof ServerLevel server) {
            int i = target(stack);
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, SteelstormArsenal.id(TARGETS.get(i)));
            Holder<Structure> holder = server.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key).orElse(null);
            Pair<BlockPos, Holder<Structure>> found = holder == null ? null : server.getChunkSource().getGenerator()
                    .findNearestMapStructure(server, HolderSet.direct(holder), player.blockPosition(), 100, false);
            if (found == null) {
                player.displayClientMessage(Component.translatable("message.steelstorm.compass_none", name(i)).withStyle(ChatFormatting.GRAY), true);
            } else {
                BlockPos pos = found.getFirst();
                Vec3 to = new Vec3(pos.getX() + 0.5 - player.getX(), 0, pos.getZ() + 0.5 - player.getZ());
                int dist = (int) to.horizontalDistance();
                player.displayClientMessage(Component.translatable("message.steelstorm.compass_found", name(i), dist,
                        Component.translatable("direction.steelstorm." + direction(to)), pos.getX(), pos.getZ()).withStyle(ChatFormatting.GOLD), false);
                // A trail of sparkles shoots off toward it.
                Vec3 dir = to.normalize();
                for (int k = 1; k <= 14; k++) {
                    Vec3 p = player.getEyePosition().add(dir.scale(k * 0.8)).add(0, -0.3, 0);
                    server.sendParticles(ModParticles.SPARKLE.get().with(0xFFD166, 1.2F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                }
                Fx.sound(server, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
                player.getCooldowns().addCooldown(this, 40);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static String direction(Vec3 v) {
        String[] names = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};
        double angle = Math.toDegrees(Math.atan2(-v.x, v.z));
        return names[Math.floorMod((int) Math.round(angle / 45.0), 8)];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.steelstorm.compass_target", name(target(stack))).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.steelstorm.compass_use").withStyle(ChatFormatting.GRAY));
    }
}

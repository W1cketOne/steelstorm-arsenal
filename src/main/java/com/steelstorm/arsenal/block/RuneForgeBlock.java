package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.item.RuneItem;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.ChakramItem;
import com.steelstorm.arsenal.weapon.Rune;
import com.steelstorm.arsenal.weapon.WeaponItem;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Inscribes runes on weapons. Place a rune on the forge (it floats above it), then use a weapon
 * on the forge to burn the rune into the blade. Empty-handed, take the rune back.
 */
public class RuneForgeBlock extends BaseEntityBlock {
    public static final MapCodec<RuneForgeBlock> CODEC = simpleCodec(RuneForgeBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 10, 14),
            Block.box(1, 10, 1, 15, 13, 15));

    public RuneForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemHolderBlockEntity(pos, state);
    }

    public static boolean canTakeRune(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem || stack.getItem() instanceof ChakramItem;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ItemHolderBlockEntity forge)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof RuneItem) {
            if (!forge.getItem().isEmpty()) {
                return ItemInteractionResult.CONSUME;
            }
            if (!level.isClientSide) {
                forge.setItem(stack.copyWithCount(1));
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (canTakeRune(stack) && forge.getItem().getItem() instanceof RuneItem runeItem) {
            if (level instanceof ServerLevel server) {
                Rune rune = runeItem.rune();
                Rune old = stack.get(ModDataComponents.RUNE);
                stack.set(ModDataComponents.RUNE, rune);
                forge.setItem(ItemStack.EMPTY);
                Vec3 top = Vec3.atCenterOf(pos).add(0, 0.9, 0);
                Fx.burst(server, ModParticles.RUNE.get(), rune.color(), 1.5F, top, 20, 0.4, 0.06);
                Fx.burst(server, ModParticles.GLOW.get(), rune.color(), 1.6F, top, 16, 0.3, 0.1);
                Fx.ring(server, Vec3.atBottomCenterOf(pos).add(0, 0.85, 0), rune.color(), 2.0F);
                Fx.sound(server, top, ModSounds.BLOCK_RUNE_FORGE, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable(old == null ? "message.steelstorm.rune_inscribed"
                                : "message.steelstorm.rune_replaced", Component.translatable(rune.nameKey()), stack.getHoverName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ItemHolderBlockEntity forge) || forge.getItem().isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.steelstorm.rune_forge_hint").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack rune = forge.getItem();
            forge.setItem(ItemStack.EMPTY);
            if (!player.addItem(rune)) {
                player.drop(rune, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            int color = level.getBlockEntity(pos) instanceof ItemHolderBlockEntity forge && forge.getItem().getItem() instanceof RuneItem r
                    ? r.rune().color() : 0xB28CFF;
            level.addParticle(ModParticles.RUNE.get().with(color, 0.7F), pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + 0.85,
                    pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0, 0.02, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemHolderBlockEntity forge) {
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), forge.getItem());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModDataComponents;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Use a weapon on it to sharpen the blade: +2 damage for the next 20 hits. */
public class WhetstoneBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WhetstoneBlock> CODEC = simpleCodec(WhetstoneBlock::new);
    public static final int SHARPENED_HITS = 20;
    private static final Map<Direction, VoxelShape> SHAPES = ShapeUtil.horizontal(Shapes.or(
            Block.box(2, 0, 3, 14, 6, 13), Block.box(3, 6, 4, 13, 15, 12)));

    public WhetstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof WeaponItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getOrDefault(ModDataComponents.SHARPENED.get(), 0) >= SHARPENED_HITS) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.steelstorm.already_sharp").withStyle(ChatFormatting.GRAY), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (level instanceof ServerLevel server) {
            stack.set(ModDataComponents.SHARPENED.get(), SHARPENED_HITS);
            Vec3 top = Vec3.atCenterOf(pos).add(0, 0.45, 0);
            Fx.sparks(server, 0xFFE9A8, top, 16, 0.5);
            Fx.sparks(server, 0xFFFFFF, top, 8, 0.35);
            Fx.sound(server, top, ModSounds.BLOCK_WHETSTONE, 1.0F, 0.9F + server.random.nextFloat() * 0.2F);
            player.displayClientMessage(Component.translatable("message.steelstorm.sharpened", SHARPENED_HITS)
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}

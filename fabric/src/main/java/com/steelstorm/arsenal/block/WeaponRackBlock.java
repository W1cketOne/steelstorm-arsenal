package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A floor-standing rack that displays up to three weapons. Use with a weapon to hang it, empty-handed to take one. */
public class WeaponRackBlock extends BaseEntityBlock {
    public static final MapCodec<WeaponRackBlock> CODEC = simpleCodec(WeaponRackBlock::new);
    private static final VoxelShape SHAPE_NS = Block.box(0, 0, 4, 16, 16, 12);
    private static final VoxelShape SHAPE_EW = Block.box(4, 0, 0, 12, 16, 16);

    public WeaponRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponRackBlockEntity(pos, state);
    }

    public static boolean canHold(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof TridentItem || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof com.steelstorm.arsenal.weapon.ThrowableWeapon;
    }

    /** Which of the three slots (0 left to 2 right, seen from the front) the player aimed at. */
    static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction right = facing.getCounterClockWise();
        double local = (hit.x - pos.getX() - 0.5) * right.getStepX() + (hit.z - pos.getZ() - 0.5) * right.getStepZ();
        return local < -1.0 / 6 ? 0 : (local > 1.0 / 6 ? 2 : 1);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty() || !canHold(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = rack.firstFreeSlot(slotAt(state, pos, hit.getLocation()));
        if (slot < 0) {
            return ItemInteractionResult.CONSUME_PARTIAL;
        }
        if (!level.isClientSide) {
            rack.setWeapon(slot, stack.copyWithCount(1));
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8F, 1.1F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        int slot = rack.filledSlotNear(slotAt(state, pos, hit.getLocation()));
        if (slot < 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack taken = rack.removeWeapon(slot);
            if (!player.addItem(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            Containers.dropContents(level, pos, rack.items());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

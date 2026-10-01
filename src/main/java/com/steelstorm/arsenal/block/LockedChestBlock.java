package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A treasure chest that starts locked. The Champion's Coffer opens when the nearby Arena Gong's
 * challenge is won; the Bandit Vault opens with a Vault Key. Locked chests can't be broken.
 */
public class LockedChestBlock extends BaseEntityBlock {
    public enum Kind { COFFER, VAULT }

    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static final MapCodec<LockedChestBlock> COFFER_CODEC = simpleCodec(p -> new LockedChestBlock(Kind.COFFER, p));
    public static final MapCodec<LockedChestBlock> VAULT_CODEC = simpleCodec(p -> new LockedChestBlock(Kind.VAULT, p));
    private static final Map<Direction, VoxelShape> SHAPES = ShapeUtil.horizontal(Block.box(1, 0, 2, 15, 14, 14));

    private final Kind kind;

    public LockedChestBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(LOCKED, true));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return kind == Kind.COFFER ? COFFER_CODEC : VAULT_CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, LOCKED);
    }

    /** Placed by players, they start unlocked: only generated ones guard their treasure. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LOCKED, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(HorizontalDirectionalBlock.FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(LOCKED) && !player.getAbilities().instabuild ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (kind == Kind.VAULT && state.getValue(LOCKED) && stack.is(ModItems.VAULT_KEY.get())) {
            if (level instanceof ServerLevel server) {
                stack.consume(1, player);
                unlock(server, pos, state);
                player.displayClientMessage(Component.translatable("message.steelstorm.vault_unlocked").withStyle(ChatFormatting.GOLD), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LOCKED)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(kind == Kind.COFFER ? "message.steelstorm.coffer_locked"
                        : "message.steelstorm.vault_locked").withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest) {
            player.openMenu(chest);
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, kind == Kind.COFFER ? 0.8F : 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Opens the lock with fanfare: chains fall away and the chest can be used. */
    public static void unlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof LockedChestBlock block) || !state.getValue(LOCKED)) {
            return;
        }
        level.setBlock(pos, state.setValue(LOCKED, false), Block.UPDATE_ALL);
        Vec3 c = Vec3.atCenterOf(pos);
        int color = block.kind == Kind.COFFER ? Fx.GOLD : 0xC9A227;
        Fx.burst(level, ModParticles.GLOW.get(), color, 1.6F, c.add(0, 0.4, 0), 24, 0.4, 0.12);
        Fx.sparks(level, color, c.add(0, 0.5, 0), 16, 0.6);
        Fx.ring(level, Vec3.atBottomCenterOf(pos).add(0, 0.05, 0), color, 2.5F);
        Fx.sound(level, c, block.kind == Kind.COFFER ? ModSounds.BLOCK_COFFER_UNLOCK : ModSounds.BLOCK_VAULT_OPEN, 1.2F, 1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.COFFER && random.nextInt(4) == 0) {
            int color = state.getValue(LOCKED) ? 0xFFD166 : 0xFFF1C1;
            level.addParticle(ModParticles.GLOW.get().with(color, 0.6F), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.9, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest) {
            Containers.dropContents(level, pos, chest);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

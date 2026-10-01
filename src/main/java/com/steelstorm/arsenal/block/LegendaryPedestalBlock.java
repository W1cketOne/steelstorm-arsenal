package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.entity.CryptKnight;
import com.steelstorm.arsenal.entity.FallenWarlord;
import com.steelstorm.arsenal.entity.StormHerald;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
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
import net.minecraft.world.entity.Mob;
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
 * A stone pedestal with a weapon floating over it. While a guardian (Crypt Knight, Fallen Warlord
 * or Storm Herald) lives within 24 blocks, the weapon is sealed and can't be taken.
 */
public class LegendaryPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<LegendaryPedestalBlock> CODEC = simpleCodec(LegendaryPedestalBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(4, 3, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 14, 14));

    public LegendaryPedestalBlock(Properties properties) {
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

    @Nullable
    public static Mob guardian(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(24),
                m -> m.isAlive() && (m instanceof CryptKnight || m instanceof FallenWarlord || m instanceof StormHerald))
                .stream().findFirst().orElse(null);
    }

    /** Can't be broken to get around the seal. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.getAbilities().instabuild && level instanceof Level world && level.getBlockEntity(pos) instanceof ItemHolderBlockEntity pedestal
                && !pedestal.getItem().isEmpty() && guardian(world, pos) != null) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ItemHolderBlockEntity pedestal && pedestal.getItem().isEmpty()
                && stack.getItem() instanceof WeaponItem) {
            if (!level.isClientSide) {
                pedestal.setItem(stack.copyWithCount(1));
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ItemHolderBlockEntity pedestal) || pedestal.getItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Mob guard = guardian(level, pos);
            Vec3 top = Vec3.atCenterOf(pos).add(0, 0.9, 0);
            if (guard != null && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.steelstorm.pedestal_sealed", guard.getDisplayName())
                        .withStyle(ChatFormatting.DARK_RED), true);
                Fx.burst(server, ModParticles.RUNE.get(), 0xFF4040, 1.3F, top, 10, 0.3, 0.02);
                Fx.sound(server, top, ModSounds.ABILITY_ZAP, 0.8F, 0.6F);
                return InteractionResult.SUCCESS;
            }
            ItemStack weapon = pedestal.getItem();
            Component name = weapon.getHoverName();
            pedestal.setItem(ItemStack.EMPTY);
            if (!player.addItem(weapon)) {
                player.drop(weapon, false);
            }
            Fx.burst(server, ModParticles.GLOW.get(), Fx.GOLD, 1.8F, top, 30, 0.4, 0.15);
            Fx.ring(server, Vec3.atBottomCenterOf(pos), Fx.GOLD, 4.0F);
            Fx.sound(server, top, ModSounds.ULTIMATE_READY, 1.2F, 1.0F);
            player.displayClientMessage(Component.translatable("message.steelstorm.pedestal_claimed", name)
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ItemHolderBlockEntity pedestal && !pedestal.getItem().isEmpty() && random.nextInt(2) == 0) {
            level.addParticle(ModParticles.GLOW.get().with(0xFFD166, 0.8F), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + 1.0, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.04, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemHolderBlockEntity pedestal) {
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), pedestal.getItem());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

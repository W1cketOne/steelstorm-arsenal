package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Light it with flint and steel or a fire charge: for a minute every monster within 48 blocks
 * glows through walls and everyone nearby gets Night Vision. It burns out after two minutes.
 */
public class SignalBrazierBlock extends Block {
    public static final MapCodec<SignalBrazierBlock> CODEC = simpleCodec(SignalBrazierBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final int BURN_TICKS = 20 * 120;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 9, 2, 14, 15, 14), Block.box(6, 0, 6, 10, 9, 10),
            Block.box(3, 0, 3, 13, 2, 13));

    public SignalBrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (!(flint || stack.is(Items.FIRE_CHARGE)) || state.getValue(LIT)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, BURN_TICKS);
            if (flint) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                stack.consume(1, player);
            }
            reveal(server, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void reveal(ServerLevel level, BlockPos pos, Player player) {
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        int found = 0;
        for (Entity e : level.getEntities((Entity) null, new AABB(pos).inflate(48), e -> e instanceof Enemy && e.isAlive())) {
            ((LivingEntity) e).addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60, 0, false, false));
            found++;
        }
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(32))) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0, false, true));
        }
        Fx.sound(level, c, ModSounds.BLOCK_BRAZIER_IGNITE, 1.5F, 1.0F);
        Fx.burst(level, ModParticles.GLOW.get(), 0xFF9A3C, 2.0F, c, 30, 0.3, 0.2);
        Fx.sparks(level, 0xFFD166, c, 20, 0.6);
        Fx.ring(level, Vec3.atBottomCenterOf(pos), 0xFF9A3C, 12.0F);
        player.displayClientMessage(Component.translatable("message.steelstorm.brazier_lit", found).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0, 0.04, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.3, z, 0, 0.07, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ModParticles.GLOW.get().with(0xFF9A3C, 1.2F), x + (random.nextDouble() - 0.5) * 0.4, y + 0.2,
                    z + (random.nextDouble() - 0.5) * 0.4, 0, 0.08, 0);
        }
        if (random.nextInt(10) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
        }
    }
}

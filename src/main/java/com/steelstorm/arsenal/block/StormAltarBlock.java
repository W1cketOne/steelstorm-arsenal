package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.entity.StormHerald;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Offer four Stormsteel Ingots and the storm answers: the Storm Herald descends. */
public class StormAltarBlock extends Block {
    public static final MapCodec<StormAltarBlock> CODEC = simpleCodec(StormAltarBlock::new);
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 10, 14),
            Block.box(1, 10, 1, 15, 12, 15));

    public StormAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.STORMSTEEL_INGOT.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            if (!level.getEntitiesOfClass(StormHerald.class, new AABB(pos).inflate(64)).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.steelstorm.storm_rages").withStyle(ChatFormatting.AQUA), true);
                return ItemInteractionResult.CONSUME;
            }
            int charge = state.getValue(CHARGE) + 1;
            stack.consume(1, player);
            Vec3 top = Vec3.atCenterOf(pos).add(0, 0.4, 0);
            Fx.burst(server, ModParticles.GLOW.get(), Fx.STORM, 1.5F, top, 10 + charge * 4, 0.35, 0.08);
            Fx.sparks(server, Fx.LIGHTNING, top, 6 + charge * 3, 0.5);
            Fx.sound(server, top, ModSounds.BLOCK_ALTAR_CHARGE, 1.0F, 0.8F + charge * 0.15F);
            if (charge >= 4) {
                level.setBlock(pos, state.setValue(CHARGE, 4), Block.UPDATE_ALL);
                summon(server, pos, player);
            } else {
                level.setBlock(pos, state.setValue(CHARGE, charge), Block.UPDATE_ALL);
                player.displayClientMessage(Component.translatable("message.steelstorm.altar_charge", charge, 4)
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.steelstorm.altar_hint", state.getValue(CHARGE), 4)
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void summon(ServerLevel level, BlockPos pos, Player player) {
        Vec3 c = Vec3.atBottomCenterOf(pos.above());
        Fx.sound(level, c, ModSounds.BLOCK_ALTAR_SUMMON, 2.5F, 1.0F);
        level.setWeatherParameters(0, 20 * 60 * 3, true, true);
        for (int i = 0; i < 6; i++) {
            final int n = i;
            com.steelstorm.arsenal.combat.ServerScheduler.schedule(i * 6, () -> {
                double a = n * Math.PI / 3;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(c.x + Math.cos(a) * 5, c.y, c.z + Math.sin(a) * 5);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            });
        }
        com.steelstorm.arsenal.combat.ServerScheduler.schedule(40, () -> {
            StormHerald herald = ModEntities.STORM_HERALD.get().create(level);
            if (herald != null) {
                herald.moveTo(c.x, c.y + 4, c.z, level.random.nextFloat() * 360, 0);
                herald.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                herald.setPersistenceRequired();
                herald.setHome(pos);
                if (player instanceof ServerPlayer target && !target.isCreative()) {
                    herald.setTarget(target);
                }
                level.addFreshEntity(herald);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(c.x, c.y + 4, c.z);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                Fx.ring(level, c, Fx.STORM, 10.0F);
            }
            BlockState now = level.getBlockState(pos);
            if (now.is(this)) {
                level.setBlock(pos, now.setValue(CHARGE, 0), Block.UPDATE_ALL);
            }
        });
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        if (random.nextInt(5 - charge) == 0) {
            level.addParticle(ModParticles.SPARK.get().with(Fx.STORM, 0.8F), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.8, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.08, 0);
        }
    }
}

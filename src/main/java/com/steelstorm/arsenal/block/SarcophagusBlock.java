package com.steelstorm.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A two-block stone coffin. Opening it slides the lid aside, spills its grave goods, and wakes
 * the Crypt Knight sleeping inside.
 */
public class SarcophagusBlock extends HorizontalDirectionalBlock {
    public enum Part implements StringRepresentable {
        HEAD("head"), FOOT("foot");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<SarcophagusBlock> CODEC = simpleCodec(SarcophagusBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final Map<Direction, VoxelShape> SHAPES = ShapeUtil.horizontal(Block.box(1, 0, 0, 15, 13, 16));

    public SarcophagusBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.FOOT).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN);
    }

    /** The head half lies in the facing direction from the foot half. */
    private static Direction toOther(BlockState state) {
        return state.getValue(PART) == Part.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos head = context.getClickedPos().relative(facing);
        Level level = context.getLevel();
        // Placed by players they start open and empty: only generated ones hold treasure (and a knight).
        return level.getBlockState(head).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(head)
                ? defaultBlockState().setValue(FACING, facing).setValue(OPEN, true) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(PART, Part.HEAD), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (direction == toOther(state)) {
            return neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART)
                    ? state.setValue(OPEN, neighbor.getValue(OPEN)) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(PART) == Part.HEAD ? state.getValue(FACING).getOpposite() : state.getValue(FACING);
        return SHAPES.get(facing);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(OPEN)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockPos other = pos.relative(toOther(state));
            BlockPos head = state.getValue(PART) == Part.HEAD ? pos : other;
            level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this)) {
                level.setBlock(other, otherState.setValue(OPEN, true), Block.UPDATE_ALL);
            }
            Vec3 middle = Vec3.atCenterOf(pos).add(Vec3.atCenterOf(other)).scale(0.5);
            Fx.sound(server, middle, ModSounds.BLOCK_SARCOPHAGUS_OPEN, 1.2F, 1.0F);
            Fx.burst(server, ModParticles.SMOKE.get(), 0x8A8378, 1.8F, middle.add(0, 0.5, 0), 20, 0.6, 0.2, 0.6, 0.02);
            Fx.burst(server, ModParticles.GLOW.get(), 0x9BB7FF, 1.3F, middle.add(0, 0.6, 0), 14, 0.5, 0.2, 0.5, 0.05);
            spillLoot(server, middle.add(0, 0.8, 0), player);
            com.steelstorm.arsenal.combat.ServerScheduler.schedule(25, () -> wakeKnight(server, head, state.getValue(FACING)));
            player.displayClientMessage(Component.translatable("message.steelstorm.sarcophagus").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void spillLoot(ServerLevel level, Vec3 at, Player player) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(com.steelstorm.arsenal.registry.ModLootTables.SARCOPHAGUS);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at)
                .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, stack);
            item.setDeltaMovement(level.random.nextGaussian() * 0.08, 0.3, level.random.nextGaussian() * 0.08);
            level.addFreshEntity(item);
        }
    }

    private static void wakeKnight(ServerLevel level, BlockPos head, Direction facing) {
        Mob knight = ModEntities.CRYPT_KNIGHT.get().create(level);
        if (knight == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(head.above());
        knight.moveTo(at.x, at.y, at.z, facing.toYRot() + 180, 0);
        knight.finalizeSpawn(level, level.getCurrentDifficultyAt(head), MobSpawnType.EVENT, null);
        knight.setPersistenceRequired();
        level.addFreshEntity(knight);
        Fx.burst(level, ModParticles.SMOKE.get(), 0x2A2E3A, 2.0F, at.add(0, 1, 0), 24, 0.5, 0.8, 0.5, 0.03);
        Fx.burst(level, ModParticles.GLOW.get(), 0x7FA7FF, 1.6F, at.add(0, 1.2, 0), 20, 0.4, 0.8, 0.4, 0.06);
        Fx.ring(level, at, 0x7FA7FF, 4.0F);
        Fx.sound(level, at, ModSounds.ENTITY_WARLORD_ROAR, 1.2F, 1.3F);
    }
}

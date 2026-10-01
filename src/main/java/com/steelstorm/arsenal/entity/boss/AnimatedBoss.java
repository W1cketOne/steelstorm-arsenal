package com.steelstorm.arsenal.entity.boss;

import com.steelstorm.arsenal.entity.SteelstormBoss;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A boss with a fully custom, keyframe-animated model. Its moves are {@link BossMove}s: the server
 * picks one, syncs its id, and clients play the matching animation while the server runs the move's
 * timeline. Below half health it enters a second phase once, with its own transition move.
 */
public abstract class AnimatedBoss extends SteelstormBoss {
    private static final EntityDataAccessor<Integer> DATA_MOVE = SynchedEntityData.defineId(AnimatedBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(AnimatedBoss.class, EntityDataSerializers.BOOLEAN);

    /** Client: the idle loop and one state per move id. */
    public final AnimationState idle = new AnimationState();
    public final AnimationState[] moveStates = new AnimationState[16];

    private final Map<Integer, Long> readyAt = new HashMap<>();
    @Nullable
    private BossMove current;
    private int moveTick;
    private boolean transitioned;

    protected AnimatedBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level, color, BossEvent.BossBarOverlay.NOTCHED_12);
        for (int i = 0; i < moveStates.length; i++) {
            moveStates[i] = new AnimationState();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOVE, 0);
        builder.define(DATA_ENRAGED, false);
    }

    /** The moveset, in no particular order. */
    protected abstract List<BossMove> moves();

    /** The move that starts phase two (played once, when health first drops below half). */
    protected abstract BossMove phaseTwoMove();

    public boolean enraged() {
        return entityData.get(DATA_ENRAGED);
    }

    protected void setEnraged() {
        entityData.set(DATA_ENRAGED, true);
    }

    public int currentMove() {
        return entityData.get(DATA_MOVE);
    }

    public boolean busy() {
        return current != null;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MOVE.equals(key) && level().isClientSide) {
            int id = entityData.get(DATA_MOVE);
            for (int i = 0; i < moveStates.length; i++) {
                if (i == id && id != 0) {
                    moveStates[i].start(tickCount);
                } else {
                    moveStates[i].stop();
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            idle.startIfStopped(tickCount);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (current == null && !transitioned && getHealth() < getMaxHealth() * 0.5F) {
            transitioned = true;
            begin(phaseTwoMove());
        }
    }

    private void begin(BossMove move) {
        current = move;
        moveTick = 0;
        entityData.set(DATA_MOVE, move.id());
        getNavigation().stop();
    }

    private void finish() {
        if (current != null) {
            readyAt.put(current.id(), level().getGameTime() + current.cooldown());
        }
        current = null;
        entityData.set(DATA_MOVE, 0);
    }

    /** Runs the current move's timeline and picks new moves. Added by subclasses at high priority. */
    protected class MoveGoal extends Goal {
        public MoveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (current != null) {
                return true;
            }
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || getRandom().nextInt(enraged() ? 3 : 5) != 0) {
                return false;
            }
            double dist = distanceTo(target);
            long now = level().getGameTime();
            List<BossMove> options = new ArrayList<>();
            int total = 0;
            for (BossMove m : moves()) {
                if (dist >= m.minRange() && dist <= m.maxRange() && now >= readyAt.getOrDefault(m.id(), 0L) && m.ready().getAsBoolean()
                        && hasLineOfSight(target)) {
                    options.add(m);
                    total += m.weight();
                }
            }
            if (options.isEmpty()) {
                return false;
            }
            int pick = getRandom().nextInt(total);
            for (BossMove m : options) {
                pick -= m.weight();
                if (pick < 0) {
                    begin(m);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return current != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (current == null) {
                return;
            }
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 20.0F, 20.0F);
            }
            if (target == null || !target.isAlive()) {
                // Keep finishing the move's swing even without a target, then stop.
                if (moveTick > current.duration() / 2) {
                    finish();
                    return;
                }
            } else {
                current.action().tick(moveTick, target);
            }
            if (++moveTick >= current.duration()) {
                finish();
            }
        }

        @Override
        public void stop() {
            if (current != null) {
                finish();
            }
        }
    }

    /** Defeating a lair's boss unlocks the coffers it guarded. */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(blockPosition().offset(-24, -6, -24),
                    blockPosition().offset(24, 6, 24))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof com.steelstorm.arsenal.block.LockedChestBlock
                        && state.getValue(com.steelstorm.arsenal.block.LockedChestBlock.LOCKED)) {
                    com.steelstorm.arsenal.block.LockedChestBlock.unlock(level, pos.immutable(), state);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseTwo", transitioned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        transitioned = tag.getBoolean("PhaseTwo");
        if (transitioned) {
            setEnraged();
        }
    }
}

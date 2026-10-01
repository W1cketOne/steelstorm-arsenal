package com.steelstorm.arsenal.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

/** A Steelstorm enemy with a boss bar shown to players nearby. Never despawns. */
public abstract class SteelstormBoss extends SteelstormMonster {
    private static final double BAR_SHOW_DISTANCE = 40.0;
    private static final double BAR_HIDE_DISTANCE = 56.0;
    protected final ServerBossEvent bossEvent;

    protected SteelstormBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(type.getDescription(), color, overlay);
        setPersistenceRequired();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 == 0 && level() instanceof ServerLevel server) {
            // Show the bar to players who come close (or that it is fighting), hide it once they leave.
            for (ServerPlayer player : server.players()) {
                double distance = player.distanceToSqr(this);
                boolean shown = bossEvent.getPlayers().contains(player);
                if (!shown && (distance < BAR_SHOW_DISTANCE * BAR_SHOW_DISTANCE || getTarget() == player)) {
                    bossEvent.addPlayer(player);
                } else if (shown && distance > BAR_HIDE_DISTANCE * BAR_HIDE_DISTANCE && getTarget() != player) {
                    bossEvent.removePlayer(player);
                }
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}

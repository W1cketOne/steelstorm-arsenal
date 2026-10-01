package com.steelstorm.arsenal.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Base for the mod's humanoid enemies. {@link #isCharging()} is synced to clients so the model can
 * raise its arms while a big attack winds up: that is the telegraph players learn to dodge or parry.
 */
public abstract class SteelstormMonster extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(SteelstormMonster.class, EntityDataSerializers.BOOLEAN);

    protected SteelstormMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
    }

    public boolean isCharging() {
        return entityData.get(DATA_CHARGING);
    }

    public void setCharging(boolean charging) {
        entityData.set(DATA_CHARGING, charging);
    }
}

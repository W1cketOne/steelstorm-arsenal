package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.registry.ModItems;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A straw training dummy. It never dies: every hit shows the damage dealt (and damage per second
 * over the current burst) above its head, then it heals back to full. Sneak-attack it to pick it up.
 */
public class TargetDummyEntity extends Mob {
    private static final int RESET_TICKS = 60;
    private float burstDamage;
    private long burstStart;
    private long lastHit = -1000;

    public TargetDummyEntity(EntityType<? extends TargetDummyEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setCustomNameVisible(true);
        setCustomName(defaultName());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    private static Component defaultName() {
        return Component.translatable("entity.steelstorm.target_dummy").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player && player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty()) {
            // Sneak-punch with an empty hand to pick the dummy up.
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(ModItems.TARGET_DUMMY.get()));
            }
            playSound(SoundEvents.ARMOR_STAND_BREAK, 1.0F, 1.0F);
            discard();
            return true;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        float before = getHealth();
        boolean result = super.hurt(source, amount);
        float dealt = before - getHealth();
        if (dealt > 0) {
            showDamage(dealt);
        }
        setHealth(getMaxHealth());
        return result;
    }

    private void showDamage(float dealt) {
        long now = level().getGameTime();
        if (now - lastHit > RESET_TICKS) {
            burstDamage = 0;
            burstStart = now;
        }
        burstDamage += dealt;
        lastHit = now;
        float seconds = Math.max(1.0F, (now - burstStart) / 20.0F);
        setCustomName(Component.literal(String.format(Locale.ROOT, "-%.1f", dealt)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(String.format(Locale.ROOT, "  %.1f DPS", burstDamage / seconds))
                        .withStyle(ChatFormatting.GRAY)));
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY(0.8), getZ(), Math.min(10, 1 + (int) dealt), 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && lastHit > 0 && level().getGameTime() - lastHit == RESET_TICKS) {
            setCustomName(defaultName());
        }
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    /** /kill and the void remove the dummy outright instead of leaving it stuck mid-death. */
    @Override
    public void kill() {
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOL_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.TARGET_DUMMY.get());
    }

}

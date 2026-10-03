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
                    if (introduced.add(player.getUUID())) {
                        introduce(player);
                    }
                } else if (shown && distance > BAR_HIDE_DISTANCE * BAR_HIDE_DISTANCE && getTarget() != player) {
                    bossEvent.removePlayer(player);
                }
            }
        }
    }

    private final java.util.Set<java.util.UUID> introduced = new java.util.HashSet<>();

    /** A film-style title card the first time a player meets this boss. */
    private void introduce(ServerPlayer player) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        String epithet = switch (id) {
            case "forge_colossus" -> "The Molten King";
            case "moonblade_revenant" -> "Blade of the Pale Moon";
            case "fallen_warlord" -> "Tyrant of the Fallen Keep";
            case "storm_herald" -> "Voice of the Tempest";
            case "iron_revenant" -> "The Unbroken Guard";
            default -> "Champion of Steel";
        };
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(8, 50, 16));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                getType().getDescription().copy().withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                net.minecraft.network.chat.Component.literal("— " + epithet + " —").withStyle(net.minecraft.ChatFormatting.GRAY,
                        net.minecraft.ChatFormatting.ITALIC)));
        player.playNotifySound(com.steelstorm.arsenal.registry.ModSounds.ULTIMATE_RELEASE.get(), net.minecraft.sounds.SoundSource.HOSTILE,
                0.9F, 0.6F);
        com.steelstorm.arsenal.combat.Stamina.shake(player, 0.6F, 14);
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

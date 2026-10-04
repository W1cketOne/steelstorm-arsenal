package com.steelstorm.arsenal.block;

import com.steelstorm.arsenal.combat.Stamina;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModBlockEntities;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Runs an arena challenge: three waves of enemies spawn around the gong; when the last one falls,
 * every locked Champion's Coffer within 16 blocks opens. The challenge is abandoned if every
 * player leaves, and the gong rests for a few minutes after a victory.
 */
public class ArenaGongBlockEntity extends BlockEntity {
    private static final int WAVES = 3;
    private static final int REST_AFTER_VICTORY = 20 * 60 * 5;
    private static final double ARENA_RADIUS = 40;

    private int wave;
    private int breather;
    private int emptyTicks;
    private long restUntil;
    private int waveSize = 1;
    private final List<UUID> spawned = new ArrayList<>();
    @Nullable
    private UUID challenger;
    @Nullable
    private ServerBossEvent bar;

    public ArenaGongBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARENA_GONG.get(), pos, state);
    }

    private static List<Supplier<? extends EntityType<? extends Mob>>> composition(int wave) {
        return switch (wave) {
            case 1 -> List.of(ModEntities.BANDIT_DUELIST, ModEntities.BANDIT_DUELIST, ModEntities.BANDIT_DUELIST, ModEntities.BANDIT_ARCHER);
            case 2 -> List.of(ModEntities.BANDIT_DUELIST, ModEntities.BANDIT_DUELIST, ModEntities.BANDIT_ARCHER, ModEntities.BANDIT_ARCHER,
                    ModEntities.IRON_REVENANT);
            default -> List.of(ModEntities.IRON_REVENANT, ModEntities.IRON_REVENANT, ModEntities.BANDIT_ARCHER, ModEntities.BANDIT_CAPTAIN);
        };
    }

    public void strike(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        Vec3 c = Vec3.atCenterOf(worldPosition);
        Fx.sound(level, c, ModSounds.BLOCK_GONG, 3.0F, wave > 0 ? 1.2F : 1.0F);
        Fx.ring(level, Vec3.atBottomCenterOf(worldPosition), Fx.GOLD, 6.0F);
        Fx.burst(level, ModParticles.GLOW.get(), Fx.GOLD, 1.4F, c.add(0, 0.5, 0), 12, 0.4, 0.05);
        if (wave > 0) {
            player.displayClientMessage(Component.translatable("message.steelstorm.gong_active").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        long now = level.getGameTime();
        if (now < restUntil) {
            player.displayClientMessage(Component.translatable("message.steelstorm.gong_resting", (restUntil - now) / 20 / 60 + 1)
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        challenger = player.getUUID();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(c) < 24 * 24) {
                Stamina.shake(p, 0.6F, 10);
            }
        }
        startWave(level, 1);
    }

    private void startWave(ServerLevel level, int number) {
        wave = number;
        spawned.clear();
        List<Supplier<? extends EntityType<? extends Mob>>> types = composition(number);
        Entity target = challenger == null ? null : level.getEntity(challenger);
        for (Supplier<? extends EntityType<? extends Mob>> type : types) {
            Mob mob = type.get().create(level);
            if (mob == null) {
                continue;
            }
            Vec3 spot = findSpawn(level, mob);
            mob.moveTo(spot.x, spot.y, spot.z, level.random.nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), MobSpawnType.EVENT, null);
            mob.setPersistenceRequired();
            if (target instanceof net.minecraft.world.entity.LivingEntity living && !(target instanceof ServerPlayer p && p.isCreative())) {
                mob.setTarget(living);
            }
            level.addFreshEntity(mob);
            spawned.add(mob.getUUID());
            Fx.burst(level, ModParticles.SMOKE.get(), 0x3A302A, 1.5F, spot.add(0, 0.8, 0), 12, 0.3, 0.5, 0.3, 0.02);
            Fx.burst(level, ModParticles.GLOW.get(), 0xFF6B6B, 1.2F, spot.add(0, 1.0, 0), 8, 0.3, 0.5, 0.3, 0.04);
        }
        waveSize = Math.max(1, spawned.size());
        bar().setName(Component.translatable("boss.steelstorm.arena_wave", number, WAVES));
        bar().setProgress(1.0F);
        title(level, Component.translatable("title.steelstorm.wave", number).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable(number == WAVES ? "title.steelstorm.final_wave" : "title.steelstorm.wave_sub")
                        .withStyle(ChatFormatting.GRAY));
        setChanged();
    }

    /** A free spot on solid ground 5-10 blocks from the gong, or next to the gong if none is found. */
    private Vec3 findSpawn(ServerLevel level, Mob mob) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 5 + level.random.nextDouble() * 5;
            double x = worldPosition.getX() + 0.5 + Math.cos(a) * r;
            double z = worldPosition.getZ() + 0.5 + Math.sin(a) * r;
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos feet = BlockPos.containing(x, worldPosition.getY() + dy, z);
                if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                        && level.noCollision(mob.getType().getSpawnAABB(x, feet.getY(), z))) {
                    return new Vec3(x, feet.getY(), z);
                }
            }
        }
        return Vec3.atBottomCenterOf(worldPosition.above());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArenaGongBlockEntity gong) {
        if (gong.wave <= 0 || !(level instanceof ServerLevel server) || server.getGameTime() % 5 != 0) {
            return;
        }
        gong.tickChallenge(server);
    }

    private void tickChallenge(ServerLevel level) {
        Vec3 c = Vec3.atCenterOf(worldPosition);
        List<ServerPlayer> nearby = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(c) < ARENA_RADIUS * ARENA_RADIUS) {
                nearby.add(p);
            }
        }
        ServerBossEvent bar = bar();
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
            if (!nearby.contains(p)) {
                bar.removePlayer(p);
            }
        }
        nearby.forEach(bar::addPlayer);
        if (nearby.isEmpty()) {
            emptyTicks += 5;
            if (emptyTicks > 20 * 30) {
                abandon(level);
            }
            return;
        }
        emptyTicks = 0;
        if (breather > 0) {
            breather -= 5;
            if (breather <= 0) {
                startWave(level, wave + 1);
            }
            return;
        }
        spawned.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        bar.setProgress(spawned.size() / (float) waveSize);
        if (!spawned.isEmpty()) {
            return;
        }
        if (wave >= WAVES) {
            victory(level, nearby);
        } else {
            breather = 60;
            Fx.sound(level, c, ModSounds.BLOCK_GONG, 2.0F, 1.3F);
            title(level, Component.translatable("title.steelstorm.wave_clear").withStyle(ChatFormatting.GREEN),
                    Component.translatable("title.steelstorm.next_wave").withStyle(ChatFormatting.GRAY));
        }
    }

    private void victory(ServerLevel level, List<ServerPlayer> players) {
        wave = 0;
        restUntil = level.getGameTime() + REST_AFTER_VICTORY;
        clearBar();
        Vec3 c = Vec3.atCenterOf(worldPosition);
        int opened = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-16, -6, -16), worldPosition.offset(16, 6, 16))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof LockedChestBlock chest && chest.kind() == LockedChestBlock.Kind.COFFER
                    && state.getValue(LockedChestBlock.LOCKED)) {
                LockedChestBlock.unlock(level, p.immutable(), state);
                opened++;
            }
        }
        ExperienceOrb.award(level, c.add(0, 1, 0), 120);
        for (int i = 0; i < 5; i++) {
            final int n = i;
            com.steelstorm.arsenal.combat.ServerScheduler.schedule(i * 6, () -> {
                Vec3 at = c.add(level.random.nextGaussian() * 3, 3 + n, level.random.nextGaussian() * 3);
                Fx.burst(level, ModParticles.GLOW.get(), n % 2 == 0 ? Fx.GOLD : 0xFFFFFF, 2.0F, at, 30, 0.2, 0.25);
                Fx.sparks(level, Fx.GOLD, at, 20, 0.8);
            });
        }
        Fx.sound(level, c, ModSounds.BLOCK_GONG, 3.0F, 0.8F);
        Fx.sound(level, c, ModSounds.ULTIMATE_READY, 2.0F, 1.0F);
        title(level, Component.translatable("title.steelstorm.victory").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable(opened > 0 ? "title.steelstorm.coffer_open" : "title.steelstorm.victory_sub")
                        .withStyle(ChatFormatting.YELLOW));
        setChanged();
    }

    private void abandon(ServerLevel level) {
        for (UUID id : spawned) {
            Entity e = level.getEntity(id);
            if (e != null) {
                Fx.burst(level, ModParticles.SMOKE.get(), 0x3A302A, 1.4F, e.position().add(0, 1, 0), 10, 0.3, 0.02);
                e.discard();
            }
        }
        spawned.clear();
        wave = 0;
        breather = 0;
        emptyTicks = 0;
        clearBar();
        setChanged();
    }

    private void title(ServerLevel level, Component title, Component subtitle) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < ARENA_RADIUS * ARENA_RADIUS) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
                p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                p.connection.send(new ClientboundSetTitleTextPacket(title));
            }
        }
    }

    private ServerBossEvent bar() {
        if (bar == null) {
            bar = new ServerBossEvent(Component.translatable("boss.steelstorm.arena_wave", Math.max(1, wave), WAVES),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }
        return bar;
    }

    private void clearBar() {
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        clearBar();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Wave", wave);
        tag.putInt("Breather", breather);
        tag.putLong("RestUntil", restUntil);
        tag.putInt("WaveSize", waveSize);
        ListTag list = new ListTag();
        for (UUID id : spawned) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Spawned", list);
        if (challenger != null) {
            tag.putUUID("Challenger", challenger);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        wave = tag.getInt("Wave");
        breather = tag.getInt("Breather");
        restUntil = tag.getLong("RestUntil");
        waveSize = Math.max(1, tag.getInt("WaveSize"));
        spawned.clear();
        for (Tag t : tag.getList("Spawned", Tag.TAG_INT_ARRAY)) {
            spawned.add(NbtUtils.loadUUID(t));
        }
        challenger = tag.hasUUID("Challenger") ? tag.getUUID("Challenger") : null;
    }
}

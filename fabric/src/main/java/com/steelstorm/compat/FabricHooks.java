package com.steelstorm.compat;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.compat.neo.neoforge.event.entity.EntityAttributeCreationEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingDeathEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.player.AttackEntityEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.player.CriticalHitEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.player.PlayerEvent;
import com.steelstorm.compat.neo.neoforge.event.server.ServerStartedEvent;
import com.steelstorm.compat.neo.neoforge.event.server.ServerStoppedEvent;
import com.steelstorm.compat.neo.neoforge.event.server.ServerStoppingEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.LevelTickEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.ServerTickEvent;
import com.steelstorm.compat.neo.neoforge.event.village.VillagerTradesEvent;
import com.steelstorm.compat.neo.neoforge.network.event.RegisterPayloadHandlersEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.levelgen.GenerationStep;

/** Turns Fabric API callbacks into the NeoForge-style events the mod's handlers expect. */
public final class FabricHooks {
    /** Set by the client; tooltips built on the server never show details. */
    public static java.util.function.BooleanSupplier shiftDown = () -> false;

    public static void init() {
        Subscribers.register(Subscribers.COMMON);

        PayloadTypeRegistry.playS2C().register(SpawnDataPayload.TYPE, SpawnDataPayload.STREAM_CODEC);
        NeoBus.post(new RegisterPayloadHandlersEvent());
        NeoBus.post(new EntityAttributeCreationEvent());
        NeoBus.post(new RegisterSpawnPlacementsEvent());
        registerTrades();

        BiomeModifications.addFeature(
                BiomeSelectors.tag(TagKey.create(Registries.BIOME, SteelstormArsenal.id("has_stormsteel_ore"))),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, SteelstormArsenal.id("ore_stormsteel")));
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(TagKey.create(Registries.BIOME, SteelstormArsenal.id("spawns_iron_revenant"))),
                MobCategory.MONSTER, ModEntities.IRON_REVENANT.get(), 10, 1, 1);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> NeoBus.post(new ServerStartedEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> NeoBus.post(new ServerStoppingEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NeoBus.post(new ServerStoppedEvent(server)));
        ServerTickEvents.START_SERVER_TICK.register(server -> NeoBus.post(new ServerTickEvent.Pre(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> NeoBus.post(new ServerTickEvent.Post(server)));
        ServerTickEvents.START_WORLD_TICK.register(level -> NeoBus.post(new LevelTickEvent.Pre(level)));
        ServerTickEvents.END_WORLD_TICK.register(level -> NeoBus.post(new LevelTickEvent.Post(level)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                NeoBus.post(new PlayerEvent.PlayerLoggedInEvent(handler.getPlayer())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                NeoBus.post(new PlayerEvent.PlayerLoggedOutEvent(handler.getPlayer())));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                NeoBus.post(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive)));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                NeoBus.post(new PlayerEvent.PlayerChangedDimensionEvent(player, origin.dimension(), destination.dimension())));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !NeoBus.post(new LivingDeathEvent(entity, source)).isCanceled());

        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (NeoBus.post(new AttackEntityEvent(player, target)).isCanceled()) {
                return InteractionResult.FAIL;
            }
            // Vanilla's critical-hit rules, evaluated before the attack resets the charge.
            boolean crit = player.getAttackStrengthScale(0.5F) > 0.9F && player.fallDistance > 0.0F && !player.onGround()
                    && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS)
                    && !player.isPassenger() && target instanceof LivingEntity && !player.isSprinting();
            NeoBus.post(new CriticalHitEvent(player, target, crit));
            return InteractionResult.PASS;
        });
    }

    private static void registerTrades() {
        VillagerProfession profession = VillagerProfession.WEAPONSMITH;
        Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) {
            trades.put(level, new ArrayList<>());
        }
        NeoBus.post(new VillagerTradesEvent(trades, profession));
        for (int level = 1; level <= 5; level++) {
            List<VillagerTrades.ItemListing> added = trades.get(level);
            if (!added.isEmpty()) {
                TradeOfferHelper.registerVillagerOffers(profession, level, factories -> factories.addAll(added));
            }
        }
    }

    private FabricHooks() {
    }
}

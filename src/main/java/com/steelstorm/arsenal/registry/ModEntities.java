package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.entity.AuraFxEntity;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.BanditArcher;
import com.steelstorm.arsenal.entity.BanditCaptain;
import com.steelstorm.arsenal.entity.BanditDuelist;
import com.steelstorm.arsenal.entity.CryptKnight;
import com.steelstorm.arsenal.entity.StormHerald;
import com.steelstorm.arsenal.entity.ChakramEntity;
import com.steelstorm.arsenal.entity.EarthChunkEntity;
import com.steelstorm.arsenal.entity.GroundWaveEntity;
import com.steelstorm.arsenal.entity.OrbitBladesEntity;
import com.steelstorm.arsenal.entity.SlashWaveEntity;
import com.steelstorm.arsenal.entity.SpectralWeaponEntity;
import com.steelstorm.arsenal.entity.ThrownHammerEntity;
import com.steelstorm.arsenal.entity.VortexEntity;
import com.steelstorm.arsenal.entity.FallenWarlord;
import com.steelstorm.arsenal.entity.IronRevenant;
import java.util.List;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import com.steelstorm.arsenal.entity.TargetDummyEntity;
import com.steelstorm.arsenal.entity.ThrowingKnifeEntity;
import com.steelstorm.arsenal.entity.ThrownSpear;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, SteelstormArsenal.MODID);

    public static final Supplier<EntityType<ThrownSpear>> THROWN_SPEAR = ENTITIES.register("thrown_spear",
            () -> EntityType.Builder.<ThrownSpear>of(ThrownSpear::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("thrown_spear"));
    public static final Supplier<EntityType<ThrowingKnifeEntity>> THROWING_KNIFE = ENTITIES.register("throwing_knife",
            () -> EntityType.Builder.<ThrowingKnifeEntity>of(ThrowingKnifeEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(20).build("throwing_knife"));
    public static final Supplier<EntityType<ChakramEntity>> CHAKRAM = ENTITIES.register("chakram",
            () -> EntityType.Builder.<ChakramEntity>of(ChakramEntity::new, MobCategory.MISC)
                    .sized(0.7F, 0.25F).clientTrackingRange(6).updateInterval(1).build("chakram"));
    public static final Supplier<EntityType<ThrownHammerEntity>> THROWN_HAMMER = ENTITIES.register("thrown_hammer",
            () -> EntityType.Builder.<ThrownHammerEntity>of(ThrownHammerEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(6).updateInterval(1).build("thrown_hammer"));

    // Ability visuals. None of these are saved; the client animates most of them on its own.
    public static final Supplier<EntityType<GroundWaveEntity>> GROUND_WAVE = ENTITIES.register("ground_wave",
            () -> EntityType.Builder.<GroundWaveEntity>of(GroundWaveEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(Integer.MAX_VALUE)
                    .build("ground_wave"));
    public static final Supplier<EntityType<EarthChunkEntity>> EARTH_CHUNK = ENTITIES.register("earth_chunk",
            () -> EntityType.Builder.<EarthChunkEntity>of(EarthChunkEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).noSave().noSummon().fireImmune().clientTrackingRange(6).updateInterval(Integer.MAX_VALUE)
                    .build("earth_chunk"));
    public static final Supplier<EntityType<SlashWaveEntity>> SLASH_WAVE = ENTITIES.register("slash_wave",
            () -> EntityType.Builder.<SlashWaveEntity>of(SlashWaveEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(6).updateInterval(1).build("slash_wave"));
    public static final Supplier<EntityType<SpectralWeaponEntity>> SPECTRAL_WEAPON = ENTITIES.register("spectral_weapon",
            () -> EntityType.Builder.<SpectralWeaponEntity>of(SpectralWeaponEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(Integer.MAX_VALUE)
                    .build("spectral_weapon"));
    public static final Supplier<EntityType<VortexEntity>> VORTEX = ENTITIES.register("vortex",
            () -> EntityType.Builder.<VortexEntity>of(VortexEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(Integer.MAX_VALUE)
                    .build("vortex"));
    public static final Supplier<EntityType<OrbitBladesEntity>> ORBIT_BLADES = ENTITIES.register("orbit_blades",
            () -> EntityType.Builder.<OrbitBladesEntity>of(OrbitBladesEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(6).updateInterval(Integer.MAX_VALUE)
                    .build("orbit_blades"));

    public static final Supplier<EntityType<AuraFxEntity>> AURA_FX = ENTITIES.register("aura_fx",
            () -> EntityType.Builder.<AuraFxEntity>of(AuraFxEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
                    .build("aura_fx"));

    public static final Supplier<EntityType<TargetDummyEntity>> TARGET_DUMMY = ENTITIES.register("target_dummy",
            () -> EntityType.Builder.<TargetDummyEntity>of(TargetDummyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("target_dummy"));

    public static final Supplier<EntityType<BanditDuelist>> BANDIT_DUELIST = ENTITIES.register("bandit_duelist",
            () -> EntityType.Builder.<BanditDuelist>of(BanditDuelist::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("bandit_duelist"));
    public static final Supplier<EntityType<BanditArcher>> BANDIT_ARCHER = ENTITIES.register("bandit_archer",
            () -> EntityType.Builder.<BanditArcher>of(BanditArcher::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("bandit_archer"));
    public static final Supplier<EntityType<IronRevenant>> IRON_REVENANT = ENTITIES.register("iron_revenant",
            () -> EntityType.Builder.<IronRevenant>of(IronRevenant::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.05F).clientTrackingRange(8).build("iron_revenant"));
    public static final Supplier<EntityType<FallenWarlord>> FALLEN_WARLORD = ENTITIES.register("fallen_warlord",
            () -> EntityType.Builder.<FallenWarlord>of(FallenWarlord::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.8F).fireImmune().clientTrackingRange(10).build("fallen_warlord"));

    public static final Supplier<EntityType<BanditCaptain>> BANDIT_CAPTAIN = ENTITIES.register("bandit_captain",
            () -> EntityType.Builder.<BanditCaptain>of(BanditCaptain::new, MobCategory.MONSTER)
                    .sized(0.65F, 2.0F).clientTrackingRange(10).build("bandit_captain"));
    public static final Supplier<EntityType<CryptKnight>> CRYPT_KNIGHT = ENTITIES.register("crypt_knight",
            () -> EntityType.Builder.<CryptKnight>of(CryptKnight::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.3F).fireImmune().clientTrackingRange(10).build("crypt_knight"));
    public static final Supplier<EntityType<StormHerald>> STORM_HERALD = ENTITIES.register("storm_herald",
            () -> EntityType.Builder.<StormHerald>of(StormHerald::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.4F).fireImmune().clientTrackingRange(12).build("storm_herald"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TARGET_DUMMY.get(), TargetDummyEntity.createAttributes().build());
        event.put(BANDIT_DUELIST.get(), BanditDuelist.createAttributes().build());
        event.put(BANDIT_ARCHER.get(), BanditArcher.createAttributes().build());
        event.put(IRON_REVENANT.get(), IronRevenant.createAttributes().build());
        event.put(FALLEN_WARLORD.get(), FallenWarlord.createAttributes().build());
        event.put(BANDIT_CAPTAIN.get(), BanditCaptain.createAttributes().build());
        event.put(CRYPT_KNIGHT.get(), CryptKnight.createAttributes().build());
        event.put(STORM_HERALD.get(), StormHerald.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (Supplier<? extends EntityType<? extends Monster>> type : List.of(BANDIT_DUELIST, BANDIT_ARCHER, IRON_REVENANT)) {
            event.register(type.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private ModEntities() {
    }
}

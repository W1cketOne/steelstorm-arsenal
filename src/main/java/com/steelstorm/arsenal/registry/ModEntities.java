package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.BanditArcher;
import com.steelstorm.arsenal.entity.BanditDuelist;
import com.steelstorm.arsenal.entity.ChakramEntity;
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

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TARGET_DUMMY.get(), TargetDummyEntity.createAttributes().build());
        event.put(BANDIT_DUELIST.get(), BanditDuelist.createAttributes().build());
        event.put(BANDIT_ARCHER.get(), BanditArcher.createAttributes().build());
        event.put(IRON_REVENANT.get(), IronRevenant.createAttributes().build());
        event.put(FALLEN_WARLORD.get(), FallenWarlord.createAttributes().build());
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

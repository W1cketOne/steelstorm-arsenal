package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.ChakramEntity;
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

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TARGET_DUMMY.get(), TargetDummyEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}

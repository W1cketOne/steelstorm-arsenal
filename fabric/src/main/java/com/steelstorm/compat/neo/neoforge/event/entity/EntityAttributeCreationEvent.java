package com.steelstorm.compat.neo.neoforge.event.entity;

import com.steelstorm.compat.neo.bus.api.Event;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class EntityAttributeCreationEvent extends Event {
    public void put(EntityType<? extends LivingEntity> type, AttributeSupplier attributes) {
        FabricDefaultAttributeRegistry.register(type, attributes);
    }
}

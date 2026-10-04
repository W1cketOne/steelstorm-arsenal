package com.steelstorm.compat.neo.neoforge.registries;

import com.steelstorm.compat.neo.bus.api.IEventBus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Collects entries and registers them into the vanilla registries when register(bus) is called. */
public class DeferredRegister<T> {
    protected final ResourceKey<? extends Registry<T>> registryKey;
    protected final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private final List<DeferredHolder<T, ? extends T>> order = new ArrayList<>();
    private boolean registered;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace) {
        return new DeferredRegister<>(key, namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    public static DataComponents createDataComponents(ResourceKey<? extends Registry<DataComponentType<?>>> key, String namespace) {
        return new DataComponents(namespace);
    }

    protected <I extends T, H extends DeferredHolder<T, I>> H add(H holder, Supplier<? extends I> supplier) {
        if (registered) {
            throw new IllegalStateException("Cannot add " + holder.getId() + " after registration");
        }
        entries.put(holder, supplier);
        order.add(holder);
        return holder;
    }

    @SuppressWarnings("unchecked")
    protected ResourceKey<T> key(String name) {
        return ResourceKey.create((ResourceKey<? extends Registry<T>>) registryKey, ResourceLocation.fromNamespaceAndPath(namespace, name));
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        return add(new DeferredHolder<>(key(name)), supplier);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<ResourceLocation, ? extends I> factory) {
        ResourceKey<T> k = key(name);
        return add(new DeferredHolder<>(k), () -> factory.apply(k.location()));
    }

    @SuppressWarnings("unchecked")
    public void register(IEventBus bus) {
        if (registered) {
            return;
        }
        registered = true;
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null) {
            throw new IllegalStateException("Unknown registry " + registryKey);
        }
        for (DeferredHolder<T, ? extends T> holder : order) {
            T value = entries.get(holder).get();
            holder.ref = Registry.registerForHolder(registry, holder.key, value);
        }
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return Collections.unmodifiableList(order);
    }

    public static class Items extends DeferredRegister<Item> {
        Items(String namespace) {
            super(Registries.ITEM, namespace);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> supplier) {
            return add(new DeferredItem<>(key(name)), supplier);
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return register(name, () -> new Item(new Item.Properties()));
        }

        public DeferredItem<Item> registerSimpleItem(String name, Item.Properties props) {
            return register(name, () -> new Item(props));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) {
            return register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Item.Properties properties) {
            return register(name, () -> new BlockItem(block.get(), properties));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredHolder<Block, ? extends Block> block) {
            return registerSimpleBlockItem(block.getId().getPath(), block);
        }
    }

    public static class Blocks extends DeferredRegister<Block> {
        Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> supplier) {
            return add(new DeferredBlock<>(key(name)), supplier);
        }
    }

    public static class DataComponents extends DeferredRegister<DataComponentType<?>> {
        DataComponents(String namespace) {
            super(Registries.DATA_COMPONENT_TYPE, namespace);
        }

        public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(String name,
                UnaryOperator<DataComponentType.Builder<D>> builder) {
            return register(name, () -> builder.apply(DataComponentType.builder()).build());
        }
    }
}

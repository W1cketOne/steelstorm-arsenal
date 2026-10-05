package com.steelstorm.compat.neo.neoforge.registries;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/** A registry entry that is filled in when its DeferredRegister registers; usable as a Holder. */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    protected final ResourceKey<R> key;
    Holder.Reference<R> ref;

    DeferredHolder(ResourceKey<R> key) {
        this.key = key;
    }

    private Holder.Reference<R> ref() {
        if (ref == null) {
            throw new IllegalStateException("Registry entry " + key + " used before registration");
        }
        return ref;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        return (T) ref().value();
    }

    /** The registry's own reference, which save codecs require; this holder until registration. */
    public Holder<R> delegate() {
        return ref != null ? ref : this;
    }

    public ResourceLocation getId() {
        return key.location();
    }

    public ResourceKey<R> getKey() {
        return key;
    }

    @Override
    public R value() {
        return ref().value();
    }

    @Override
    public boolean isBound() {
        return ref != null && ref.isBound();
    }

    @Override
    public boolean is(ResourceLocation id) {
        return key.location().equals(id);
    }

    @Override
    public boolean is(ResourceKey<R> k) {
        return key.equals(k);
    }

    @Override
    public boolean is(Predicate<ResourceKey<R>> predicate) {
        return predicate.test(key);
    }

    @Override
    public boolean is(TagKey<R> tag) {
        return ref().is(tag);
    }

    @Override
    public boolean is(Holder<R> other) {
        return ref().is(other);
    }

    @Override
    public Stream<TagKey<R>> tags() {
        return ref().tags();
    }

    @Override
    public Either<ResourceKey<R>, R> unwrap() {
        return Either.left(key);
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(key);
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<R> owner) {
        return ref().canSerializeIn(owner);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Holder<?> h) {
            return h.unwrapKey().map(key::equals).orElse(false);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}

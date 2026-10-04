package com.steelstorm.compat.neo.neoforge.common;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

/** A small properties-file config with the same builder API the mod uses. */
public class ModConfigSpec {
    private final List<ConfigValue<?>> values;

    private ModConfigSpec(List<ConfigValue<?>> values) {
        this.values = values;
    }

    public void load(String fileName) {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(fileName);
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                props.load(r);
            } catch (IOException ignored) {
            }
        }
        for (ConfigValue<?> v : values) {
            String raw = props.getProperty(v.path);
            if (raw != null) {
                v.parse(raw.trim());
            }
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                w.write("# Steelstorm Arsenal configuration\n");
                for (ConfigValue<?> v : values) {
                    if (!v.comment.isEmpty()) {
                        w.write("\n# " + v.comment.replace("\n", "\n# ") + "\n");
                    }
                    w.write(v.path + "=" + v.get() + "\n");
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static class ConfigValue<T> implements Supplier<T> {
        final String path;
        final String comment;
        T value;
        final java.util.function.Function<String, T> parser;
        final java.util.function.Predicate<T> valid;

        ConfigValue(String path, String comment, T def, java.util.function.Function<String, T> parser, java.util.function.Predicate<T> valid) {
            this.path = path;
            this.comment = comment;
            this.value = def;
            this.parser = parser;
            this.valid = valid;
        }

        void parse(String raw) {
            try {
                T v = parser.apply(raw);
                if (valid.test(v)) {
                    value = v;
                }
            } catch (RuntimeException ignored) {
            }
        }

        @Override
        public T get() {
            return value;
        }

        public void set(T v) {
            value = v;
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String path, String comment, boolean def) {
            super(path, comment, def, Boolean::parseBoolean, v -> true);
        }
    }

    public static class IntValue extends ConfigValue<Integer> {
        IntValue(String path, String comment, int def, int min, int max) {
            super(path, comment, def, Integer::parseInt, v -> v >= min && v <= max);
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        DoubleValue(String path, String comment, double def, double min, double max) {
            super(path, comment, def, Double::parseDouble, v -> v >= min && v <= max);
        }
    }

    public static class Builder {
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();
        private String comment = "";

        public Builder comment(String... lines) {
            comment = String.join("\n", lines);
            return this;
        }

        public Builder push(String name) {
            path.add(name);
            comment = "";
            return this;
        }

        public Builder pop() {
            if (!path.isEmpty()) {
                path.remove(path.size() - 1);
            }
            return this;
        }

        private String key(String name) {
            return path.isEmpty() ? name : String.join(".", path) + "." + name;
        }

        private <V extends ConfigValue<?>> V add(V v) {
            values.add(v);
            comment = "";
            return v;
        }

        public BooleanValue define(String name, boolean def) {
            return add(new BooleanValue(key(name), comment, def));
        }

        public IntValue defineInRange(String name, int def, int min, int max) {
            return add(new IntValue(key(name), comment + " [" + min + ".." + max + "]", def, min, max));
        }

        public DoubleValue defineInRange(String name, double def, double min, double max) {
            return add(new DoubleValue(key(name), comment + " [" + min + ".." + max + "]", def, min, max));
        }

        public ModConfigSpec build() {
            return new ModConfigSpec(values);
        }
    }
}

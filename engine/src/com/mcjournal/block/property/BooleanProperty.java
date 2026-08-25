package com.mcjournal.block.property;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BooleanProperty implements Property<Boolean> {
    private static final List<Boolean> VALUES = List.of(true, false);
    private final String name;

    protected BooleanProperty(String name) {
        this.name = name;
    }

    public static BooleanProperty of(String name) {
        return new BooleanProperty(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Boolean> getValueClass() {
        return Boolean.class;
    }

    @Override
    public Collection<Boolean> getAllowedValues() {
        return VALUES;
    }

    @Override
    public String getName(Boolean value) {
        return value.toString();
    }

    @Override
    public Optional<Boolean> parseValue(String name) {
        if ("true".equalsIgnoreCase(name)) return Optional.of(true);
        if ("false".equalsIgnoreCase(name)) return Optional.of(false);
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BooleanProperty that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "BooleanProperty{" + "name='" + name + '\'' + '}';
    }
}

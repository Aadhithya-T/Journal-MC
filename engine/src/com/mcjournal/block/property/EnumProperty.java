package com.mcjournal.block.property;

import java.util.*;

public class EnumProperty<T extends Enum<T> & Comparable<T>> implements Property<T> {
    private final String name;
    private final Class<T> valueClass;
    private final List<T> values;
    private final Map<String, T> nameToValue = new HashMap<>();

    protected EnumProperty(String name, Class<T> valueClass, Collection<T> values) {
        this.name = name;
        this.valueClass = valueClass;
        this.values = List.copyOf(values);
        for (T value : values) {
            String valName = value.toString().toLowerCase(Locale.ROOT);
            this.nameToValue.put(valName, value);
        }
    }

    public static <T extends Enum<T> & Comparable<T>> EnumProperty<T> of(String name, Class<T> valueClass) {
        return new EnumProperty<>(name, valueClass, Arrays.asList(valueClass.getEnumConstants()));
    }

    public static <T extends Enum<T> & Comparable<T>> EnumProperty<T> of(String name, Class<T> valueClass, Collection<T> values) {
        return new EnumProperty<>(name, valueClass, values);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getValueClass() {
        return valueClass;
    }

    @Override
    public Collection<T> getAllowedValues() {
        return values;
    }

    @Override
    public String getName(T value) {
        return value.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public Optional<T> parseValue(String name) {
        return Optional.ofNullable(nameToValue.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnumProperty<?> that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(valueClass, that.valueClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueClass);
    }

    @Override
    public String toString() {
        return "EnumProperty{" + "name='" + name + '\'' + ", values=" + values + '}';
    }
}

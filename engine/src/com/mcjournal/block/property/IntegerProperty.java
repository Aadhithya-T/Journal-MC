package com.mcjournal.block.property;

import java.util.*;

public class IntegerProperty implements Property<Integer> {
    private final String name;
    private final int min;
    private final int max;
    private final List<Integer> values;

    protected IntegerProperty(String name, int min, int max) {
        if (min < 0) throw new IllegalArgumentException("Min value must be >= 0: " + min);
        if (max <= min) throw new IllegalArgumentException("Max value must be > min value: " + max);
        this.name = name;
        this.min = min;
        this.max = max;

        List<Integer> list = new ArrayList<>(max - min + 1);
        for (int i = min; i <= max; i++) {
            list.add(i);
        }
        this.values = List.copyOf(list);
    }

    public static IntegerProperty of(String name, int min, int max) {
        return new IntegerProperty(name, min, max);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Integer> getValueClass() {
        return Integer.class;
    }

    @Override
    public Collection<Integer> getAllowedValues() {
        return values;
    }

    @Override
    public String getName(Integer value) {
        return value.toString();
    }

    @Override
    public Optional<Integer> parseValue(String name) {
        try {
            int val = Integer.parseInt(name);
            if (val >= min && val <= max) {
                return Optional.of(val);
            }
        } catch (NumberFormatException ignored) {}
        return Optional.empty();
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntegerProperty that)) return false;
        return min == that.min && max == that.max && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, min, max);
    }

    @Override
    public String toString() {
        return "IntegerProperty{" + "name='" + name + '\'' + ", min=" + min + ", max=" + max + '}';
    }
}

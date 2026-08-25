package com.mcjournal.block.property;

import java.util.Collection;
import java.util.Optional;

public interface Property<T extends Comparable<T>> {
    /**
     * @return The name of this property (e.g., "axis", "facing", "level", "waterlogged").
     */
    String getName();

    /**
     * @return The class type of values this property holds.
     */
    Class<T> getValueClass();

    /**
     * @return All allowed values for this property.
     */
    Collection<T> getAllowedValues();

    /**
     * Converts a value to its serialized string representation.
     */
    String getName(T value);

    /**
     * Parses a value from its serialized string representation.
     */
    Optional<T> parseValue(String name);
}

package com.mcjournal.world.structure;

import java.util.*;

/**
 * Thread-safe registry for StructureDefinition types.
 * Enforces unique identifiers and parameter validation.
 * Holds no permanent gameplay structures until explicit registration.
 */
public class StructureRegistry {

    private final Map<String, StructureDefinition> definitions = new LinkedHashMap<>();

    public StructureRegistry() {}

    /**
     * Registers a structure definition.
     * Rejects duplicate IDs and null definitions.
     */
    public synchronized void register(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Cannot register null StructureDefinition");
        }
        if (definitions.containsKey(definition.getId())) {
            throw new IllegalArgumentException("Duplicate StructureDefinition ID already registered: " + definition.getId());
        }
        definitions.put(definition.getId(), definition);
    }

    public synchronized StructureDefinition get(String id) {
        if (id == null) return null;
        return definitions.get(id.toLowerCase().trim());
    }

    public synchronized boolean has(String id) {
        if (id == null) return false;
        return definitions.containsKey(id.toLowerCase().trim());
    }

    public synchronized Collection<StructureDefinition> getAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(definitions.values()));
    }

    public synchronized int size() {
        return definitions.size();
    }

    public synchronized void clear() {
        definitions.clear();
    }
}

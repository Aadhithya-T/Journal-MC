package com.mcjournal.world.structure;

import java.util.Objects;

/**
 * Immutable definition of a placeable structure type, binding a 3D template to
 * placement constraints and frequency parameters.
 */
public final class StructureDefinition {

    private final String id;
    private final String displayName;
    private final StructureTemplate template;
    private final PlacementConstraints constraints;
    private final int cellSize;
    private final double spawnChance;

    public StructureDefinition(String id, String displayName, StructureTemplate template,
                               PlacementConstraints constraints, int cellSize, double spawnChance) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Structure ID cannot be null or blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("DisplayName cannot be null or blank");
        this.template = Objects.requireNonNull(template, "StructureTemplate cannot be null");
        this.constraints = (constraints != null) ? constraints : PlacementConstraints.defaults();
        if (cellSize <= 0) throw new IllegalArgumentException("Cell size must be positive, got: " + cellSize);
        if (spawnChance < 0.0 || spawnChance > 1.0) throw new IllegalArgumentException("Spawn chance must be between 0.0 and 1.0, got: " + spawnChance);

        this.id = id.toLowerCase().trim();
        this.displayName = displayName;
        this.cellSize = cellSize;
        this.spawnChance = spawnChance;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public StructureTemplate getTemplate() { return template; }
    public PlacementConstraints getConstraints() { return constraints; }
    public int getCellSize() { return cellSize; }
    public double getSpawnChance() { return spawnChance; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructureDefinition other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StructureDefinition{" + id + " ('" + displayName + "')}";
    }
}


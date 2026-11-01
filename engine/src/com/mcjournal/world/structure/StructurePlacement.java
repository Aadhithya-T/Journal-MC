package com.mcjournal.world.structure;

/**
 * Resolved placement of a structure instance in the world.
 * Holds origin coordinates, transformed template, and bounding box.
 */
public final class StructurePlacement {

    private final StructureDefinition definition;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final Rotation rotation;
    private final Mirror mirror;
    private final StructureTemplate transformedTemplate;

    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;

    public StructurePlacement(StructureDefinition definition, int originX, int originY, int originZ,
                              Rotation rotation, Mirror mirror) {
        this.definition = definition;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.rotation = (rotation != null) ? rotation : Rotation.NONE;
        this.mirror = (mirror != null) ? mirror : Mirror.NONE;

        this.transformedTemplate = definition.getTemplate().getTransformed(this.rotation, this.mirror);

        this.minX = originX;
        this.maxX = originX + transformedTemplate.getWidth() - 1;
        this.minY = originY;
        this.maxY = originY + transformedTemplate.getHeight() - 1;
        this.minZ = originZ;
        this.maxZ = originZ + transformedTemplate.getDepth() - 1;
    }

    public StructureDefinition getDefinition() { return definition; }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getOriginZ() { return originZ; }
    public Rotation getRotation() { return rotation; }
    public Mirror getMirror() { return mirror; }
    public StructureTemplate getTransformedTemplate() { return transformedTemplate; }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }

    /**
     * Checks if this structure placement's horizontal bounding box intersects chunk (cx, cz).
     */
    public boolean intersectsChunk(int cx, int cz) {
        int cMinX = cx * 16;
        int cMaxX = cMinX + 15;
        int cMinZ = cz * 16;
        int cMaxZ = cMinZ + 15;

        return minX <= cMaxX && maxX >= cMinX && minZ <= cMaxZ && maxZ >= cMinZ;
    }

    /**
     * Checks if this structure conflicts with another placement given minimum spacing.
     */
    public boolean conflictsWith(StructurePlacement other, int requiredSpacing) {
        int s = Math.max(0, requiredSpacing);
        boolean overlapX = (this.minX - s) <= other.maxX && (this.maxX + s) >= other.minX;
        boolean overlapY = (this.minY - s) <= other.maxY && (this.maxY + s) >= other.minY;
        boolean overlapZ = (this.minZ - s) <= other.maxZ && (this.maxZ + s) >= other.minZ;
        return overlapX && overlapY && overlapZ;
    }

    @Override
    public String toString() {
        return "StructurePlacement{" + definition.getId() + " at (" + originX + "," + originY + "," + originZ +
                ") [" + minX + ".." + maxX + " x " + minZ + ".." + maxZ + "], rot=" + rotation + "}";
    }
}


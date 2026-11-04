package com.mcjournal.world.landmark;

import java.util.Objects;

/**
 * Immutable landmark / Point of Interest (POI) representation within a specific world.
 */
public final class WorldLandmark {

    private final String landmarkId;
    private final String worldId;
    private final String name;
    private final String description;
    private final double x;
    private final double y;
    private final double z;
    private final LandmarkType landmarkType;
    private final DiscoveryState discoveryState;
    private final long creationTimestamp;

    public WorldLandmark(
            String landmarkId,
            String worldId,
            String name,
            String description,
            double x,
            double y,
            double z,
            LandmarkType landmarkType,
            DiscoveryState discoveryState,
            long creationTimestamp) {

        if (landmarkId == null || landmarkId.isBlank()) {
            throw new IllegalArgumentException("Landmark ID cannot be null or blank");
        }
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("World ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Landmark name cannot be null or blank");
        }

        this.landmarkId = landmarkId.trim();
        this.worldId = worldId.trim();
        this.name = name.trim();
        this.description = (description != null) ? description.trim() : "";
        this.x = x;
        this.y = y;
        this.z = z;
        this.landmarkType = (landmarkType != null) ? landmarkType : LandmarkType.NATURAL;
        this.discoveryState = (discoveryState != null) ? discoveryState : DiscoveryState.UNDISCOVERED;
        this.creationTimestamp = (creationTimestamp > 0) ? creationTimestamp : System.currentTimeMillis();
    }

    public String getLandmarkId() { return landmarkId; }
    public String getWorldId() { return worldId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public LandmarkType getLandmarkType() { return landmarkType; }
    public DiscoveryState getDiscoveryState() { return discoveryState; }
    public long getCreationTimestamp() { return creationTimestamp; }

    public boolean isDiscovered() {
        return discoveryState == DiscoveryState.DISCOVERED || discoveryState == DiscoveryState.VISITED;
    }

    public double distanceSquaredTo(double px, double py, double pz) {
        double dx = this.x - px;
        double dy = this.y - py;
        double dz = this.z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceTo(double px, double py, double pz) {
        return Math.sqrt(distanceSquaredTo(px, py, pz));
    }

    /**
     * Returns an updated landmark with a new discovery state.
     */
    public WorldLandmark withDiscoveryState(DiscoveryState newState) {
        if (this.discoveryState == newState) return this;
        return new WorldLandmark(
            this.landmarkId,
            this.worldId,
            this.name,
            this.description,
            this.x,
            this.y,
            this.z,
            this.landmarkType,
            newState,
            this.creationTimestamp
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldLandmark that = (WorldLandmark) o;
        return Objects.equals(landmarkId, that.landmarkId) && Objects.equals(worldId, that.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(landmarkId, worldId);
    }

    @Override
    public String toString() {
        return "WorldLandmark[" + landmarkId + " ('" + name + "') at (" + x + ", " + y + ", " + z + "), state=" + discoveryState + "]";
    }
}


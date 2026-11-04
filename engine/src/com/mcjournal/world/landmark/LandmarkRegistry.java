package com.mcjournal.world.landmark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mcjournal.event.EventBus;
import com.mcjournal.event.LandmarkDiscoveredEvent;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe Landmark and Point of Interest (POI) registry.
 * Manages landmark registration, proximity queries, discovery progression,
 * and independent JSON persistence.
 */
public class LandmarkRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LANDMARK_LIST_TYPE = new TypeToken<List<WorldLandmark>>() {}.getType();

    private final String worldId;
    private final Map<String, WorldLandmark> landmarks = new ConcurrentHashMap<>();

    public LandmarkRegistry(String worldId) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("World ID cannot be null or blank");
        }
        this.worldId = worldId.trim();
    }

    public String getWorldId() {
        return worldId;
    }

    /**
     * Registers a landmark for this world.
     * Enforces world isolation: landmark must belong to this registry's worldId.
     */
    public synchronized void register(WorldLandmark landmark) {
        if (landmark == null) {
            throw new IllegalArgumentException("Cannot register null landmark");
        }
        if (!this.worldId.equals(landmark.getWorldId())) {
            throw new IllegalArgumentException("Cannot register landmark for world '" + landmark.getWorldId() + "' in registry for world '" + this.worldId + "'");
        }
        landmarks.put(landmark.getLandmarkId(), landmark);
    }

    /**
     * Retrieves a landmark by its unique ID.
     */
    public Optional<WorldLandmark> get(String landmarkId) {
        if (landmarkId == null) return Optional.empty();
        return Optional.ofNullable(landmarks.get(landmarkId.trim()));
    }

    /**
     * Returns an unmodifiable snapshot collection of all registered landmarks.
     */
    public List<WorldLandmark> getAll() {
        return List.copyOf(landmarks.values());
    }

    /**
     * Queries all landmarks within a specified radius from given coordinates, sorted by distance.
     */
    public List<WorldLandmark> getNearbyLandmarks(double x, double y, double z, double radius) {
        if (radius <= 0.0) return Collections.emptyList();
        double radiusSq = radius * radius;

        return landmarks.values().stream()
            .filter(lm -> lm.distanceSquaredTo(x, y, z) <= radiusSq)
            .sorted(Comparator.comparingDouble(lm -> lm.distanceSquaredTo(x, y, z)))
            .collect(Collectors.toList());
    }

    /**
     * Finds the closest discovered or visited landmark within maxRadius, if any.
     */
    public Optional<WorldLandmark> getNearestDiscoveredLandmark(double x, double y, double z, double maxRadius) {
        if (maxRadius <= 0.0) return Optional.empty();
        double maxRadiusSq = maxRadius * maxRadius;

        return landmarks.values().stream()
            .filter(WorldLandmark::isDiscovered)
            .filter(lm -> lm.distanceSquaredTo(x, y, z) <= maxRadiusSq)
            .min(Comparator.comparingDouble(lm -> lm.distanceSquaredTo(x, y, z)));
    }

    /**
     * Marks a landmark as discovered, posting a LandmarkDiscoveredEvent if state changed.
     */
    public synchronized boolean markDiscovered(String landmarkId) {
        WorldLandmark existing = landmarks.get(landmarkId);
        if (existing == null) return false;

        if (existing.getDiscoveryState() == DiscoveryState.UNDISCOVERED) {
            WorldLandmark updated = existing.withDiscoveryState(DiscoveryState.DISCOVERED);
            landmarks.put(landmarkId, updated);

            EventBus.getInstance().post(new LandmarkDiscoveredEvent(
                worldId,
                updated.getLandmarkId(),
                updated.getName(),
                updated.getLandmarkType().name(),
                updated.getX(),
                updated.getY(),
                updated.getZ()
            ));
            return true;
        }
        return false;
    }

    /**
     * Marks a landmark as visited.
     */
    public synchronized boolean markVisited(String landmarkId) {
        WorldLandmark existing = landmarks.get(landmarkId);
        if (existing == null) return false;

        if (existing.getDiscoveryState() != DiscoveryState.VISITED) {
            WorldLandmark updated = existing.withDiscoveryState(DiscoveryState.VISITED);
            landmarks.put(landmarkId, updated);
            return true;
        }
        return false;
    }

    /**
     * Checks if the player is within proximity of any undiscovered landmarks and marks them discovered.
     */
    public void checkDiscoveryProximity(double px, double py, double pz, double discoveryRadius) {
        double radiusSq = discoveryRadius * discoveryRadius;
        for (WorldLandmark lm : landmarks.values()) {
            if (lm.getDiscoveryState() == DiscoveryState.UNDISCOVERED && lm.distanceSquaredTo(px, py, pz) <= radiusSq) {
                markDiscovered(lm.getLandmarkId());
            }
        }
    }

    public int size() {
        return landmarks.size();
    }

    public void clear() {
        landmarks.clear();
    }

    // JSON Serialization
    public String toJson() {
        List<WorldLandmark> list = new ArrayList<>(landmarks.values());
        return GSON.toJson(list);
    }

    public synchronized void loadFromJson(String json) {
        if (json == null || json.isBlank()) return;
        try {
            List<WorldLandmark> list = GSON.fromJson(json, LANDMARK_LIST_TYPE);
            if (list != null) {
                for (WorldLandmark lm : list) {
                    if (lm != null && this.worldId.equals(lm.getWorldId())) {
                        landmarks.put(lm.getLandmarkId(), lm);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LandmarkRegistry] Failed to parse landmarks JSON: " + e.getMessage());
        }
    }
}


package com.mcjournal.world.terrain;

/**
 * Reusable, deterministic terrain modifier that creates leveled foundation plateaus
 * with optional smooth blending for authored structures or points of interest.
 * Operates purely in world coordinates and independent of chunk-loading order.
 */
public class PlateauModifier implements TerrainModifier {

    private final String name;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final double targetHeight;
    private final int blendRadius;

    public PlateauModifier(String name, int minX, int maxX, int minZ, int maxZ, double targetHeight) {
        this(name, minX, maxX, minZ, maxZ, targetHeight, 0);
    }

    public PlateauModifier(String name, int minX, int maxX, int minZ, int maxZ, double targetHeight, int blendRadius) {
        this.name = (name != null) ? name : "plateau";
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxZ = Math.max(minZ, maxZ);
        this.targetHeight = targetHeight;
        this.blendRadius = Math.max(0, blendRadius);
    }

    @Override
    public String getName() {
        return name;
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public double getTargetHeight() { return targetHeight; }
    public int getBlendRadius() { return blendRadius; }

    @Override
    public double modifyHeight(int wx, int wz, double currentHeight) {
        // Inside core plateau footprint
        if (wx >= minX && wx <= maxX && wz >= minZ && wz <= maxZ) {
            return targetHeight;
        }

        // Smooth transition margin if blendRadius > 0
        if (blendRadius > 0) {
            int dx = (wx < minX) ? (minX - wx) : (wx > maxX ? (wx - maxX) : 0);
            int dz = (wz < minZ) ? (minZ - wz) : (wz > maxZ ? (wz - maxZ) : 0);
            double dist = Math.hypot(dx, dz);
            if (dist < blendRadius) {
                double t = dist / blendRadius;
                double s = t * t * (3.0 - 2.0 * t); // Hermite smoothstep
                return targetHeight * (1.0 - s) + currentHeight * s;
            }
        }

        return currentHeight;
    }
}

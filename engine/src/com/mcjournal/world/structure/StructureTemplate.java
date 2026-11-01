package com.mcjournal.world.structure;

import com.mcjournal.block.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable 3D block arrangement representing a structure template.
 * Supports discrete rotations (0, 90, 180, 270) and mirroring.
 * Uses a paletted compact voxel buffer to keep memory overhead minimal.
 */
public final class StructureTemplate {

    private final int width;
    private final int height;
    private final int depth;
    private final List<BlockState> palette;
    private final short[] voxelPaletteIndices; // 0 = null (pass-through / preserve existing block)

    private final Map<String, StructureTemplate> transformedCache = new ConcurrentHashMap<>();

    private StructureTemplate(int width, int height, int depth, List<BlockState> palette, short[] voxelPaletteIndices) {
        if (width <= 0 || width > 256) throw new IllegalArgumentException("Width must be between 1 and 256, got: " + width);
        if (height <= 0 || height > 256) throw new IllegalArgumentException("Height must be between 1 and 256, got: " + height);
        if (depth <= 0 || depth > 256) throw new IllegalArgumentException("Depth must be between 1 and 256, got: " + depth);

        this.width = width;
        this.height = height;
        this.depth = depth;
        this.palette = List.copyOf(palette);
        this.voxelPaletteIndices = voxelPaletteIndices;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public int getTotalVolume() { return width * height * depth; }

    private int index(int x, int y, int z) {
        return (y * depth + z) * width + x;
    }

    /**
     * Gets the BlockState at template coordinate (x, y, z).
     * Returns null if the voxel is empty (pass-through).
     */
    public BlockState getBlock(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            return null;
        }
        int paletteIdx = voxelPaletteIndices[index(x, y, z)] & 0xFFFF;
        if (paletteIdx == 0) {
            return null;
        }
        return palette.get(paletteIdx - 1);
    }

    /**
     * Returns an immutable transformed template with the specified rotation and mirror applied.
     * Caches transformed variations for zero runtime allocation on repeated queries.
     */
    public StructureTemplate getTransformed(Rotation rotation, Mirror mirror) {
        Rotation rot = (rotation != null) ? rotation : Rotation.NONE;
        Mirror mir = (mirror != null) ? mirror : Mirror.NONE;

        if (rot == Rotation.NONE && mir == Mirror.NONE) {
            return this;
        }

        String cacheKey = rot.name() + "_" + mir.name();
        return transformedCache.computeIfAbsent(cacheKey, k -> createTransformed(rot, mir));
    }

    private StructureTemplate createTransformed(Rotation rot, Mirror mir) {
        // Compute rotated dimensions
        int rotW = rot.transformedWidth(width, depth);
        int rotD = rot.transformedDepth(width, depth);

        // Mirroring preserves width and depth
        int newW = rotW;
        int newD = rotD;
        int newH = height;

        Builder builder = new Builder(newW, newH, newD);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = getBlock(x, y, z);
                    if (state == null) continue;

                    // 1. Rotate
                    int rx = rot.transformX(x, z, width, depth);
                    int rz = rot.transformZ(x, z, width, depth);
                    BlockState rotatedState = rot.rotateState(state);

                    // 2. Mirror
                    int mx = mir.transformX(rx, newW);
                    int mz = mir.transformZ(rz, newD);

                    builder.setBlock(mx, y, mz, rotatedState);
                }
            }
        }

        return builder.build();
    }

    public static Builder builder(int width, int height, int depth) {
        return new Builder(width, height, depth);
    }

    public static final class Builder {
        private final int width;
        private final int height;
        private final int depth;
        private final short[] indices;
        private final List<BlockState> palette = new ArrayList<>();
        private final Map<BlockState, Short> stateToIndex = new HashMap<>();

        public Builder(int width, int height, int depth) {
            if (width <= 0 || width > 256 || height <= 0 || height > 256 || depth <= 0 || depth > 256) {
                throw new IllegalArgumentException("Template dimensions must be strictly between 1 and 256. Got: " +
                        width + "x" + height + "x" + depth);
            }
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.indices = new short[width * height * depth];
        }

        private short getOrRegisterState(BlockState state) {
            if (state == null) return 0;
            return stateToIndex.computeIfAbsent(state, s -> {
                palette.add(s);
                return (short) palette.size();
            });
        }

        public Builder setBlock(int x, int y, int z, BlockState state) {
            if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
                throw new IndexOutOfBoundsException("Template coordinate out of bounds: (" + x + "," + y + "," + z + ") for size " +
                        width + "x" + height + "x" + depth);
            }
            short idx = getOrRegisterState(state);
            indices[(y * depth + z) * width + x] = idx;
            return this;
        }

        public Builder fill(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
            int x0 = Math.max(0, Math.min(minX, maxX));
            int x1 = Math.min(width - 1, Math.max(minX, maxX));
            int y0 = Math.max(0, Math.min(minY, maxY));
            int y1 = Math.min(height - 1, Math.max(minY, maxY));
            int z0 = Math.max(0, Math.min(minZ, maxZ));
            int z1 = Math.min(depth - 1, Math.max(minZ, maxZ));

            short idx = getOrRegisterState(state);
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        indices[(y * depth + z) * width + x] = idx;
                    }
                }
            }
            return this;
        }

        public Builder fillHollowBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                     BlockState wallState, BlockState interiorState) {
            int x0 = Math.max(0, Math.min(minX, maxX));
            int x1 = Math.min(width - 1, Math.max(minX, maxX));
            int y0 = Math.max(0, Math.min(minY, maxY));
            int y1 = Math.min(height - 1, Math.max(minY, maxY));
            int z0 = Math.max(0, Math.min(minZ, maxZ));
            int z1 = Math.min(depth - 1, Math.max(minZ, maxZ));

            short wallIdx = getOrRegisterState(wallState);
            short interiorIdx = getOrRegisterState(interiorState);

            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        boolean isEdge = (x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1);
                        indices[(y * depth + z) * width + x] = isEdge ? wallIdx : interiorIdx;
                    }
                }
            }
            return this;
        }

        public StructureTemplate build() {
            return new StructureTemplate(width, height, depth, palette, indices);
        }
    }
}


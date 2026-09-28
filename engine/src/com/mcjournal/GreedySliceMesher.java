package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

/**
 * 2D Slice-based Greedy Mesher with per-corner Ambient Occlusion compatibility.
 * Emits indexed geometry (4 unique vertices per quad and 6 indices) with interleaved
 * vertex attributes (stride 13 floats = 52 bytes).
 */
public class GreedySliceMesher {

    private static final float[] AO_CURVE = new float[]{1.0f, 0.78f, 0.58f, 0.42f};
    private static final int GRID_SIZE = 8; // 8x8 texture atlas

    // Reusable slice buffer: max slice size is 16 * 256 = 4096 entries
    private final long[] mask = new long[Chunk.SIZE * Chunk.HEIGHT];

    public static class FaceDef {
        public final int[] dir;
        public final float[] norm;
        public final float baseShade;
        public final int[] uAxis;
        public final int[] vAxis;
        public final int[][] cornerOffsets;

        FaceDef(int[] dir, float[] norm, float baseShade, int[] uAxis, int[] vAxis, int[][] cornerOffsets) {
            this.dir = dir;
            this.norm = norm;
            this.baseShade = baseShade;
            this.uAxis = uAxis;
            this.vAxis = vAxis;
            this.cornerOffsets = cornerOffsets;
        }
    }

    public static final FaceDef[] FACES = new FaceDef[] {
        // 0: Right (+X)
        new FaceDef(
            new int[]{1, 0, 0}, new float[]{1, 0, 0}, 0.75f,
            new int[]{0, 0, 1}, new int[]{0, 1, 0},
            new int[][]{{1, -1}, {-1, -1}, {-1, 1}, {1, 1}}
        ),
        // 1: Left (-X)
        new FaceDef(
            new int[]{-1, 0, 0}, new float[]{-1, 0, 0}, 0.75f,
            new int[]{0, 0, -1}, new int[]{0, 1, 0},
            new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}
        ),
        // 2: Top (+Y)
        new FaceDef(
            new int[]{0, 1, 0}, new float[]{0, 1, 0}, 1.0f,
            new int[]{1, 0, 0}, new int[]{0, 0, 1},
            new int[][]{{-1, 1}, {1, 1}, {1, -1}, {-1, -1}}
        ),
        // 3: Bottom (-Y)
        new FaceDef(
            new int[]{0, -1, 0}, new float[]{0, -1, 0}, 0.55f,
            new int[]{1, 0, 0}, new int[]{0, 0, -1},
            new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}
        ),
        // 4: Front (+Z)
        new FaceDef(
            new int[]{0, 0, 1}, new float[]{0, 0, 1}, 0.85f,
            new int[]{-1, 0, 0}, new int[]{0, 1, 0},
            new int[][]{{1, -1}, {-1, -1}, {-1, 1}, {1, 1}}
        ),
        // 5: Back (-Z)
        new FaceDef(
            new int[]{0, 0, -1}, new float[]{0, 0, -1}, 0.85f,
            new int[]{1, 0, 0}, new int[]{0, 1, 0},
            new int[][]{{1, -1}, {-1, -1}, {-1, 1}, {1, 1}}
        )
    };

    public static float[] getTileBounds(int slotIndex) {
        int col = slotIndex % GRID_SIZE;
        int row = slotIndex / GRID_SIZE;

        float totalWidth = GRID_SIZE * 64.0f;
        float eps = 0.5f / totalWidth;

        float uMin = (float) col / GRID_SIZE + eps;
        float uMax = (float) (col + 1) / GRID_SIZE - eps;
        float vMin = 1.0f - (float) (row + 1) / GRID_SIZE + eps;
        float vMax = 1.0f - (float) row / GRID_SIZE - eps;

        return new float[]{uMin, uMax, vMin, vMax};
    }

    public static int computeCornerOcclusion(ChunkNeighborhood neighborhood, int lx, int y, int lz, FaceDef face, int cornerIdx) {
        int uSign = face.cornerOffsets[cornerIdx][0];
        int vSign = face.cornerOffsets[cornerIdx][1];

        int fx = lx + face.dir[0];
        int fy = y + face.dir[1];
        int fz = lz + face.dir[2];

        int u1 = face.uAxis[0] * uSign;
        int u2 = face.uAxis[1] * uSign;
        int u3 = face.uAxis[2] * uSign;

        int v1 = face.vAxis[0] * vSign;
        int v2 = face.vAxis[1] * vSign;
        int v3 = face.vAxis[2] * vSign;

        boolean s1 = neighborhood.isAOSolid(fx + u1, fy + u2, fz + u3);
        boolean s2 = neighborhood.isAOSolid(fx + v1, fy + v2, fz + v3);
        boolean corner = neighborhood.isAOSolid(fx + u1 + v1, fy + u2 + v2, fz + u3 + v3);

        int occlusion = 0;
        if (s1) occlusion++;
        if (s2) occlusion++;
        if (s1 && s2) {
            occlusion++;
        } else if (corner) {
            occlusion++;
        }
        return Math.min(3, occlusion);
    }

    public static long packKey(int stateId, int rot, int ao0, int ao1, int ao2, int ao3) {
        return (stateId & 0xFFFFL)
            | ((long) (rot & 3) << 16)
            | ((long) (ao0 & 3) << 18)
            | ((long) (ao1 & 3) << 20)
            | ((long) (ao2 & 3) << 22)
            | ((long) (ao3 & 3) << 24);
    }

    public static int unpackStateId(long key) {
        return (int) (key & 0xFFFFL);
    }

    public static int unpackRot(long key) {
        return (int) ((key >> 16) & 3);
    }

    public static int unpackAO(long key, int cornerIdx) {
        return (int) ((key >> (18 + cornerIdx * 2)) & 3);
    }

    /**
     * Meshes all solid opaque faces within the chunk into interleaved vertices and indexed triangles.
     */
    public void meshSolid(
        ChunkNeighborhood neighborhood,
        ChunkMeshBuilder.FloatArrayList vertexList,
        ChunkMeshBuilder.IntArrayList indexList,
        ChunkMeshBuilder.FloatArrayList legacyPosList,
        ChunkMeshBuilder.FloatArrayList legacyUvList,
        ChunkMeshBuilder.FloatArrayList legacyNormList,
        ChunkMeshBuilder.FloatArrayList legacyColList
    ) {
        int cx = neighborhood.getCenterCx();
        int cz = neighborhood.getCenterCz();
        int worldOriginX = cx * Chunk.SIZE;
        int worldOriginZ = cz * Chunk.SIZE;

        // 1. TOP (+Y, Face 2) & BOTTOM (-Y, Face 3) SLICES
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            meshYSlice(neighborhood, y, 2, worldOriginX, worldOriginZ, cx, cz, vertexList, indexList, legacyPosList, legacyUvList, legacyNormList, legacyColList);
            meshYSlice(neighborhood, y, 3, worldOriginX, worldOriginZ, cx, cz, vertexList, indexList, legacyPosList, legacyUvList, legacyNormList, legacyColList);
        }

        // 2. RIGHT (+X, Face 0) & LEFT (-X, Face 1) SLICES
        for (int x = 0; x < Chunk.SIZE; x++) {
            meshXSlice(neighborhood, x, 0, worldOriginX, worldOriginZ, cx, cz, vertexList, indexList, legacyPosList, legacyUvList, legacyNormList, legacyColList);
            meshXSlice(neighborhood, x, 1, worldOriginX, worldOriginZ, cx, cz, vertexList, indexList, legacyPosList, legacyUvList, legacyNormList, legacyColList);
        }

        // 3. FRONT (+Z, Face 4) & BACK (-Z, Face 5) SLICES
        for (int z = 0; z < Chunk.SIZE; z++) {
            meshZSlice(neighborhood, z, 4, worldOriginX, worldOriginZ, cx, cz, vertexList, indexList, legacyPosList, legacyUvList, legacyNormList, legacyColList);
            meshZSlice(neighborhood, z, 5, worldOriginX, worldOriginZ, cx, cz, vertexList, indexList, legacyPosList, legacyUvList, legacyNormList, legacyColList);
        }
    }

    private void meshYSlice(
        ChunkNeighborhood neighborhood, int y, int faceIdx,
        int worldOriginX, int worldOriginZ, int cx, int cz,
        ChunkMeshBuilder.FloatArrayList vertexList,
        ChunkMeshBuilder.IntArrayList indexList,
        ChunkMeshBuilder.FloatArrayList legacyPosList,
        ChunkMeshBuilder.FloatArrayList legacyUvList,
        ChunkMeshBuilder.FloatArrayList legacyNormList,
        ChunkMeshBuilder.FloatArrayList legacyColList
    ) {
        FaceDef face = FACES[faceIdx];
        int dy = face.dir[1];

        // Fill 16x16 mask
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int idx = z * 16 + x;
                int stateId = neighborhood.getStateId(x, y, z);
                if (stateId == 0) {
                    mask[idx] = 0;
                    continue;
                }
                BlockState block = BlockStateRegistry.getStateById(stateId);
                // Exclude plants, leaves, and water from solid mesh pass
                if (block.isPlant() || block.is(Blocks.WATER) || block.is(Blocks.OAK_LEAVES) || block.is(Blocks.BIRCH_LEAVES)) {
                    mask[idx] = 0;
                    continue;
                }

                // Transparency check of neighbor block
                int ny = y + dy;
                if (!neighborhood.isTransparent(x, ny, z)) {
                    mask[idx] = 0;
                    continue;
                }

                int ao0 = computeCornerOcclusion(neighborhood, x, y, z, face, 0);
                int ao1 = computeCornerOcclusion(neighborhood, x, y, z, face, 1);
                int ao2 = computeCornerOcclusion(neighborhood, x, y, z, face, 2);
                int ao3 = computeCornerOcclusion(neighborhood, x, y, z, face, 3);

                mask[idx] = packKey(stateId, 0, ao0, ao1, ao2, ao3);
            }
        }

        // Greedy 2D quad merge on 16x16 grid
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                long key = mask[z * 16 + x];
                if (key == 0) continue;

                int w = 1;
                while (x + w < 16 && mask[z * 16 + x + w] == key) {
                    w++;
                }

                int h = 1;
                boolean canExtend = true;
                while (z + h < 16 && canExtend) {
                    for (int k = 0; k < w; k++) {
                        if (mask[(z + h) * 16 + x + k] != key) {
                            canExtend = false;
                            break;
                        }
                    }
                    if (canExtend) h++;
                }

                emitQuad(
                    worldOriginX + x, y, worldOriginZ + z,
                    w, h, faceIdx, key,
                    vertexList, indexList,
                    legacyPosList, legacyUvList, legacyNormList, legacyColList
                );

                for (int dz = 0; dz < h; dz++) {
                    for (int dx = 0; dx < w; dx++) {
                        mask[(z + dz) * 16 + x + dx] = 0;
                    }
                }
            }
        }
    }

    private void meshXSlice(
        ChunkNeighborhood neighborhood, int x, int faceIdx,
        int worldOriginX, int worldOriginZ, int cx, int cz,
        ChunkMeshBuilder.FloatArrayList vertexList,
        ChunkMeshBuilder.IntArrayList indexList,
        ChunkMeshBuilder.FloatArrayList legacyPosList,
        ChunkMeshBuilder.FloatArrayList legacyUvList,
        ChunkMeshBuilder.FloatArrayList legacyNormList,
        ChunkMeshBuilder.FloatArrayList legacyColList
    ) {
        FaceDef face = FACES[faceIdx];
        int dx = face.dir[0];

        // 2D grid in (z, y): width is z in [0..15], height is y in [0..255]
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                int idx = y * 16 + z;
                int stateId = neighborhood.getStateId(x, y, z);
                if (stateId == 0) {
                    mask[idx] = 0;
                    continue;
                }
                BlockState block = BlockStateRegistry.getStateById(stateId);
                if (block.isPlant() || block.is(Blocks.WATER) || block.is(Blocks.OAK_LEAVES) || block.is(Blocks.BIRCH_LEAVES)) {
                    mask[idx] = 0;
                    continue;
                }

                int nx = x + dx;
                if (!neighborhood.isTransparent(nx, y, z)) {
                    mask[idx] = 0;
                    continue;
                }

                int ao0 = computeCornerOcclusion(neighborhood, x, y, z, face, 0);
                int ao1 = computeCornerOcclusion(neighborhood, x, y, z, face, 1);
                int ao2 = computeCornerOcclusion(neighborhood, x, y, z, face, 2);
                int ao3 = computeCornerOcclusion(neighborhood, x, y, z, face, 3);

                mask[idx] = packKey(stateId, 0, ao0, ao1, ao2, ao3);
            }
        }

        // Greedy 2D quad merge on 16x256 grid
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                long key = mask[y * 16 + z];
                if (key == 0) continue;

                int w = 1;
                while (z + w < 16 && mask[y * 16 + z + w] == key) {
                    w++;
                }

                int h = 1;
                boolean canExtend = true;
                while (y + h < Chunk.HEIGHT && canExtend) {
                    for (int k = 0; k < w; k++) {
                        if (mask[(y + h) * 16 + z + k] != key) {
                            canExtend = false;
                            break;
                        }
                    }
                    if (canExtend) h++;
                }

                emitQuad(
                    worldOriginX + x, y, worldOriginZ + z,
                    w, h, faceIdx, key,
                    vertexList, indexList,
                    legacyPosList, legacyUvList, legacyNormList, legacyColList
                );

                for (int dy = 0; dy < h; dy++) {
                    for (int dz = 0; dz < w; dz++) {
                        mask[(y + dy) * 16 + z + dz] = 0;
                    }
                }
            }
        }
    }

    private void meshZSlice(
        ChunkNeighborhood neighborhood, int z, int faceIdx,
        int worldOriginX, int worldOriginZ, int cx, int cz,
        ChunkMeshBuilder.FloatArrayList vertexList,
        ChunkMeshBuilder.IntArrayList indexList,
        ChunkMeshBuilder.FloatArrayList legacyPosList,
        ChunkMeshBuilder.FloatArrayList legacyUvList,
        ChunkMeshBuilder.FloatArrayList legacyNormList,
        ChunkMeshBuilder.FloatArrayList legacyColList
    ) {
        FaceDef face = FACES[faceIdx];
        int dz = face.dir[2];

        // 2D grid in (x, y): width is x in [0..15], height is y in [0..255]
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int x = 0; x < 16; x++) {
                int idx = y * 16 + x;
                int stateId = neighborhood.getStateId(x, y, z);
                if (stateId == 0) {
                    mask[idx] = 0;
                    continue;
                }
                BlockState block = BlockStateRegistry.getStateById(stateId);
                if (block.isPlant() || block.is(Blocks.WATER) || block.is(Blocks.OAK_LEAVES) || block.is(Blocks.BIRCH_LEAVES)) {
                    mask[idx] = 0;
                    continue;
                }

                int nz = z + dz;
                if (!neighborhood.isTransparent(x, y, nz)) {
                    mask[idx] = 0;
                    continue;
                }

                int ao0 = computeCornerOcclusion(neighborhood, x, y, z, face, 0);
                int ao1 = computeCornerOcclusion(neighborhood, x, y, z, face, 1);
                int ao2 = computeCornerOcclusion(neighborhood, x, y, z, face, 2);
                int ao3 = computeCornerOcclusion(neighborhood, x, y, z, face, 3);

                mask[idx] = packKey(stateId, 0, ao0, ao1, ao2, ao3);
            }
        }

        // Greedy 2D quad merge on 16x256 grid
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int x = 0; x < 16; x++) {
                long key = mask[y * 16 + x];
                if (key == 0) continue;

                int w = 1;
                while (x + w < 16 && mask[y * 16 + x + w] == key) {
                    w++;
                }

                int h = 1;
                boolean canExtend = true;
                while (y + h < Chunk.HEIGHT && canExtend) {
                    for (int k = 0; k < w; k++) {
                        if (mask[(y + h) * 16 + x + k] != key) {
                            canExtend = false;
                            break;
                        }
                    }
                    if (canExtend) h++;
                }

                emitQuad(
                    worldOriginX + x, y, worldOriginZ + z,
                    w, h, faceIdx, key,
                    vertexList, indexList,
                    legacyPosList, legacyUvList, legacyNormList, legacyColList
                );

                for (int dy = 0; dy < h; dy++) {
                    for (int dx = 0; dx < w; dx++) {
                        mask[(y + dy) * 16 + x + dx] = 0;
                    }
                }
            }
        }
    }

    private void emitQuad(
        float wx, float y, float wz,
        int w, int h, int faceIdx, long key,
        ChunkMeshBuilder.FloatArrayList vertexList,
        ChunkMeshBuilder.IntArrayList indexList,
        ChunkMeshBuilder.FloatArrayList legacyPosList,
        ChunkMeshBuilder.FloatArrayList legacyUvList,
        ChunkMeshBuilder.FloatArrayList legacyNormList,
        ChunkMeshBuilder.FloatArrayList legacyColList
    ) {
        int stateId = unpackStateId(key);
        float s0 = AO_CURVE[unpackAO(key, 0)];
        float s1 = AO_CURVE[unpackAO(key, 1)];
        float s2 = AO_CURVE[unpackAO(key, 2)];
        float s3 = AO_CURVE[unpackAO(key, 3)];

        BlockState block = BlockStateRegistry.getStateById(stateId);
        int slot = block.getFaceTextureSlot(faceIdx);
        float[] tileBounds = getTileBounds(slot);
        float uTileMin = tileBounds[0];
        float vTileMin = tileBounds[2];

        // Corner 3D positions
        float x0, y0, z0;
        float x1, y1, z1;
        float x2, y2, z2;
        float x3, y3, z3;

        FaceDef face = FACES[faceIdx];
        float nx = face.norm[0], ny = face.norm[1], nz = face.norm[2];

        switch (faceIdx) {
            case 2: // Top (+Y)
                x0 = wx;     y0 = y + 1; z0 = wz + h;
                x1 = wx + w; y1 = y + 1; z1 = wz + h;
                x2 = wx + w; y2 = y + 1; z2 = wz;
                x3 = wx;     y3 = y + 1; z3 = wz;
                break;
            case 3: // Bottom (-Y)
                x0 = wx;     y0 = y;     z0 = wz;
                x1 = wx + w; y1 = y;     z1 = wz;
                x2 = wx + w; y2 = y;     z2 = wz + h;
                x3 = wx;     y3 = y;     z3 = wz + h;
                break;
            case 0: // Right (+X)
                x0 = wx + 1; y0 = y;     z0 = wz + w;
                x1 = wx + 1; y1 = y;     z1 = wz;
                x2 = wx + 1; y2 = y + h; z2 = wz;
                x3 = wx + 1; y3 = y + h; z3 = wz + w;
                break;
            case 1: // Left (-X)
                x0 = wx;     y0 = y;     z0 = wz;
                x1 = wx;     y1 = y;     z1 = wz + w;
                x2 = wx;     y2 = y + h; z2 = wz + w;
                x3 = wx;     y3 = y + h; z3 = wz;
                break;
            case 4: // Front (+Z)
                x0 = wx;     y0 = y;     z0 = wz + 1;
                x1 = wx + w; y1 = y;     z1 = wz + 1;
                x2 = wx + w; y2 = y + h; z2 = wz + 1;
                x3 = wx;     y3 = y + h; z3 = wz + 1;
                break;
            case 5: // Back (-Z)
            default:
                x0 = wx + w; y0 = y;     z0 = wz;
                x1 = wx;     y1 = y;     z1 = wz;
                x2 = wx;     y2 = y + h; z2 = wz;
                x3 = wx + w; y3 = y + h; z3 = wz;
                break;
        }

        // Local UV coordinates (0..w, 0..h)
        float u0 = 0, v0 = 0;
        float u1 = w, v1 = 0;
        float u2 = w, v2 = h;
        float u3 = 0, v3 = h;

        // 1. Emit 4 unique vertices into interleaved vertex buffer (52 bytes / vertex)
        int baseIndex = vertexList.size() / 13;

        // v0
        vertexList.add13(x0, y0, z0, u0, v0, uTileMin, vTileMin, s0, s0, s0, nx, ny, nz);
        // v1
        vertexList.add13(x1, y1, z1, u1, v1, uTileMin, vTileMin, s1, s1, s1, nx, ny, nz);
        // v2
        vertexList.add13(x2, y2, z2, u2, v2, uTileMin, vTileMin, s2, s2, s2, nx, ny, nz);
        // v3
        vertexList.add13(x3, y3, z3, u3, v3, uTileMin, vTileMin, s3, s3, s3, nx, ny, nz);

        // 2. Emit 6 indices for indexed triangle drawing with diagonal flip
        if (s0 + s2 > s1 + s3) {
            indexList.add6(baseIndex, baseIndex + 1, baseIndex + 2, baseIndex, baseIndex + 2, baseIndex + 3);
        } else {
            indexList.add6(baseIndex + 1, baseIndex + 2, baseIndex + 3, baseIndex + 1, baseIndex + 3, baseIndex);
        }

        // 3. Optional backward compatibility for unindexed legacy arrays
        if (legacyPosList != null) {
            if (s0 + s2 > s1 + s3) {
                legacyPosList.add9(x0, y0, z0, x1, y1, z1, x2, y2, z2);
                legacyNormList.add9(nx, ny, nz, nx, ny, nz, nx, ny, nz);
                legacyUvList.add12(u0, v0, uTileMin, vTileMin, u1, v1, uTileMin, vTileMin, u2, v2, uTileMin, vTileMin);
                legacyColList.add9(s0, s0, s0, s1, s1, s1, s2, s2, s2);

                legacyPosList.add9(x0, y0, z0, x2, y2, z2, x3, y3, z3);
                legacyNormList.add9(nx, ny, nz, nx, ny, nz, nx, ny, nz);
                legacyUvList.add12(u0, v0, uTileMin, vTileMin, u2, v2, uTileMin, vTileMin, u3, v3, uTileMin, vTileMin);
                legacyColList.add9(s0, s0, s0, s2, s2, s2, s3, s3, s3);
            } else {
                legacyPosList.add9(x1, y1, z1, x2, y2, z2, x3, y3, z3);
                legacyNormList.add9(nx, ny, nz, nx, ny, nz, nx, ny, nz);
                legacyUvList.add12(u1, v1, uTileMin, vTileMin, u2, v2, uTileMin, vTileMin, u3, v3, uTileMin, vTileMin);
                legacyColList.add9(s1, s1, s1, s2, s2, s2, s3, s3, s3);

                legacyPosList.add9(x1, y1, z1, x3, y3, z3, x0, y0, z0);
                legacyNormList.add9(nx, ny, nz, nx, ny, nz, nx, ny, nz);
                legacyUvList.add12(u1, v1, uTileMin, vTileMin, u3, v3, uTileMin, vTileMin, u0, v0, uTileMin, vTileMin);
                legacyColList.add9(s1, s1, s1, s3, s3, s3, s0, s0, s0);
            }
        }
    }
}

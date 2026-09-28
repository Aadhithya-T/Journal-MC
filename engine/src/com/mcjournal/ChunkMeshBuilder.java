package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

/**
 * Builds chunk meshes separated into distinct channels with indexed interleaved geometry:
 * - SolidMesh: Opaque voxels with 2D greedy meshing and ambient occlusion
 * - CutoutMesh: Alpha-tested cross foliage and leaves
 * - WaterMesh: Translucent fluids with Beer-Lambert depth and shoreline optics
 *
 * Each channel produces an interleaved float array (13 floats per vertex) and an index array (int[]).
 */
public class ChunkMeshBuilder {

    private static final ThreadLocal<GreedySliceMesher> SLICE_MESHER =
        ThreadLocal.withInitial(GreedySliceMesher::new);

    public static class MeshData {
        // Interleaved indexed geometry (P4.1 & P4.2)
        public final float[] solidVertices;
        public final int[] solidIndices;

        public final float[] cutoutVertices;
        public final int[] cutoutIndices;

        public final float[] waterVertices;
        public final int[] waterIndices;

        // Legacy unindexed arrays (100% backward compatibility)
        public final float[] solidPositions;
        public final float[] solidNormals;
        public final float[] solidUvs;
        public final float[] solidColors;

        public final float[] cutoutPositions;
        public final float[] cutoutNormals;
        public final float[] cutoutUvs;
        public final float[] cutoutColors;

        public final float[] waterPositions;
        public final float[] waterNormals;
        public final float[] waterUvs;
        public final float[] waterColors;

        public MeshData(
            float[] solidVertices, int[] solidIndices,
            float[] cutoutVertices, int[] cutoutIndices,
            float[] waterVertices, int[] waterIndices,
            float[] solidPositions, float[] solidNormals, float[] solidUvs, float[] solidColors,
            float[] cutoutPositions, float[] cutoutNormals, float[] cutoutUvs, float[] cutoutColors,
            float[] waterPositions, float[] waterNormals, float[] waterUvs, float[] waterColors
        ) {
            this.solidVertices = solidVertices != null ? solidVertices : new float[0];
            this.solidIndices = solidIndices != null ? solidIndices : new int[0];
            this.cutoutVertices = cutoutVertices != null ? cutoutVertices : new float[0];
            this.cutoutIndices = cutoutIndices != null ? cutoutIndices : new int[0];
            this.waterVertices = waterVertices != null ? waterVertices : new float[0];
            this.waterIndices = waterIndices != null ? waterIndices : new int[0];

            this.solidPositions = solidPositions != null ? solidPositions : new float[0];
            this.solidNormals = solidNormals != null ? solidNormals : new float[0];
            this.solidUvs = solidUvs != null ? solidUvs : new float[0];
            this.solidColors = solidColors != null ? solidColors : new float[0];

            this.cutoutPositions = cutoutPositions != null ? cutoutPositions : new float[0];
            this.cutoutNormals = cutoutNormals != null ? cutoutNormals : new float[0];
            this.cutoutUvs = cutoutUvs != null ? cutoutUvs : new float[0];
            this.cutoutColors = cutoutColors != null ? cutoutColors : new float[0];

            this.waterPositions = waterPositions != null ? waterPositions : new float[0];
            this.waterNormals = waterNormals != null ? waterNormals : new float[0];
            this.waterUvs = waterUvs != null ? waterUvs : new float[0];
            this.waterColors = waterColors != null ? waterColors : new float[0];
        }

        public int getTotalBytes() {
            int vertexBytes = (solidVertices.length + cutoutVertices.length + waterVertices.length) * Float.BYTES;
            int indexBytes = (solidIndices.length + cutoutIndices.length + waterIndices.length) * Integer.BYTES;
            return vertexBytes + indexBytes;
        }

        public boolean isEmpty() {
            return solidIndices.length == 0 && cutoutIndices.length == 0 && waterIndices.length == 0;
        }
    }

    private static final int[][][] CUBE_CORNERS = new int[][][] {
        // 0: Right (+X)
        {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}},
        // 1: Left (-X)
        {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
        // 2: Top (+Y)
        {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}},
        // 3: Bottom (-Y)
        {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
        // 4: Front (+Z)
        {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}},
        // 5: Back (-Z)
        {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}}
    };

    public static MeshData buildMesh(Chunk chunk, ChunkManager manager) {
        return buildMesh(ChunkNeighborhood.of(chunk, manager));
    }

    public static MeshData buildMesh(ChunkNeighborhood neighborhood) {
        // Interleaved lists (stride 13: pos(3), uv(4), col(3), norm(3))
        FloatArrayList solidVerts = new FloatArrayList(16384);
        IntArrayList solidIndices = new IntArrayList(8192);

        FloatArrayList cutoutVerts = new FloatArrayList(4096);
        IntArrayList cutoutIndices = new IntArrayList(2048);

        FloatArrayList waterVerts = new FloatArrayList(4096);
        IntArrayList waterIndices = new IntArrayList(2048);

        // Legacy compatibility lists
        FloatArrayList solidPos = new FloatArrayList(16384);
        FloatArrayList solidNorm = new FloatArrayList(16384);
        FloatArrayList solidUv = new FloatArrayList(21845);
        FloatArrayList solidCol = new FloatArrayList(16384);

        FloatArrayList cutoutPos = new FloatArrayList(4096);
        FloatArrayList cutoutNorm = new FloatArrayList(4096);
        FloatArrayList cutoutUv = new FloatArrayList(5461);
        FloatArrayList cutoutCol = new FloatArrayList(4096);

        FloatArrayList waterPos = new FloatArrayList(4096);
        FloatArrayList waterNorm = new FloatArrayList(4096);
        FloatArrayList waterUv = new FloatArrayList(5461);
        FloatArrayList waterCol = new FloatArrayList(4096);

        // 1. OPAQUE SOLID PASS — Full 2D Greedy Meshing with Ambient Occlusion & Indexed Geometry
        GreedySliceMesher mesher = SLICE_MESHER.get();
        mesher.meshSolid(neighborhood, solidVerts, solidIndices, solidPos, solidUv, solidNorm, solidCol);

        // 2. CUTOUT & FLUID PASS — Foliage Cross-Quads, Leaves & Water Surfaces
        int cx = neighborhood.getCenterCx();
        int cz = neighborhood.getCenterCz();
        int worldOriginX = cx * Chunk.SIZE;
        int worldOriginZ = cz * Chunk.SIZE;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int stateId = neighborhood.getStateId(x, y, z);
                    if (stateId == 0) continue;

                    BlockState block = BlockStateRegistry.getStateById(stateId);
                    int wx = worldOriginX + x;
                    int wz = worldOriginZ + z;

                    // A. Cross-Foliage (Tall Grass, Poppy, Dandelion)
                    if (block.isPlant()) {
                        buildCrossFoliage(neighborhood, x, y, z, wx, wz, block,
                            cutoutVerts, cutoutIndices, cutoutPos, cutoutNorm, cutoutUv, cutoutCol);
                        continue;
                    }

                    // B. Cutout Leaves (Oak Leaves, Birch Leaves)
                    if (block.is(Blocks.OAK_LEAVES) || block.is(Blocks.BIRCH_LEAVES)) {
                        buildLeafBlock(neighborhood, x, y, z, wx, wz, block,
                            cutoutVerts, cutoutIndices, cutoutPos, cutoutNorm, cutoutUv, cutoutCol);
                        continue;
                    }

                    // C. Water Surface
                    if (block.is(Blocks.WATER)) {
                        buildWaterBlock(neighborhood, x, y, z, wx, wz, block,
                            waterVerts, waterIndices, waterPos, waterNorm, waterUv, waterCol);
                    }
                }
            }
        }

        return new MeshData(
            solidVerts.toArray(), solidIndices.toArray(),
            cutoutVerts.toArray(), cutoutIndices.toArray(),
            waterVerts.toArray(), waterIndices.toArray(),
            solidPos.toArray(), solidNorm.toArray(), solidUv.toArray(), solidCol.toArray(),
            cutoutPos.toArray(), cutoutNorm.toArray(), cutoutUv.toArray(), cutoutCol.toArray(),
            waterPos.toArray(), waterNorm.toArray(), waterUv.toArray(), waterCol.toArray()
        );
    }

    private static void buildLeafBlock(
        ChunkNeighborhood neighborhood, int x, int y, int z, int wx, int wz, BlockState block,
        FloatArrayList verts, IntArrayList indices,
        FloatArrayList pos, FloatArrayList norm, FloatArrayList uv, FloatArrayList col
    ) {
        int slot = block.getFaceTextureSlot(0);
        float[] tileBounds = GreedySliceMesher.getTileBounds(slot);
        float uTileMin = tileBounds[0];
        float vTileMin = tileBounds[2];

        for (int f = 0; f < 6; f++) {
            GreedySliceMesher.FaceDef face = GreedySliceMesher.FACES[f];
            int nx = x + face.dir[0];
            int ny = y + face.dir[1];
            int nz = z + face.dir[2];

            int neighborId = neighborhood.getStateId(nx, ny, nz);
            BlockState neighbor = BlockStateRegistry.getStateById(neighborId);

            if (!neighbor.is(block.getBlock()) && neighborhood.isTransparent(nx, ny, nz)) {
                int[][] c = CUBE_CORNERS[f];
                float x0 = wx + c[0][0], y0 = y + c[0][1], z0 = wz + c[0][2];
                float x1 = wx + c[1][0], y1 = y + c[1][1], z1 = wz + c[1][2];
                float x2 = wx + c[2][0], y2 = y + c[2][1], z2 = wz + c[2][2];
                float x3 = wx + c[3][0], y3 = y + c[3][1], z3 = wz + c[3][2];

                float fnx = face.norm[0], fny = face.norm[1], fnz = face.norm[2];

                // 1. Indexed interleaved vertices (4 vertices, 6 indices)
                int base = verts.size() / 13;
                verts.add13(x0, y0, z0, 0, 0, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, fnx, fny, fnz);
                verts.add13(x1, y1, z1, 1, 0, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, fnx, fny, fnz);
                verts.add13(x2, y2, z2, 1, 1, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, fnx, fny, fnz);
                verts.add13(x3, y3, z3, 0, 1, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, fnx, fny, fnz);

                indices.add6(base, base + 1, base + 2, base, base + 2, base + 3);

                // 2. Legacy arrays
                if (pos != null) {
                    pos.add9(x0, y0, z0, x1, y1, z1, x2, y2, z2);
                    norm.add9(fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz);
                    uv.add12(0, 0, uTileMin, vTileMin, 1, 0, uTileMin, vTileMin, 1, 1, uTileMin, vTileMin);
                    col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);

                    pos.add9(x0, y0, z0, x2, y2, z2, x3, y3, z3);
                    norm.add9(fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz);
                    uv.add12(0, 0, uTileMin, vTileMin, 1, 1, uTileMin, vTileMin, 0, 1, uTileMin, vTileMin);
                    col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
        }
    }

    private static void buildWaterBlock(
        ChunkNeighborhood neighborhood, int x, int y, int z, int wx, int wz, BlockState block,
        FloatArrayList verts, IntArrayList indices,
        FloatArrayList pos, FloatArrayList norm, FloatArrayList uv, FloatArrayList col
    ) {
        for (int f = 0; f < 6; f++) {
            GreedySliceMesher.FaceDef face = GreedySliceMesher.FACES[f];
            int nx = x + face.dir[0];
            int ny = y + face.dir[1];
            int nz = z + face.dir[2];

            int neighborId = neighborhood.getStateId(nx, ny, nz);
            BlockState neighbor = BlockStateRegistry.getStateById(neighborId);

            if (!neighbor.is(Blocks.WATER) && neighborhood.isTransparent(nx, ny, nz)) {
                int[][] c = CUBE_CORNERS[f];
                float yOffset = -0.1f; // Slight Y offset for water surface

                float x0 = wx + c[0][0], y0 = y + c[0][1] + yOffset, z0 = wz + c[0][2];
                float x1 = wx + c[1][0], y1 = y + c[1][1] + yOffset, z1 = wz + c[1][2];
                float x2 = wx + c[2][0], y2 = y + c[2][1] + yOffset, z2 = wz + c[2][2];
                float x3 = wx + c[3][0], y3 = y + c[3][1] + yOffset, z3 = wz + c[3][2];

                float s0 = neighborhood.computeWaterColumnDepth(x + c[0][0], y, z + c[0][2]) / 8.0f;
                float s1 = neighborhood.computeWaterColumnDepth(x + c[1][0], y, z + c[1][2]) / 8.0f;
                float s2 = neighborhood.computeWaterColumnDepth(x + c[2][0], y, z + c[2][2]) / 8.0f;
                float s3 = neighborhood.computeWaterColumnDepth(x + c[3][0], y, z + c[3][2]) / 8.0f;

                float g0 = neighborhood.computeWaterShoreline(x + c[0][0], y, z + c[0][2]);
                float g1 = neighborhood.computeWaterShoreline(x + c[1][0], y, z + c[1][2]);
                float g2 = neighborhood.computeWaterShoreline(x + c[2][0], y, z + c[2][2]);
                float g3 = neighborhood.computeWaterShoreline(x + c[3][0], y, z + c[3][2]);

                float fnx = face.norm[0], fny = face.norm[1], fnz = face.norm[2];

                // 1. Indexed interleaved vertices (4 vertices, 6 indices)
                int base = verts.size() / 13;
                verts.add13(x0, y0, z0, 0, 0, 0, 0, s0, g0, 0, fnx, fny, fnz);
                verts.add13(x1, y1, z1, 1, 0, 0, 0, s1, g1, 0, fnx, fny, fnz);
                verts.add13(x2, y2, z2, 1, 1, 0, 0, s2, g2, 0, fnx, fny, fnz);
                verts.add13(x3, y3, z3, 0, 1, 0, 0, s3, g3, 0, fnx, fny, fnz);

                indices.add6(base, base + 1, base + 2, base, base + 2, base + 3);

                // 2. Legacy arrays
                if (pos != null) {
                    pos.add9(x0, y0, z0, x1, y1, z1, x2, y2, z2);
                    norm.add9(fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz);
                    uv.add12(0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0);
                    col.add9(s0, g0, 0, s1, g1, 0, s2, g2, 0);

                    pos.add9(x0, y0, z0, x2, y2, z2, x3, y3, z3);
                    norm.add9(fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz);
                    uv.add12(0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0);
                    col.add9(s0, g0, 0, s2, g2, 0, s3, g3, 0);
                }
            }
        }
    }

    private static void buildCrossFoliage(
        ChunkNeighborhood neighborhood, int x, int y, int z, int wx, int wz, BlockState block,
        FloatArrayList verts, IntArrayList indices,
        FloatArrayList pos, FloatArrayList norm, FloatArrayList uv, FloatArrayList col
    ) {
        int slot = block.getFaceTextureSlot(0);
        float[] tileBounds = GreedySliceMesher.getTileBounds(slot);
        float uTileMin = tileBounds[0];
        float vTileMin = tileBounds[2];

        int cx = neighborhood.getCenterCx();
        int cz = neighborhood.getCenterCz();
        int hash = Math.abs((wx * 127 + wz * 311) ^ (cx * 53 + cz * 17));
        float jitterX = (((hash % 100) / 100.0f) - 0.5f) * 0.38f;
        float jitterZ = ((((hash >> 3) % 100) / 100.0f) - 0.5f) * 0.38f;

        float w = 0.5f;
        float h = 0.92f;
        float fx = wx + 0.5f + jitterX;
        float fz = wz + 0.5f + jitterZ;

        // Diagonal 1
        addFoliageQuad(fx - w, y, fz - w, fx + w, y, fz + w, fx + w, y + h, fz + w, fx - w, y + h, fz - w,
            uTileMin, vTileMin, verts, indices, pos, norm, uv, col);
        // Diagonal 2
        addFoliageQuad(fx - w, y, fz + w, fx + w, y, fz - w, fx + w, y + h, fz - w, fx - w, y + h, fz + w,
            uTileMin, vTileMin, verts, indices, pos, norm, uv, col);
    }

    private static void addFoliageQuad(
        float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
        float uTileMin, float vTileMin,
        FloatArrayList verts, IntArrayList indices,
        FloatArrayList pos, FloatArrayList norm, FloatArrayList uv, FloatArrayList col
    ) {
        // Front Face
        int base1 = verts.size() / 13;
        verts.add13(x0, y0, z0, 0, 0, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        verts.add13(x1, y1, z1, 1, 0, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        verts.add13(x2, y2, z2, 1, 1, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        verts.add13(x3, y3, z3, 0, 1, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        indices.add6(base1, base1 + 1, base1 + 2, base1, base1 + 2, base1 + 3);

        // Back Face
        int base2 = verts.size() / 13;
        verts.add13(x2, y2, z2, 1, 1, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        verts.add13(x1, y1, z1, 1, 0, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        verts.add13(x0, y0, z0, 0, 0, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        verts.add13(x3, y3, z3, 0, 1, uTileMin, vTileMin, 1.0f, 1.0f, 1.0f, 0, 1, 0);
        indices.add6(base2, base2 + 1, base2 + 2, base2, base2 + 2, base2 + 3);

        if (pos != null) {
            pos.add9(x0, y0, z0, x1, y1, z1, x2, y2, z2);
            pos.add9(x0, y0, z0, x2, y2, z2, x3, y3, z3);
            norm.add9(0, 1, 0, 0, 1, 0, 0, 1, 0);
            norm.add9(0, 1, 0, 0, 1, 0, 0, 1, 0);
            uv.add12(0, 0, uTileMin, vTileMin, 1, 0, uTileMin, vTileMin, 1, 1, uTileMin, vTileMin);
            uv.add12(0, 0, uTileMin, vTileMin, 1, 1, uTileMin, vTileMin, 0, 1, uTileMin, vTileMin);
            col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);

            pos.add9(x2, y2, z2, x1, y1, z1, x0, y0, z0);
            pos.add9(x3, y3, z3, x2, y2, z2, x0, y0, z0);
            norm.add9(0, 1, 0, 0, 1, 0, 0, 1, 0);
            norm.add9(0, 1, 0, 0, 1, 0, 0, 1, 0);
            uv.add12(1, 1, uTileMin, vTileMin, 1, 0, uTileMin, vTileMin, 0, 0, uTileMin, vTileMin);
            uv.add12(0, 1, uTileMin, vTileMin, 1, 1, uTileMin, vTileMin, 0, 0, uTileMin, vTileMin);
            col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public static class FloatArrayList {
        private float[] data;
        private int size;

        public FloatArrayList(int capacity) {
            this.data = new float[capacity];
            this.size = 0;
        }

        public void add(float val) {
            if (size == data.length) {
                float[] next = new float[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = val;
        }

        public void add6(float f1, float f2, float f3, float f4, float f5, float f6) {
            if (size + 6 >= data.length) {
                float[] next = new float[Math.max(data.length * 2, size + 6)];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = f1; data[size++] = f2; data[size++] = f3;
            data[size++] = f4; data[size++] = f5; data[size++] = f6;
        }

        public void add9(float f1, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
            if (size + 9 >= data.length) {
                float[] next = new float[Math.max(data.length * 2, size + 9)];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = f1; data[size++] = f2; data[size++] = f3;
            data[size++] = f4; data[size++] = f5; data[size++] = f6;
            data[size++] = f7; data[size++] = f8; data[size++] = f9;
        }

        public void add12(float f1, float f2, float f3, float f4, float f5, float f6,
                          float f7, float f8, float f9, float f10, float f11, float f12) {
            if (size + 12 >= data.length) {
                float[] next = new float[Math.max(data.length * 2, size + 12)];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = f1;  data[size++] = f2;  data[size++] = f3;  data[size++] = f4;
            data[size++] = f5;  data[size++] = f6;  data[size++] = f7;  data[size++] = f8;
            data[size++] = f9;  data[size++] = f10; data[size++] = f11; data[size++] = f12;
        }

        public void add13(float f1, float f2, float f3, float f4, float f5, float f6,
                          float f7, float f8, float f9, float f10, float f11, float f12, float f13) {
            if (size + 13 >= data.length) {
                float[] next = new float[Math.max(data.length * 2, size + 13)];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = f1;  data[size++] = f2;  data[size++] = f3;  data[size++] = f4;
            data[size++] = f5;  data[size++] = f6;  data[size++] = f7;  data[size++] = f8;
            data[size++] = f9;  data[size++] = f10; data[size++] = f11; data[size++] = f12;
            data[size++] = f13;
        }

        public float[] toArray() {
            float[] result = new float[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }

        public int size() {
            return size;
        }
    }

    public static class IntArrayList {
        private int[] data;
        private int size;

        public IntArrayList(int capacity) {
            this.data = new int[capacity];
            this.size = 0;
        }

        public void add(int val) {
            if (size == data.length) {
                int[] next = new int[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = val;
        }

        public void add6(int i1, int i2, int i3, int i4, int i5, int i6) {
            if (size + 6 >= data.length) {
                int[] next = new int[Math.max(data.length * 2, size + 6)];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = i1; data[size++] = i2; data[size++] = i3;
            data[size++] = i4; data[size++] = i5; data[size++] = i6;
        }

        public int[] toArray() {
            int[] result = new int[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }

        public int size() {
            return size;
        }
    }
}

package com.mcjournal;

public class ChunkMeshBuilder {

    public static class MeshData {
        public float[] solidPositions;
        public float[] solidNormals;
        public float[] solidUvs;
        public float[] solidColors;

        public float[] waterPositions;
        public float[] waterNormals;
        public float[] waterUvs;
        public float[] waterColors;

        public MeshData(
            float[] solidPositions, float[] solidNormals, float[] solidUvs, float[] solidColors,
            float[] waterPositions, float[] waterNormals, float[] waterUvs, float[] waterColors
        ) {
            this.solidPositions = solidPositions;
            this.solidNormals = solidNormals;
            this.solidUvs = solidUvs;
            this.solidColors = solidColors;
            this.waterPositions = waterPositions;
            this.waterNormals = waterNormals;
            this.waterUvs = waterUvs;
            this.waterColors = waterColors;
        }
    }

    private static class FaceDef {
        int[] dir;
        float[] norm;
        float baseShade;
        int[] uAxis;
        int[] vAxis;
        int[][] corners;
        int[][] cornerOffsets;

        FaceDef(int[] dir, float[] norm, float baseShade, int[] uAxis, int[] vAxis, int[][] corners, int[][] cornerOffsets) {
            this.dir = dir;
            this.norm = norm;
            this.baseShade = baseShade;
            this.uAxis = uAxis;
            this.vAxis = vAxis;
            this.corners = corners;
            this.cornerOffsets = cornerOffsets;
        }
    }

    private static final FaceDef[] FACES = new FaceDef[] {
        // 0: Right (+X)
        new FaceDef(
            new int[]{1, 0, 0}, new float[]{1, 0, 0}, 0.75f,
            new int[]{0, 0, 1}, new int[]{0, 1, 0},
            new int[][]{{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}},
            new int[][]{{1, -1}, {-1, -1}, {-1, 1}, {1, 1}}
        ),
        // 1: Left (-X)
        new FaceDef(
            new int[]{-1, 0, 0}, new float[]{-1, 0, 0}, 0.75f,
            new int[]{0, 0, -1}, new int[]{0, 1, 0},
            new int[][]{{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
            new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}
        ),
        // 2: Top (+Y)
        new FaceDef(
            new int[]{0, 1, 0}, new float[]{0, 1, 0}, 1.0f,
            new int[]{1, 0, 0}, new int[]{0, 0, 1},
            new int[][]{{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}},
            new int[][]{{-1, 1}, {1, 1}, {1, -1}, {-1, -1}}
        ),
        // 3: Bottom (-Y)
        new FaceDef(
            new int[]{0, -1, 0}, new float[]{0, -1, 0}, 0.55f,
            new int[]{1, 0, 0}, new int[]{0, 0, -1},
            new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
            new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}
        ),
        // 4: Front (+Z)
        new FaceDef(
            new int[]{0, 0, 1}, new float[]{0, 0, 1}, 0.85f,
            new int[]{-1, 0, 0}, new int[]{0, 1, 0},
            new int[][]{{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}},
            new int[][]{{1, -1}, {-1, -1}, {-1, 1}, {1, 1}}
        ),
        // 5: Back (-Z)
        new FaceDef(
            new int[]{0, 0, -1}, new float[]{0, 0, -1}, 0.85f,
            new int[]{1, 0, 0}, new int[]{0, 1, 0},
            new int[][]{{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}},
            new int[][]{{1, -1}, {-1, -1}, {-1, 1}, {1, 1}}
        )
    };

    private static final float[] AO_CURVE = new float[]{1.0f, 0.78f, 0.58f, 0.42f};
    private static final int GRID_SIZE = 4; // 4x4 Atlas Grid

    private static float[] getUVBounds(int slotIndex) {
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

    private static int getBlockFaceSlot(byte blockType, int faceIndex) {
        return switch (blockType) {
            case Block.GRASS -> (faceIndex == 2) ? 0 : (faceIndex == 3 ? 2 : 1);
            case Block.DIRT -> 2;
            case Block.STONE -> 3;
            case Block.COBBLESTONE -> 4;
            case Block.SAND -> 5;
            case Block.BEDROCK -> 6;
            case Block.OAK_LOG -> (faceIndex == 2 || faceIndex == 3) ? 8 : 7;
            case Block.BIRCH_LOG -> (faceIndex == 2 || faceIndex == 3) ? 8 : 15;
            case Block.OAK_LEAVES, Block.BIRCH_LEAVES -> 9;
            case Block.DIAMOND_ORE -> 10;
            case Block.WATER -> 11;
            case Block.TALL_GRASS -> 12;
            case Block.POPPY -> 13;
            case Block.DANDELION -> 14;
            default -> 2;
        };
    }

    private static boolean isTransparent(byte blockType) {
        return blockType == Block.AIR || blockType == Block.WATER ||
               blockType == Block.TALL_GRASS || blockType == Block.POPPY || blockType == Block.DANDELION;
    }

    private static float computeVertexAO(ChunkManager manager, int x, int y, int z, FaceDef face, int uSign, int vSign) {
        int fx = x + face.dir[0];
        int fy = y + face.dir[1];
        int fz = z + face.dir[2];

        int u1 = face.uAxis[0] * uSign;
        int u2 = face.uAxis[1] * uSign;
        int u3 = face.uAxis[2] * uSign;

        int v1 = face.vAxis[0] * vSign;
        int v2 = face.vAxis[1] * vSign;
        int v3 = face.vAxis[2] * vSign;

        boolean s1 = isAOSolid(manager, fx + u1, fy + u2, fz + u3);
        boolean s2 = isAOSolid(manager, fx + v1, fy + v2, fz + v3);
        boolean corner = isAOSolid(manager, fx + u1 + v1, fy + u2 + v2, fz + u3 + v3);

        int occlusion = 0;
        if (s1) occlusion++;
        if (s2) occlusion++;
        if (s1 && s2) {
            occlusion++;
        } else if (corner) {
            occlusion++;
        }

        return AO_CURVE[Math.min(3, occlusion)];
    }

    private static boolean isAOSolid(ChunkManager manager, int wx, int wy, int wz) {
        byte b = manager.getBlockAt(wx, wy, wz);
        return Block.isSolid(b) && b != Block.OAK_LEAVES && b != Block.BIRCH_LEAVES;
    }

    private static float computeWaterColumnDepth(ChunkManager manager, int wx, int y, int wz) {
        int depth = 1;
        for (int dy = 1; dy <= 16; dy++) {
            int checkY = y - dy;
            if (checkY < 0) break;
            byte b = manager.getBlockAt(wx, checkY, wz);
            if (b == Block.WATER) {
                depth++;
            } else {
                break;
            }
        }
        return (float) depth;
    }

    private static float computeWaterShoreline(ChunkManager manager, int wx, int y, int wz) {
        if (Block.isSolid(manager.getBlockAt(wx + 1, y, wz)) ||
            Block.isSolid(manager.getBlockAt(wx - 1, y, wz)) ||
            Block.isSolid(manager.getBlockAt(wx, y, wz + 1)) ||
            Block.isSolid(manager.getBlockAt(wx, y, wz - 1))) {
            return 1.0f;
        }
        return 0.0f;
    }

    public static MeshData buildMesh(Chunk chunk, ChunkManager manager) {
        FloatArrayList solidPos = new FloatArrayList(16384);
        FloatArrayList solidNorm = new FloatArrayList(16384);
        FloatArrayList solidUv = new FloatArrayList(16384);
        FloatArrayList solidCol = new FloatArrayList(16384);

        FloatArrayList waterPos = new FloatArrayList(4096);
        FloatArrayList waterNorm = new FloatArrayList(4096);
        FloatArrayList waterUv = new FloatArrayList(4096);
        FloatArrayList waterCol = new FloatArrayList(4096);

        int worldOriginX = chunk.getCx() * Chunk.SIZE;
        int worldOriginZ = chunk.getCz() * Chunk.SIZE;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    byte block = chunk.getBlock(x, y, z);
                    if (block == Block.AIR) continue;

                    int wx = worldOriginX + x;
                    int wz = worldOriginZ + z;

                    // 1. Cross-Foliage (Tall Grass, Poppy, Dandelion)
                    if (block == Block.TALL_GRASS || block == Block.POPPY || block == Block.DANDELION) {
                        buildCrossFoliage(chunk, x, y, z, wx, wz, block, solidPos, solidNorm, solidUv, solidCol);
                        continue;
                    }

                    // 2. Voxel Cube Faces
                    for (int f = 0; f < 6; f++) {
                        FaceDef face = FACES[f];
                        int nx = wx + face.dir[0];
                        int ny = y + face.dir[1];
                        int nz = wz + face.dir[2];

                        byte neighbor = manager.getBlockAt(nx, ny, nz);

                        boolean isWater = (block == Block.WATER);
                        boolean shouldDrawFace;

                        if (isWater) {
                            shouldDrawFace = (neighbor != Block.WATER && isTransparent(neighbor));
                        } else if (block == Block.OAK_LEAVES || block == Block.BIRCH_LEAVES) {
                            shouldDrawFace = (neighbor != block && isTransparent(neighbor));
                        } else {
                            shouldDrawFace = isTransparent(neighbor);
                        }

                        if (shouldDrawFace) {
                            int slot = getBlockFaceSlot(block, f);
                            float[] uvBounds = getUVBounds(slot);
                            float uMin = uvBounds[0], uMax = uvBounds[1], vMin = uvBounds[2], vMax = uvBounds[3];

                            // Texture De-Tiling Rotation on Top Faces
                            float[] uv0 = {uMin, vMin}, uv1 = {uMax, vMin}, uv2 = {uMax, vMax}, uv3 = {uMin, vMax};
                            if (f == 2 && (block == Block.STONE || block == Block.DIRT || block == Block.SAND || block == Block.BEDROCK || block == Block.COBBLESTONE)) {
                                int rot = Math.abs((wx * 374761393 + wz * 668265263) ^ (chunk.getCx() * 31 + chunk.getCz())) % 4;
                                if (rot == 1) {
                                    uv0 = new float[]{uMax, vMin}; uv1 = new float[]{uMax, vMax}; uv2 = new float[]{uMin, vMax}; uv3 = new float[]{uMin, vMin};
                                } else if (rot == 2) {
                                    uv0 = new float[]{uMax, vMax}; uv1 = new float[]{uMin, vMax}; uv2 = new float[]{uMin, vMin}; uv3 = new float[]{uMax, vMin};
                                } else if (rot == 3) {
                                    uv0 = new float[]{uMin, vMax}; uv1 = new float[]{uMin, vMin}; uv2 = new float[]{uMax, vMin}; uv3 = new float[]{uMax, vMax};
                                }
                            }

                            // 4 Corners
                            int[][] c = face.corners;
                            float yOffset = isWater ? -0.1f : 0.0f;

                            float[] v0 = {wx + c[0][0], y + c[0][1] + yOffset, wz + c[0][2]};
                            float[] v1 = {wx + c[1][0], y + c[1][1] + yOffset, wz + c[1][2]};
                            float[] v2 = {wx + c[2][0], y + c[2][1] + yOffset, wz + c[2][2]};
                            float[] v3 = {wx + c[3][0], y + c[3][1] + yOffset, wz + c[3][2]};

                            // Ambient Occlusion / Water Optical Parameters
                            float s0, s1, s2, s3;
                            float g0 = 0, g1 = 0, g2 = 0, g3 = 0;
                            float b0 = 0, b1 = 0, b2 = 0, b3 = 0;

                            if (isWater) {
                                s0 = computeWaterColumnDepth(manager, wx + c[0][0], y, wz + c[0][2]) / 8.0f;
                                s1 = computeWaterColumnDepth(manager, wx + c[1][0], y, wz + c[1][2]) / 8.0f;
                                s2 = computeWaterColumnDepth(manager, wx + c[2][0], y, wz + c[2][2]) / 8.0f;
                                s3 = computeWaterColumnDepth(manager, wx + c[3][0], y, wz + c[3][2]) / 8.0f;

                                g0 = computeWaterShoreline(manager, wx + c[0][0], y, wz + c[0][2]);
                                g1 = computeWaterShoreline(manager, wx + c[1][0], y, wz + c[1][2]);
                                g2 = computeWaterShoreline(manager, wx + c[2][0], y, wz + c[2][2]);
                                g3 = computeWaterShoreline(manager, wx + c[3][0], y, wz + c[3][2]);
                            } else {
                                float ao0 = computeVertexAO(manager, wx, y, wz, face, face.cornerOffsets[0][0], face.cornerOffsets[0][1]);
                                float ao1 = computeVertexAO(manager, wx, y, wz, face, face.cornerOffsets[1][0], face.cornerOffsets[1][1]);
                                float ao2 = computeVertexAO(manager, wx, y, wz, face, face.cornerOffsets[2][0], face.cornerOffsets[2][1]);
                                float ao3 = computeVertexAO(manager, wx, y, wz, face, face.cornerOffsets[3][0], face.cornerOffsets[3][1]);

                                s0 = g0 = b0 = ao0;
                                s1 = g1 = b1 = ao1;
                                s2 = g2 = b2 = ao2;
                                s3 = g3 = b3 = ao3;
                            }

                            FloatArrayList targetPos = isWater ? waterPos : solidPos;
                            FloatArrayList targetNorm = isWater ? waterNorm : solidNorm;
                            FloatArrayList targetUv = isWater ? waterUv : solidUv;
                            FloatArrayList targetCol = isWater ? waterCol : solidCol;

                            // Triangulation
                            if (s0 + s2 > s1 + s3) {
                                // Triangle 1 (v0, v1, v2)
                                targetPos.add9(v0[0], v0[1], v0[2], v1[0], v1[1], v1[2], v2[0], v2[1], v2[2]);
                                targetNorm.add9(face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2]);
                                targetUv.add6(uv0[0], uv0[1], uv1[0], uv1[1], uv2[0], uv2[1]);
                                targetCol.add9(s0, g0, b0, s1, g1, b1, s2, g2, b2);

                                // Triangle 2 (v0, v2, v3)
                                targetPos.add9(v0[0], v0[1], v0[2], v2[0], v2[1], v2[2], v3[0], v3[1], v3[2]);
                                targetNorm.add9(face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2]);
                                targetUv.add6(uv0[0], uv0[1], uv2[0], uv2[1], uv3[0], uv3[1]);
                                targetCol.add9(s0, g0, b0, s2, g2, b2, s3, g3, b3);
                            } else {
                                // Triangle 1 (v1, v2, v3)
                                targetPos.add9(v1[0], v1[1], v1[2], v2[0], v2[1], v2[2], v3[0], v3[1], v3[2]);
                                targetNorm.add9(face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2]);
                                targetUv.add6(uv1[0], uv1[1], uv2[0], uv2[1], uv3[0], uv3[1]);
                                targetCol.add9(s1, g1, b1, s2, g2, b2, s3, g3, b3);

                                // Triangle 2 (v1, v3, v0)
                                targetPos.add9(v1[0], v1[1], v1[2], v3[0], v3[1], v3[2], v0[0], v0[1], v0[2]);
                                targetNorm.add9(face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2], face.norm[0], face.norm[1], face.norm[2]);
                                targetUv.add6(uv1[0], uv1[1], uv3[0], uv3[1], uv0[0], uv0[1]);
                                targetCol.add9(s1, g1, b1, s3, g3, b3, s0, g0, b0);
                            }
                        }
                    }
                }
            }
        }

        return new MeshData(
            solidPos.toArray(), solidNorm.toArray(), solidUv.toArray(), solidCol.toArray(),
            waterPos.toArray(), waterNorm.toArray(), waterUv.toArray(), waterCol.toArray()
        );
    }

    private static void buildCrossFoliage(
        Chunk chunk, int x, int y, int z, int wx, int wz, byte block,
        FloatArrayList pos, FloatArrayList norm, FloatArrayList uv, FloatArrayList col
    ) {
        int slot = getBlockFaceSlot(block, 0);
        float[] uvBounds = getUVBounds(slot);
        float uMin = uvBounds[0], uMax = uvBounds[1], vMin = uvBounds[2], vMax = uvBounds[3];

        int hash = Math.abs((wx * 127 + wz * 311) ^ (chunk.getCx() * 53 + chunk.getCz() * 17));
        float jitterX = (((hash % 100) / 100.0f) - 0.5f) * 0.38f;
        float jitterZ = ((((hash >> 3) % 100) / 100.0f) - 0.5f) * 0.38f;

        float w = 0.5f;
        float h = 0.92f;
        float cx = wx + 0.5f + jitterX;
        float cz = wz + 0.5f + jitterZ;

        // Diagonal 1
        addFoliageQuad(cx - w, y, cz - w, cx + w, y, cz + w, cx + w, y + h, cz + w, cx - w, y + h, cz - w, uMin, uMax, vMin, vMax, pos, norm, uv, col);
        // Diagonal 2
        addFoliageQuad(cx - w, y, cz + w, cx + w, y, cz - w, cx + w, y + h, cz - w, cx - w, y + h, cz + w, uMin, uMax, vMin, vMax, pos, norm, uv, col);
    }

    private static void addFoliageQuad(
        float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
        float uMin, float uMax, float vMin, float vMax,
        FloatArrayList pos, FloatArrayList norm, FloatArrayList uv, FloatArrayList col
    ) {
        pos.add9(x0, y0, z0, x1, y1, z1, x2, y2, z2);
        pos.add9(x0, y0, z0, x2, y2, z2, x3, y3, z3);

        norm.add9(0, 1, 0, 0, 1, 0, 0, 1, 0);
        norm.add9(0, 1, 0, 0, 1, 0, 0, 1, 0);

        uv.add6(uMin, vMin, uMax, vMin, uMax, vMax);
        uv.add6(uMin, vMin, uMax, vMax, uMin, vMax);

        col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        col.add9(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
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

        public float[] toArray() {
            float[] result = new float[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }

        public int size() {
            return size;
        }
    }
}

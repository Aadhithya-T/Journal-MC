package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import com.mcjournal.Item;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ItemEntityManager {
    private static final int MAX_ITEMS = 512;
    private static final int FLOATS_PER_VERTEX = 11; // Pos:3, UV:2, Color:3, Normal:3
    private static final int VERTICES_PER_CUBE = 36;
    private static final int BUFFER_CAPACITY = MAX_ITEMS * VERTICES_PER_CUBE * FLOATS_PER_VERTEX;

    private final List<ItemEntity> items = new ArrayList<>();
    private final List<ItemEntity> spawnQueue = new ArrayList<>();

    private int vao;
    private int vbo;
    private FloatBuffer vertexBuffer;

    public void init() {
        vertexBuffer = MemoryUtil.memAllocFloat(BUFFER_CAPACITY);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) BUFFER_CAPACITY * Float.BYTES, GL_DYNAMIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;

        // 0: Position (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // 1: UV (vec2)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // 2: Color / AO (vec3)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // 3: Normal (vec3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void spawnItem(byte blockType, int count, float x, float y, float z) {
        if (blockType == Block.AIR || count <= 0) return;
        synchronized (spawnQueue) {
            spawnQueue.add(new ItemEntity(blockType, count, x, y, z));
        }
    }

    public void spawnThrownItem(byte blockType, int count, float x, float y, float z, float vx, float vy, float vz) {
        if (blockType == Block.AIR || count <= 0) return;
        synchronized (spawnQueue) {
            spawnQueue.add(new ItemEntity(blockType, count, x, y, z, vx, vy, vz, 1.2f));
        }
    }

    public void update(double deltaTime, ChunkManager world, Player player) {
        float dt = (float) deltaTime;

        // Drain pending spawn queue
        synchronized (spawnQueue) {
            if (!spawnQueue.isEmpty()) {
                items.addAll(spawnQueue);
                spawnQueue.clear();
            }
        }

        // Single-pass iterator traversal to prevent ConcurrentModificationException
        Iterator<ItemEntity> it = items.iterator();
        while (it.hasNext()) {
            ItemEntity item = it.next();
            item.update(dt, world, player);

            // 1. Check 5-minute despawn expiry
            if (item.isExpired()) {
                it.remove();
                continue;
            }

            // 2. Check Player Magnet Pickup Collection
            if (player != null && !player.isDead && item.pickupDelay <= 0.0f && player.canAddItem(item.blockType)) {
                float px = player.pos.x;
                float py = player.pos.y + 0.8f;
                float pz = player.pos.z;

                float dx = px - item.pos.x;
                float dy = py - item.pos.y;
                float dz = pz - item.pos.z;
                float distSq = dx * dx + dy * dy + dz * dz;

                if (distSq < ItemEntity.PICKUP_RADIUS * ItemEntity.PICKUP_RADIUS) {
                    int unadded = player.addItem(item.blockType, item.count);
                    if (unadded == 0) {
                        // Entire stack collected
                        System.out.println("[Inventory] 📦 Picked up " + item.count + "x " + Block.getName(item.blockType));
                        it.remove();
                    } else if (unadded < item.count) {
                        // Partial stack collected (inventory almost full)
                        int collected = item.count - unadded;
                        System.out.println("[Inventory] 📦 Picked up " + collected + "x " + Block.getName(item.blockType) + " (" + unadded + " remaining on ground)");
                        item.count = unadded;
                        item.pickupDelay = 0.5f;
                        item.velocity.x *= 0.1f;
                        item.velocity.z *= 0.1f;
                    } else {
                        // Inventory completely full: leave item on the ground safely
                        item.pickupDelay = 0.5f;
                        item.velocity.x *= 0.1f;
                        item.velocity.z *= 0.1f;
                    }
                }
            }
        }
    }

    public void render(ShaderProgram chunkShader, Camera camera, float partialTick) {
        if (items.isEmpty() || vertexBuffer == null) return;

        vertexBuffer.clear();
        int totalVertices = 0;

        for (ItemEntity item : items) {
            if (item.isBlinking()) continue; // Despawn warning blink

            float ix = item.prevPos.x + (item.pos.x - item.prevPos.x) * partialTick;
            float iy = item.getVisualY(partialTick);
            float iz = item.prevPos.z + (item.pos.z - item.prevPos.z) * partialTick;

            float rad = (float) Math.toRadians(item.rotation);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float s = ItemEntity.SIZE;
            float halfS = s / 2.0f;

            if (Item.isTool(item.blockType)) {
                // Render Single Flat Double-Sided Spinning Sprite for Tools (Authentic Minecraft Item Entity)
                int tile = Item.getItemTile(item.blockType);
                float[] uv = TextureAtlas.getTileUV(tile);
                float uMin = uv[0]; float uMax = uv[1];
                float vMin = uv[2]; float vMax = uv[3];

                float itemSize = ItemEntity.SIZE * 1.25f;
                float halfItem = itemSize / 2.0f;

                // Front Face (Facing +Z in local rotated frame)
                putRotatedVertex(vertexBuffer, -halfItem, 0, 0, uMin, vMin, 0, 0, 1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfItem, 0, 0, uMax, vMin, 0, 0, 1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfItem, itemSize, 0, uMax, vMax, 0, 0, 1, cos, sin, ix, iy, iz);

                putRotatedVertex(vertexBuffer, -halfItem, 0, 0, uMin, vMin, 0, 0, 1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfItem, itemSize, 0, uMax, vMax, 0, 0, 1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfItem, itemSize, 0, uMin, vMax, 0, 0, 1, cos, sin, ix, iy, iz);

                // Back Face (Facing -Z in local rotated frame - horizontally mirrored UV for correct appearance from both sides)
                putRotatedVertex(vertexBuffer,  halfItem, 0, 0, uMin, vMin, 0, 0, -1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfItem, 0, 0, uMax, vMin, 0, 0, -1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfItem, itemSize, 0, uMax, vMax, 0, 0, -1, cos, sin, ix, iy, iz);

                putRotatedVertex(vertexBuffer,  halfItem, 0, 0, uMin, vMin, 0, 0, -1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfItem, itemSize, 0, uMax, vMax, 0, 0, -1, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfItem, itemSize, 0, uMin, vMax, 0, 0, -1, cos, sin, ix, iy, iz);

                totalVertices += 12;
            } else if (Block.isPlant(item.blockType)) {
                // Render crossed 2-quad foliage for flowers & tall grass (X-cross)
                int tile = Block.getDisplayFaceTile(item.blockType);
                float[] uv = TextureAtlas.getTileUV(tile);
                float uMin = uv[0]; float uMax = uv[1];
                float vMin = uv[2]; float vMax = uv[3];

                // Quad 1: Diagonal / (Double-sided)
                putRotatedVertex(vertexBuffer, -halfS, 0, -halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, 0,  halfS, uMax, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s,  halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);

                putRotatedVertex(vertexBuffer, -halfS, 0, -halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s,  halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfS, s, -halfS, uMin, vMax, 0, 1, 0, cos, sin, ix, iy, iz);

                // Backside of Quad 1
                putRotatedVertex(vertexBuffer, -halfS, 0, -halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfS, s, -halfS, uMin, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s,  halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);

                putRotatedVertex(vertexBuffer, -halfS, 0, -halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s,  halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, 0,  halfS, uMax, vMin, 0, 1, 0, cos, sin, ix, iy, iz);

                // Quad 2: Diagonal \ (Double-sided)
                putRotatedVertex(vertexBuffer, -halfS, 0,  halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, 0, -halfS, uMax, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s, -halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);

                putRotatedVertex(vertexBuffer, -halfS, 0,  halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s, -halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfS, s,  halfS, uMin, vMax, 0, 1, 0, cos, sin, ix, iy, iz);

                // Backside of Quad 2
                putRotatedVertex(vertexBuffer, -halfS, 0,  halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer, -halfS, s,  halfS, uMin, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s, -halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);

                putRotatedVertex(vertexBuffer, -halfS, 0,  halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, s, -halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                putRotatedVertex(vertexBuffer,  halfS, 0, -halfS, uMax, vMin, 0, 1, 0, cos, sin, ix, iy, iz);

                totalVertices += 24;
            } else {
                // 3D Miniature Block Cube (6 Faces with authentic textures & orientation)
                for (int face = 0; face < 6; face++) {
                    int slot = Block.getBlockFaceSlot(item.blockType, face);
                    float[] uv = TextureAtlas.getTileUV(slot);
                    float uMin = uv[0]; float uMax = uv[1];
                    float vMin = uv[2]; float vMax = uv[3];

                    switch (face) {
                        case 0 -> { // East (+X)
                            putRotatedVertex(vertexBuffer, halfS, -halfS,  halfS, uMin, vMin, 1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, halfS, -halfS, -halfS, uMax, vMin, 1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, halfS,  halfS, -halfS, uMax, vMax, 1, 0, 0, cos, sin, ix, iy, iz);

                            putRotatedVertex(vertexBuffer, halfS, -halfS,  halfS, uMin, vMin, 1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, halfS,  halfS, -halfS, uMax, vMax, 1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, halfS,  halfS,  halfS, uMin, vMax, 1, 0, 0, cos, sin, ix, iy, iz);
                        }
                        case 1 -> { // West (-X)
                            putRotatedVertex(vertexBuffer, -halfS, -halfS, -halfS, uMin, vMin, -1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS, -halfS,  halfS, uMax, vMin, -1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS,  halfS,  halfS, uMax, vMax, -1, 0, 0, cos, sin, ix, iy, iz);

                            putRotatedVertex(vertexBuffer, -halfS, -halfS, -halfS, uMin, vMin, -1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS,  halfS,  halfS, uMax, vMax, -1, 0, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS,  halfS, -halfS, uMin, vMax, -1, 0, 0, cos, sin, ix, iy, iz);
                        }
                        case 2 -> { // Top (+Y)
                            putRotatedVertex(vertexBuffer, -halfS, halfS, -halfS, uMin, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS, halfS,  halfS, uMin, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, halfS,  halfS, uMax, vMin, 0, 1, 0, cos, sin, ix, iy, iz);

                            putRotatedVertex(vertexBuffer, -halfS, halfS, -halfS, uMin, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, halfS,  halfS, uMax, vMin, 0, 1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, halfS, -halfS, uMax, vMax, 0, 1, 0, cos, sin, ix, iy, iz);
                        }
                        case 3 -> { // Bottom (-Y)
                            putRotatedVertex(vertexBuffer, -halfS, -halfS,  halfS, uMin, vMin, 0, -1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, -halfS,  halfS, uMax, vMin, 0, -1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, -halfS, -halfS, uMax, vMax, 0, -1, 0, cos, sin, ix, iy, iz);

                            putRotatedVertex(vertexBuffer, -halfS, -halfS,  halfS, uMin, vMin, 0, -1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, -halfS, -halfS, uMax, vMax, 0, -1, 0, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS, -halfS, -halfS, uMin, vMax, 0, -1, 0, cos, sin, ix, iy, iz);
                        }
                        case 4 -> { // South (+Z)
                            putRotatedVertex(vertexBuffer, -halfS, -halfS, halfS, uMin, vMin, 0, 0, 1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS, -halfS, halfS, uMax, vMin, 0, 0, 1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS,  halfS, halfS, uMax, vMax, 0, 0, 1, cos, sin, ix, iy, iz);

                            putRotatedVertex(vertexBuffer, -halfS, -halfS, halfS, uMin, vMin, 0, 0, 1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS,  halfS, halfS, uMax, vMax, 0, 0, 1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS,  halfS, halfS, uMin, vMax, 0, 0, 1, cos, sin, ix, iy, iz);
                        }
                        case 5 -> { // North (-Z)
                            putRotatedVertex(vertexBuffer,  halfS, -halfS, -halfS, uMin, vMin, 0, 0, -1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS, -halfS, -halfS, uMax, vMin, 0, 0, -1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS,  halfS, -halfS, uMax, vMax, 0, 0, -1, cos, sin, ix, iy, iz);

                            putRotatedVertex(vertexBuffer,  halfS, -halfS, -halfS, uMin, vMin, 0, 0, -1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer, -halfS,  halfS, -halfS, uMax, vMax, 0, 0, -1, cos, sin, ix, iy, iz);
                            putRotatedVertex(vertexBuffer,  halfS,  halfS, -halfS, uMin, vMax, 0, 0, -1, cos, sin, ix, iy, iz);
                        }
                    }
                    totalVertices += 6;
                }
            }

            if (totalVertices + VERTICES_PER_CUBE >= BUFFER_CAPACITY / FLOATS_PER_VERTEX) {
                break; // Buffer safety clamp
            }
        }

        if (totalVertices == 0) return;

        vertexBuffer.flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glDrawArrays(GL_TRIANGLES, 0, totalVertices);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void putRotatedVertex(FloatBuffer buf, float lx, float ly, float lz,
                                  float u, float v,
                                  float lnx, float lny, float lnz,
                                  float cos, float sin,
                                  float wx, float wy, float wz) {
        // Rotate local vertex about Y axis and translate to world space
        float rx = lx * cos - lz * sin + wx;
        float ry = ly + wy;
        float rz = lx * sin + lz * cos + wz;

        // Rotate normal
        float nx = lnx * cos - lnz * sin;
        float ny = lny;
        float nz = lnx * sin + lnz * cos;

        // Pos:3
        buf.put(rx).put(ry).put(rz);
        // UV:2
        buf.put(u).put(v);
        // Color/AO:3 (Pure full ambient 1.0f)
        buf.put(1.0f).put(1.0f).put(1.0f);
        // Normal:3
        buf.put(nx).put(ny).put(nz);
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

    public void cleanup() {
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (vertexBuffer != null) {
            MemoryUtil.memFree(vertexBuffer);
            vertexBuffer = null;
        }
        items.clear();
        spawnQueue.clear();
    }
}

package com.mcjournal.client;

import com.mcjournal.ChunkPos;
import com.mcjournal.ChunkMeshBuilder;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ChunkRenderer {
    public static class GPUChunkMesh {
        public int solidVao = 0;
        public int solidVbo = 0;
        public int solidEbo = 0;
        public int solidIndexCount = 0;

        public int cutoutVao = 0;
        public int cutoutVbo = 0;
        public int cutoutEbo = 0;
        public int cutoutIndexCount = 0;

        public int waterVao = 0;
        public int waterVbo = 0;
        public int waterEbo = 0;
        public int waterIndexCount = 0;

        public boolean isEmpty() {
            return solidIndexCount == 0 && cutoutIndexCount == 0 && waterIndexCount == 0;
        }

        public void cleanup() {
            if (solidVao != 0) glDeleteVertexArrays(solidVao);
            if (solidVbo != 0) glDeleteBuffers(solidVbo);
            if (solidEbo != 0) glDeleteBuffers(solidEbo);

            if (cutoutVao != 0) glDeleteVertexArrays(cutoutVao);
            if (cutoutVbo != 0) glDeleteBuffers(cutoutVbo);
            if (cutoutEbo != 0) glDeleteBuffers(cutoutEbo);

            if (waterVao != 0) glDeleteVertexArrays(waterVao);
            if (waterVbo != 0) glDeleteBuffers(waterVbo);
            if (waterEbo != 0) glDeleteBuffers(waterEbo);

            solidVao = cutoutVao = waterVao = 0;
            solidVbo = cutoutVbo = waterVbo = 0;
            solidEbo = cutoutEbo = waterEbo = 0;
            solidIndexCount = cutoutIndexCount = waterIndexCount = 0;
        }
    }

    private final Map<ChunkPos, GPUChunkMesh> meshes = new ConcurrentHashMap<>();
    private final List<ChunkPos> visibleChunks = new ArrayList<>(256);
    private int lastRenderedChunks = 0;

    public void uploadChunkMesh(int cx, int cz, ChunkMeshBuilder.MeshData meshData) {
        uploadChunkMesh(new ChunkPos(cx, cz), meshData);
    }

    public void uploadChunkMesh(ChunkPos pos, ChunkMeshBuilder.MeshData meshData) {
        if (meshData == null) return;
        long uploadStart = System.nanoTime();
        GPUChunkMesh mesh = meshes.computeIfAbsent(pos, k -> new GPUChunkMesh());

        int stride = com.mcjournal.EngineConstants.BYTES_PER_VERTEX; // 52 bytes per interleaved vertex

        // 1. Upload Solid Mesh (Interleaved VBO + EBO)
        if (meshData.solidVertices != null && meshData.solidVertices.length > 0
            && meshData.solidIndices != null && meshData.solidIndices.length > 0) {
            if (mesh.solidVao == 0) mesh.solidVao = glGenVertexArrays();
            glBindVertexArray(mesh.solidVao);

            if (mesh.solidVbo == 0) mesh.solidVbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.solidVbo);
            FloatBuffer vBuf = MemoryUtil.memAllocFloat(meshData.solidVertices.length);
            vBuf.put(meshData.solidVertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);
            MemoryUtil.memFree(vBuf);

            // 0: Pos (vec3) -> offset 0
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);

            // 1: UV (vec4) -> offset 12 bytes
            glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            // 2: Color / AO (vec3) -> offset 28 bytes
            glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 7 * Float.BYTES);
            glEnableVertexAttribArray(2);

            // 3: Normal (vec3) -> offset 40 bytes
            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 10 * Float.BYTES);
            glEnableVertexAttribArray(3);

            // EBO (Index Buffer)
            if (mesh.solidEbo == 0) mesh.solidEbo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.solidEbo);
            IntBuffer iBuf = MemoryUtil.memAllocInt(meshData.solidIndices.length);
            iBuf.put(meshData.solidIndices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_STATIC_DRAW);
            MemoryUtil.memFree(iBuf);

            mesh.solidIndexCount = meshData.solidIndices.length;
            glBindVertexArray(0);
        } else {
            mesh.solidIndexCount = 0;
        }

        // 2. Upload Cutout Mesh (Cross Foliage & Leaves)
        if (meshData.cutoutVertices != null && meshData.cutoutVertices.length > 0
            && meshData.cutoutIndices != null && meshData.cutoutIndices.length > 0) {
            if (mesh.cutoutVao == 0) mesh.cutoutVao = glGenVertexArrays();
            glBindVertexArray(mesh.cutoutVao);

            if (mesh.cutoutVbo == 0) mesh.cutoutVbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.cutoutVbo);
            FloatBuffer vBuf = MemoryUtil.memAllocFloat(meshData.cutoutVertices.length);
            vBuf.put(meshData.cutoutVertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);
            MemoryUtil.memFree(vBuf);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);

            glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 7 * Float.BYTES);
            glEnableVertexAttribArray(2);

            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 10 * Float.BYTES);
            glEnableVertexAttribArray(3);

            if (mesh.cutoutEbo == 0) mesh.cutoutEbo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.cutoutEbo);
            IntBuffer iBuf = MemoryUtil.memAllocInt(meshData.cutoutIndices.length);
            iBuf.put(meshData.cutoutIndices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_STATIC_DRAW);
            MemoryUtil.memFree(iBuf);

            mesh.cutoutIndexCount = meshData.cutoutIndices.length;
            glBindVertexArray(0);
        } else {
            mesh.cutoutIndexCount = 0;
        }

        // 3. Upload Water Mesh (Translucent Fluids)
        if (meshData.waterVertices != null && meshData.waterVertices.length > 0
            && meshData.waterIndices != null && meshData.waterIndices.length > 0) {
            if (mesh.waterVao == 0) mesh.waterVao = glGenVertexArrays();
            glBindVertexArray(mesh.waterVao);

            if (mesh.waterVbo == 0) mesh.waterVbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.waterVbo);
            FloatBuffer vBuf = MemoryUtil.memAllocFloat(meshData.waterVertices.length);
            vBuf.put(meshData.waterVertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);
            MemoryUtil.memFree(vBuf);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);

            glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 7 * Float.BYTES);
            glEnableVertexAttribArray(2);

            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 10 * Float.BYTES);
            glEnableVertexAttribArray(3);

            if (mesh.waterEbo == 0) mesh.waterEbo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.waterEbo);
            IntBuffer iBuf = MemoryUtil.memAllocInt(meshData.waterIndices.length);
            iBuf.put(meshData.waterIndices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_STATIC_DRAW);
            MemoryUtil.memFree(iBuf);

            mesh.waterIndexCount = meshData.waterIndices.length;
            glBindVertexArray(0);
        } else {
            mesh.waterIndexCount = 0;
        }

        EngineMetrics.getInstance().recordGpuUpload();
        EngineMetrics.getInstance().recordGpuUploadTime((System.nanoTime() - uploadStart) / 1_000_000.0);
    }

    /**
     * Updates visibility once per frame using combined radial distance culling and view-frustum testing.
     * Caches visible chunk positions so subsequent render passes (Solid, Cutout, Water) execute in O(V)
     * without re-iterating hash maps or re-evaluating frustum planes.
     */
    public void updateVisibility(FrustumCuller culler, float camX, float camZ, int renderDistance) {
        visibleChunks.clear();
        int camChunkX = Math.floorDiv((int) Math.floor(camX), com.mcjournal.EngineConstants.CHUNK_SIZE);
        int camChunkZ = Math.floorDiv((int) Math.floor(camZ), com.mcjournal.EngineConstants.CHUNK_SIZE);
        int maxDistSq = (renderDistance + 1) * (renderDistance + 1);

        for (Map.Entry<ChunkPos, GPUChunkMesh> entry : meshes.entrySet()) {
            ChunkPos pos = entry.getKey();
            GPUChunkMesh mesh = entry.getValue();
            if (mesh == null || mesh.isEmpty()) continue;

            // 1. Fast O(1) Radial Distance Culling
            int dx = pos.x() - camChunkX;
            int dz = pos.z() - camChunkZ;
            if (dx * dx + dz * dz > maxDistSq) {
                continue;
            }

            // 2. View-Frustum Plane Culling
            if (culler != null && !culler.isChunkInFrustum(pos.x(), pos.z())) {
                continue;
            }

            visibleChunks.add(pos);
        }
    }

    public int getLastRenderedChunks() {
        return lastRenderedChunks;
    }

    public List<ChunkPos> getVisibleChunks() {
        return visibleChunks;
    }

    public void renderSolid() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        int count = 0;
        for (int i = 0; i < visibleChunks.size(); i++) {
            ChunkPos pos = visibleChunks.get(i);
            GPUChunkMesh mesh = meshes.get(pos);
            if (mesh != null && mesh.solidVao != 0 && mesh.solidIndexCount > 0) {
                glBindVertexArray(mesh.solidVao);
                glDrawElements(GL_TRIANGLES, mesh.solidIndexCount, GL_UNSIGNED_INT, 0);
                EngineMetrics.getInstance().recordDrawCall((mesh.solidIndexCount / 6) * 4);
                count++;
            }
        }
        glBindVertexArray(0);
        this.lastRenderedChunks = count;
    }

    public void renderSolid(FrustumCuller culler) {
        if (visibleChunks.isEmpty() && culler != null && !meshes.isEmpty()) {
            updateVisibility(culler, 0, 0, 16);
        }
        renderSolid();
    }

    public void renderCutout() {
        glDisable(GL_CULL_FACE); // Double-sided visibility for cross foliage

        for (int i = 0; i < visibleChunks.size(); i++) {
            ChunkPos pos = visibleChunks.get(i);
            GPUChunkMesh mesh = meshes.get(pos);
            if (mesh != null && mesh.cutoutVao != 0 && mesh.cutoutIndexCount > 0) {
                glBindVertexArray(mesh.cutoutVao);
                glDrawElements(GL_TRIANGLES, mesh.cutoutIndexCount, GL_UNSIGNED_INT, 0);
                EngineMetrics.getInstance().recordDrawCall((mesh.cutoutIndexCount / 6) * 4);
            }
        }
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void renderCutout(FrustumCuller culler) {
        renderCutout();
    }

    public void renderWater(boolean isUnderwater) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        if (isUnderwater) {
            glDisable(GL_CULL_FACE);
        } else {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
        }

        for (int i = 0; i < visibleChunks.size(); i++) {
            ChunkPos pos = visibleChunks.get(i);
            GPUChunkMesh mesh = meshes.get(pos);
            if (mesh != null && mesh.waterVao != 0 && mesh.waterIndexCount > 0) {
                glBindVertexArray(mesh.waterVao);
                glDrawElements(GL_TRIANGLES, mesh.waterIndexCount, GL_UNSIGNED_INT, 0);
                EngineMetrics.getInstance().recordDrawCall((mesh.waterIndexCount / 6) * 4);
            }
        }
        glBindVertexArray(0);
        glDepthMask(true);
    }

    public void renderWater(FrustumCuller culler, boolean isUnderwater) {
        renderWater(isUnderwater);
    }

    public void unloadChunkMesh(int cx, int cz) {
        unloadChunkMesh(new ChunkPos(cx, cz));
    }

    public void unloadChunkMesh(ChunkPos pos) {
        GPUChunkMesh mesh = meshes.remove(pos);
        if (mesh != null) {
            mesh.cleanup();
        }
    }

    public int getLoadedMeshCount() {
        return meshes.size();
    }

    public void cleanup() {
        for (GPUChunkMesh mesh : meshes.values()) {
            mesh.cleanup();
        }
        meshes.clear();
        visibleChunks.clear();
    }
}

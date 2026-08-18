package com.mcjournal.client;

import com.mcjournal.ChunkMeshBuilder;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ChunkRenderer {
    public static class GPUChunkMesh {
        public int solidVao = 0;
        public int solidVboPos = 0;
        public int solidVboUv = 0;
        public int solidVboCol = 0;
        public int solidVboNorm = 0;
        public int solidVertexCount = 0;

        public int waterVao = 0;
        public int waterVboPos = 0;
        public int waterVboUv = 0;
        public int waterVboCol = 0;
        public int waterVboNorm = 0;
        public int waterVertexCount = 0;

        public void cleanup() {
            if (solidVao != 0) glDeleteVertexArrays(solidVao);
            if (solidVboPos != 0) glDeleteBuffers(solidVboPos);
            if (solidVboUv != 0) glDeleteBuffers(solidVboUv);
            if (solidVboCol != 0) glDeleteBuffers(solidVboCol);
            if (solidVboNorm != 0) glDeleteBuffers(solidVboNorm);

            if (waterVao != 0) glDeleteVertexArrays(waterVao);
            if (waterVboPos != 0) glDeleteBuffers(waterVboPos);
            if (waterVboUv != 0) glDeleteBuffers(waterVboUv);
            if (waterVboCol != 0) glDeleteBuffers(waterVboCol);
            if (waterVboNorm != 0) glDeleteBuffers(waterVboNorm);

            solidVao = waterVao = 0;
        }
    }

    private final Map<String, GPUChunkMesh> meshes = new ConcurrentHashMap<>();

    public void uploadChunkMesh(int cx, int cz, ChunkMeshBuilder.MeshData meshData) {
        String key = cx + "," + cz;
        GPUChunkMesh mesh = meshes.computeIfAbsent(key, k -> new GPUChunkMesh());

        // Upload Solid Mesh
        if (meshData.solidPositions != null && meshData.solidPositions.length > 0) {
            if (mesh.solidVao == 0) mesh.solidVao = glGenVertexArrays();
            glBindVertexArray(mesh.solidVao);

            mesh.solidVertexCount = meshData.solidPositions.length / 3;

            // 1. Positions (Location 0)
            if (mesh.solidVboPos == 0) mesh.solidVboPos = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.solidVboPos);
            FloatBuffer posBuf = MemoryUtil.memAllocFloat(meshData.solidPositions.length);
            posBuf.put(meshData.solidPositions).flip();
            glBufferData(GL_ARRAY_BUFFER, posBuf, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);
            MemoryUtil.memFree(posBuf);

            // 2. UVs (Location 1)
            if (mesh.solidVboUv == 0) mesh.solidVboUv = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.solidVboUv);
            FloatBuffer uvBuf = MemoryUtil.memAllocFloat(meshData.solidUvs.length);
            uvBuf.put(meshData.solidUvs).flip();
            glBufferData(GL_ARRAY_BUFFER, uvBuf, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);
            MemoryUtil.memFree(uvBuf);

            // 3. Colors / AO (Location 2)
            if (mesh.solidVboCol == 0) mesh.solidVboCol = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.solidVboCol);
            FloatBuffer colBuf = MemoryUtil.memAllocFloat(meshData.solidColors.length);
            colBuf.put(meshData.solidColors).flip();
            glBufferData(GL_ARRAY_BUFFER, colBuf, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(2);
            MemoryUtil.memFree(colBuf);

            // 4. Normals (Location 3)
            if (meshData.solidNormals != null && meshData.solidNormals.length > 0) {
                if (mesh.solidVboNorm == 0) mesh.solidVboNorm = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, mesh.solidVboNorm);
                FloatBuffer normBuf = MemoryUtil.memAllocFloat(meshData.solidNormals.length);
                normBuf.put(meshData.solidNormals).flip();
                glBufferData(GL_ARRAY_BUFFER, normBuf, GL_STATIC_DRAW);
                glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
                glEnableVertexAttribArray(3);
                MemoryUtil.memFree(normBuf);
            }

            glBindVertexArray(0);
        } else {
            mesh.solidVertexCount = 0;
        }

        // Upload Water Mesh
        if (meshData.waterPositions != null && meshData.waterPositions.length > 0) {
            if (mesh.waterVao == 0) mesh.waterVao = glGenVertexArrays();
            glBindVertexArray(mesh.waterVao);

            mesh.waterVertexCount = meshData.waterPositions.length / 3;

            // 1. Positions (Location 0)
            if (mesh.waterVboPos == 0) mesh.waterVboPos = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.waterVboPos);
            FloatBuffer posBuf = MemoryUtil.memAllocFloat(meshData.waterPositions.length);
            posBuf.put(meshData.waterPositions).flip();
            glBufferData(GL_ARRAY_BUFFER, posBuf, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);
            MemoryUtil.memFree(posBuf);

            // 2. UVs (Location 1)
            if (mesh.waterVboUv == 0) mesh.waterVboUv = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.waterVboUv);
            FloatBuffer uvBuf = MemoryUtil.memAllocFloat(meshData.waterUvs.length);
            uvBuf.put(meshData.waterUvs).flip();
            glBufferData(GL_ARRAY_BUFFER, uvBuf, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);
            MemoryUtil.memFree(uvBuf);

            // 3. Colors / AO (Location 2)
            if (mesh.waterVboCol == 0) mesh.waterVboCol = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, mesh.waterVboCol);
            FloatBuffer colBuf = MemoryUtil.memAllocFloat(meshData.waterColors.length);
            colBuf.put(meshData.waterColors).flip();
            glBufferData(GL_ARRAY_BUFFER, colBuf, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(2);
            MemoryUtil.memFree(colBuf);

            // 4. Normals (Location 3)
            if (meshData.waterNormals != null && meshData.waterNormals.length > 0) {
                if (mesh.waterVboNorm == 0) mesh.waterVboNorm = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, mesh.waterVboNorm);
                FloatBuffer normBuf = MemoryUtil.memAllocFloat(meshData.waterNormals.length);
                normBuf.put(meshData.waterNormals).flip();
                glBufferData(GL_ARRAY_BUFFER, normBuf, GL_STATIC_DRAW);
                glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
                glEnableVertexAttribArray(3);
                MemoryUtil.memFree(normBuf);
            }

            glBindVertexArray(0);
        } else {
            mesh.waterVertexCount = 0;
        }
    }

    public void renderSolid() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        for (GPUChunkMesh mesh : meshes.values()) {
            if (mesh.solidVao != 0 && mesh.solidVertexCount > 0) {
                glBindVertexArray(mesh.solidVao);
                glDrawArrays(GL_TRIANGLES, 0, mesh.solidVertexCount);
            }
        }
        glBindVertexArray(0);
    }

    public void renderWater() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        for (GPUChunkMesh mesh : meshes.values()) {
            if (mesh.waterVao != 0 && mesh.waterVertexCount > 0) {
                glBindVertexArray(mesh.waterVao);
                glDrawArrays(GL_TRIANGLES, 0, mesh.waterVertexCount);
            }
        }
        glBindVertexArray(0);

        glDepthMask(true);
    }

    public void cleanup() {
        for (GPUChunkMesh mesh : meshes.values()) {
            mesh.cleanup();
        }
        meshes.clear();
    }
}

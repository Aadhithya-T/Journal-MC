package com.mcjournal.client;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlockSelectionRenderer {
    private ShaderProgram wireframeShader;
    private ShaderProgram destroyShader;

    private int lineVao;
    private int lineVbo;

    private int cubeVao;
    private int cubeVbo;

    // 10 authentic Minecraft destroy stages (0 to 9)
    private final int[] destroyTextures = new int[10];

    public void init() {
        this.wireframeShader = new ShaderProgram("/shaders/selection_vertex.glsl", "/shaders/selection_fragment.glsl");
        this.destroyShader = new ShaderProgram("/shaders/destroy_vertex.glsl", "/shaders/destroy_fragment.glsl");

        initLineMesh();
        initCubeMesh();
        generateDestroyTextures();
    }

    private void initLineMesh() {
        // 12 wireframe box edges with slight 0.002 padding
        float min = -0.001f;
        float max = 1.001f;

        float[] lineVertices = {
            // Bottom 4 edges
            min, min, min,  max, min, min,
            max, min, min,  max, min, max,
            max, min, max,  min, min, max,
            min, min, max,  min, min, min,

            // Top 4 edges
            min, max, min,  max, max, min,
            max, max, min,  max, max, max,
            max, max, max,  min, max, max,
            min, max, max,  min, max, min,

            // 4 vertical pillar edges
            min, min, min,  min, max, min,
            max, min, min,  max, max, min,
            max, min, max,  max, max, max,
            min, min, max,  min, max, max
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(lineVertices.length);
        vertexBuffer.put(lineVertices).flip();

        lineVao = glGenVertexArrays();
        lineVbo = glGenBuffers();

        glBindVertexArray(lineVao);
        glBindBuffer(GL_ARRAY_BUFFER, lineVbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    private void initCubeMesh() {
        // Cube mesh with exact texture UVs for destruction overlay
        float min = -0.002f;
        float max = 1.002f;

        float[] cubeVertices = {
            // Front face
            min, min, max, 0, 0,   max, min, max, 1, 0,   max, max, max, 1, 1,
            min, min, max, 0, 0,   max, max, max, 1, 1,   min, max, max, 0, 1,
            // Back face
            max, min, min, 0, 0,   min, min, min, 1, 0,   min, max, min, 1, 1,
            max, min, min, 0, 0,   min, max, min, 1, 1,   max, max, min, 0, 1,
            // Top face
            min, max, max, 0, 0,   max, max, max, 1, 0,   max, max, min, 1, 1,
            min, max, max, 0, 0,   max, max, min, 1, 1,   min, max, min, 0, 1,
            // Bottom face
            min, min, min, 0, 0,   max, min, min, 1, 0,   max, min, max, 1, 1,
            min, min, min, 0, 0,   max, min, max, 1, 1,   min, min, max, 0, 1,
            // Right face
            max, min, max, 0, 0,   max, min, min, 1, 0,   max, max, min, 1, 1,
            max, min, max, 0, 0,   max, max, min, 1, 1,   max, max, max, 0, 1,
            // Left face
            min, min, min, 0, 0,   min, min, max, 1, 0,   min, max, max, 1, 1,
            min, min, min, 0, 0,   min, max, max, 1, 1,   min, max, min, 0, 1
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(cubeVertices.length);
        vertexBuffer.put(cubeVertices).flip();

        cubeVao = glGenVertexArrays();
        cubeVbo = glGenBuffers();

        glBindVertexArray(cubeVao);
        glBindBuffer(GL_ARRAY_BUFFER, cubeVbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    /**
     * Mathematically generates all 10 authentic Minecraft destroy stages (0-9).
     * Creates progressive jagged hairline and deep fissure pixel cracks.
     */
    private void generateDestroyTextures() {
        int size = 16;
        Random rng = new Random(1337);

        // Predefined crack seed paths
        boolean[][] masterCrack = new boolean[size][size];
        int[][] lines = {
            {7, 8, 5, 5}, {5, 5, 3, 2}, {3, 2, 2, 0},
            {7, 8, 10, 6}, {10, 6, 13, 4}, {13, 4, 15, 3},
            {7, 8, 8, 11}, {8, 11, 6, 14}, {6, 14, 5, 15},
            {7, 8, 11, 10}, {11, 10, 14, 13}, {14, 13, 15, 15},
            {5, 5, 7, 3}, {10, 6, 9, 2}, {8, 11, 12, 13}, {11, 10, 8, 7},
            {3, 2, 0, 3}, {13, 4, 15, 8}, {6, 14, 2, 13}, {14, 13, 11, 15}
        };

        // Render master lines
        for (int[] seg : lines) {
            drawLine(masterCrack, seg[0], seg[1], seg[2], seg[3]);
        }

        for (int stage = 0; stage < 10; stage++) {
            ByteBuffer buf = MemoryUtil.memAlloc(size * size * 4);
            float threshold = (stage + 1) / 10.0f;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean isCrack = masterCrack[y][x];

                    // Crack expansion and secondary spallation
                    if (!isCrack && stage >= 3) {
                        int neighbors = countCrackNeighbors(masterCrack, x, y);
                        if (neighbors >= 2 && rng.nextFloat() < (stage * 0.10f)) {
                            isCrack = true;
                        }
                    }

                    // Proximity to center determines stage appearance
                    float distFromCenter = (float) Math.hypot(x - 7.5, y - 7.5) / 10.5f;

                    if (isCrack && (distFromCenter <= threshold || stage >= 7)) {
                        // Dark shattered fissure with slight edge highlight
                        byte r = (byte) 20;
                        byte g = (byte) 20;
                        byte b = (byte) 20;
                        byte a = (byte) 230;

                        buf.put(r).put(g).put(b).put(a);
                    } else {
                        // Transparent
                        buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                    }
                }
            }
            buf.flip();

            destroyTextures[stage] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, destroyTextures[stage]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            glBindTexture(GL_TEXTURE_2D, 0);

            MemoryUtil.memFree(buf);
        }
    }

    private void drawLine(boolean[][] grid, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < 16 && y0 >= 0 && y0 < 16) {
                grid[y0][x0] = true;
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private int countCrackNeighbors(boolean[][] grid, int x, int y) {
        int count = 0;
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && grid[ny][nx]) {
                count++;
            }
        }
        return count;
    }

    public void render(Camera camera, int bx, int by, int bz, float breakProgress) {
        // 1. Render Authentic Minecraft 10-Stage Destruction Overlay
        if (breakProgress > 0.001f) {
            int stage = Math.clamp((int) Math.floor(breakProgress * 10.0f), 0, 9);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);

            destroyShader.bind();
            destroyShader.setUniform("uProjection", camera.getProjectionMatrix());
            destroyShader.setUniform("uView", camera.getViewMatrix());
            destroyShader.setUniform("uBlockPos", new Vector3f(bx, by, bz));

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, destroyTextures[stage]);
            destroyShader.setUniform("uDestroyTex", 0);

            glBindVertexArray(cubeVao);
            glDrawArrays(GL_TRIANGLES, 0, 36);
            glBindVertexArray(0);

            glBindTexture(GL_TEXTURE_2D, 0);
            destroyShader.unbind();
        }

        // 2. Render Black Wireframe Bounding Box Outline (Vanilla Minecraft)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glLineWidth(2.5f);

        wireframeShader.bind();
        wireframeShader.setUniform("uProjection", camera.getProjectionMatrix());
        wireframeShader.setUniform("uView", camera.getViewMatrix());
        wireframeShader.setUniform("uBlockPos", new Vector3f(bx, by, bz));
        wireframeShader.setUniform("uColor", new Vector4f(0.0f, 0.0f, 0.0f, 0.65f));

        glBindVertexArray(lineVao);
        glDrawArrays(GL_LINES, 0, 24);
        glBindVertexArray(0);

        wireframeShader.unbind();
    }

    public void cleanup() {
        if (wireframeShader != null) wireframeShader.cleanup();
        if (destroyShader != null) destroyShader.cleanup();
        if (lineVbo != 0) glDeleteBuffers(lineVbo);
        if (lineVao != 0) glDeleteVertexArrays(lineVao);
        if (cubeVbo != 0) glDeleteBuffers(cubeVbo);
        if (cubeVao != 0) glDeleteVertexArrays(cubeVao);

        for (int i = 0; i < 10; i++) {
            if (destroyTextures[i] != 0) {
                glDeleteTextures(destroyTextures[i]);
            }
        }
    }
}

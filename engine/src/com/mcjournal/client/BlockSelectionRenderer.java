package com.mcjournal.client;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlockSelectionRenderer {
    private ShaderProgram selectionShader;
    private int lineVao;
    private int lineVbo;

    private int cubeVao;
    private int cubeVbo;

    public void init() {
        this.selectionShader = new ShaderProgram("/shaders/selection_vertex.glsl", "/shaders/selection_fragment.glsl");
        initLineMesh();
        initCubeMesh();
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
        // Full cube for destruction overlay
        float min = -0.002f;
        float max = 1.002f;

        float[] cubeVertices = {
            // Front face
            min, min, max,  max, min, max,  max, max, max,
            min, min, max,  max, max, max,  min, max, max,
            // Back face
            max, min, min,  min, min, min,  min, max, min,
            max, min, min,  min, max, min,  max, max, min,
            // Top face
            min, max, max,  max, max, max,  max, max, min,
            min, max, max,  max, max, min,  min, max, min,
            // Bottom face
            min, min, min,  max, min, min,  max, min, max,
            min, min, min,  max, min, max,  min, min, max,
            // Right face
            max, min, max,  max, min, min,  max, max, min,
            max, min, max,  max, max, min,  max, max, max,
            // Left face
            min, min, min,  min, min, max,  min, max, max,
            min, min, min,  min, max, max,  min, max, min
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(cubeVertices.length);
        vertexBuffer.put(cubeVertices).flip();

        cubeVao = glGenVertexArrays();
        cubeVbo = glGenBuffers();

        glBindVertexArray(cubeVao);
        glBindBuffer(GL_ARRAY_BUFFER, cubeVbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    public void render(Camera camera, int bx, int by, int bz, float breakProgress) {
        selectionShader.bind();
        selectionShader.setUniform("uProjection", camera.getProjectionMatrix());
        selectionShader.setUniform("uView", camera.getViewMatrix());
        selectionShader.setUniform("uBlockPos", new Vector3f(bx, by, bz));

        // 1. Render Black Wireframe Bounding Box Outline (Vanilla Minecraft)
        glLineWidth(2.5f);
        selectionShader.setUniform("uColor", new Vector4f(0.0f, 0.0f, 0.0f, 0.65f));
        glBindVertexArray(lineVao);
        glDrawArrays(GL_LINES, 0, 24);
        glBindVertexArray(0);

        // 2. Render Destruction Damage Overlay
        if (breakProgress > 0.01f) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // Red/Dark cracking overlay pulsing with progress
            float alpha = Math.min(0.65f, breakProgress * 0.7f);
            selectionShader.setUniform("uColor", new Vector4f(0.85f, 0.2f, 0.1f, alpha));

            glBindVertexArray(cubeVao);
            glDrawArrays(GL_TRIANGLES, 0, 36);
            glBindVertexArray(0);
        }

        selectionShader.unbind();
    }

    public void cleanup() {
        if (selectionShader != null) selectionShader.cleanup();
        if (lineVbo != 0) glDeleteBuffers(lineVbo);
        if (lineVao != 0) glDeleteVertexArrays(lineVao);
        if (cubeVbo != 0) glDeleteBuffers(cubeVbo);
        if (cubeVao != 0) glDeleteVertexArrays(cubeVao);
    }
}

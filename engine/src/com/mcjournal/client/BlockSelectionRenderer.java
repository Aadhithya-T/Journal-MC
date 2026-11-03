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
    private ShaderProgram wireframeShader;

    private int lineVao;
    private int lineVbo;

    public void init() {
        this.wireframeShader = new ShaderProgram("/shaders/selection_vertex.glsl", "/shaders/selection_fragment.glsl");
        initLineMesh();
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

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    public void render(Camera camera, int bx, int by, int bz) {
        // Render Black Wireframe Bounding Box Outline (Vanilla Minecraft)
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
        if (lineVbo != 0) glDeleteBuffers(lineVbo);
        if (lineVao != 0) glDeleteVertexArrays(lineVao);
    }
}

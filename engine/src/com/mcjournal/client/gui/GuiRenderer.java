package com.mcjournal.client.gui;

import com.mcjournal.client.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class GuiRenderer {
    private final ShaderProgram guiShader;
    private final Matrix4f orthoMatrix = new Matrix4f();
    private int vao;
    private int vbo;
    private final FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(6 * 8); // 6 vertices * 8 floats (x, y, u, v, r, g, b, a)

    public GuiRenderer() {
        this.guiShader = new ShaderProgram("/shaders/gui_vertex.glsl", "/shaders/gui_fragment.glsl");
        initGL();
    }

    private void initGL() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // 8 floats per vertex: vec2 pos (offset 0), vec2 uv (offset 8), vec4 color (offset 16)
        int stride = 8 * Float.BYTES;

        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void begin(int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        orthoMatrix.identity();
        orthoMatrix.ortho2D(0, screenWidth, screenHeight, 0); // (0,0) at Top-Left

        guiShader.bind();
        guiShader.setUniform("uOrtho", orthoMatrix);
    }

    public void end() {
        guiShader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        guiShader.setUniform("uUseTexture", 0);

        vertexBuffer.clear();
        putVertex(x, y, 0, 0, r, g, b, a);
        putVertex(x + w, y, 0, 0, r, g, b, a);
        putVertex(x + w, y + h, 0, 0, r, g, b, a);

        putVertex(x, y, 0, 0, r, g, b, a);
        putVertex(x + w, y + h, 0, 0, r, g, b, a);
        putVertex(x, y + h, 0, 0, r, g, b, a);
        vertexBuffer.flip();

        drawBuffer(0);
    }

    public void drawBevelBox(float x, float y, float w, float h, int bgHex, int borderLightHex, int borderDarkHex) {
        float bgR = ((bgHex >> 16) & 0xFF) / 255f, bgG = ((bgHex >> 8) & 0xFF) / 255f, bgB = (bgHex & 0xFF) / 255f;
        float lR = ((borderLightHex >> 16) & 0xFF) / 255f, lG = ((borderLightHex >> 8) & 0xFF) / 255f, lB = (borderLightHex & 0xFF) / 255f;
        float dR = ((borderDarkHex >> 16) & 0xFF) / 255f, dG = ((borderDarkHex >> 8) & 0xFF) / 255f, dB = (borderDarkHex & 0xFF) / 255f;

        // Background
        drawRect(x, y, w, h, bgR, bgG, bgB, 0.92f);

        // Top & Left Light Border (2px)
        drawRect(x, y, w, 2, lR, lG, lB, 1.0f);
        drawRect(x, y, 2, h, lR, lG, lB, 1.0f);

        // Bottom & Right Dark Border (2px)
        drawRect(x, y + h - 2, w, 2, dR, dG, dB, 1.0f);
        drawRect(x + w - 2, y, 2, h, dR, dG, dB, 1.0f);
    }

    public void drawTexturedQuad(int textureId, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        guiShader.setUniform("uUseTexture", 1);
        guiShader.setUniform("uTexture", 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);

        vertexBuffer.clear();
        putVertex(x, y, u0, v0, r, g, b, a);
        putVertex(x + w, y, u1, v0, r, g, b, a);
        putVertex(x + w, y + h, u1, v1, r, g, b, a);

        putVertex(x, y, u0, v0, r, g, b, a);
        putVertex(x + w, y + h, u1, v1, r, g, b, a);
        putVertex(x, y + h, u0, v1, r, g, b, a);
        vertexBuffer.flip();

        drawBuffer(textureId);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void putVertex(float x, float y, float u, float v, float r, float g, float b, float a) {
        vertexBuffer.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    private void drawBuffer(int textureId) {
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (guiShader != null) guiShader.cleanup();
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        MemoryUtil.memFree(vertexBuffer);
    }
}

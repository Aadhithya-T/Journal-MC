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
    private final FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(6 * 8);

    public GuiRenderer() {
        this.guiShader = new ShaderProgram("/shaders/gui_vertex.glsl", "/shaders/gui_fragment.glsl");
        initGL();
    }

    private void initGL() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

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
        orthoMatrix.ortho2D(0, screenWidth, screenHeight, 0);

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

        drawBuffer();
    }

    public void drawMinecraftButton(float x, float y, float w, float h, boolean isHovered, boolean isEnabled) {
        int outerTopLeft, outerBottomRight;
        int innerTopLeft, innerBottomRight;
        int bodyColor;

        if (!isEnabled) {
            outerTopLeft = 0x5c5c5c;
            outerBottomRight = 0x1a1a1a;
            innerTopLeft = 0x363636;
            innerBottomRight = 0x484848;
            bodyColor = 0x303030;
        } else if (isHovered) {
            // Vanilla Hover: Brilliant White/Blue Highlight and lighter body
            outerTopLeft = 0xffffff;
            outerBottomRight = 0x22223a;
            innerTopLeft = 0x6e6e8c;
            innerBottomRight = 0x9fa0cc;
            bodyColor = 0x767698;
        } else {
            // Vanilla Normal Stone Button
            outerTopLeft = 0xd4d4d4;
            outerBottomRight = 0x262626;
            innerTopLeft = 0x4f4f4f;
            innerBottomRight = 0x8e8e8e;
            bodyColor = 0x666666;
        }

        // 1. Center Body
        drawHexRect(x + 2, y + 2, w - 4, h - 4, bodyColor, 1.0f);

        // 2. Outer Highlight (Top & Left - 2px)
        drawHexRect(x, y, w - 2, 2, outerTopLeft, 1.0f);
        drawHexRect(x, y, 2, h - 2, outerTopLeft, 1.0f);

        // 3. Outer Shadow (Bottom & Right - 2px)
        drawHexRect(x, y + h - 2, w, 2, outerBottomRight, 1.0f);
        drawHexRect(x + w - 2, y, 2, h, outerBottomRight, 1.0f);

        // 4. Inner Ridge Inset (1px)
        drawHexRect(x + 2, y + 2, w - 4, 1, innerTopLeft, 1.0f);
        drawHexRect(x + 2, y + 2, 1, h - 4, innerTopLeft, 1.0f);
        drawHexRect(x + 2, y + h - 3, w - 4, 1, innerBottomRight, 1.0f);
        drawHexRect(x + w - 3, y + 2, 1, h - 4, innerBottomRight, 1.0f);
    }

    public void drawMenuHeaderFooterStrips(int screenWidth, int screenHeight, int headerHeight, int footerHeight) {
        // Dark translucent gradient strips for header and footer (Vanilla Minecraft layout)
        drawRect(0, 0, screenWidth, headerHeight, 0.08f, 0.08f, 0.10f, 0.85f);
        drawRect(0, headerHeight - 2, screenWidth, 2, 0.0f, 0.0f, 0.0f, 1.0f);

        drawRect(0, screenHeight - footerHeight, screenWidth, footerHeight, 0.08f, 0.08f, 0.10f, 0.85f);
        drawRect(0, screenHeight - footerHeight, screenWidth, 2, 0.0f, 0.0f, 0.0f, 1.0f);
    }

    public void drawBevelBox(float x, float y, float w, float h, int bgHex, int borderLightHex, int borderDarkHex) {
        float bgR = ((bgHex >> 16) & 0xFF) / 255f, bgG = ((bgHex >> 8) & 0xFF) / 255f, bgB = (bgHex & 0xFF) / 255f;
        float lR = ((borderLightHex >> 16) & 0xFF) / 255f, lG = ((borderLightHex >> 8) & 0xFF) / 255f, lB = (borderLightHex & 0xFF) / 255f;
        float dR = ((borderDarkHex >> 16) & 0xFF) / 255f, dG = ((borderDarkHex >> 8) & 0xFF) / 255f, dB = (borderDarkHex & 0xFF) / 255f;

        drawRect(x, y, w, h, bgR, bgG, bgB, 0.90f);

        drawRect(x, y, w, 2, lR, lG, lB, 1.0f);
        drawRect(x, y, 2, h, lR, lG, lB, 1.0f);

        drawRect(x, y + h - 2, w, 2, dR, dG, dB, 1.0f);
        drawRect(x + w - 2, y, 2, h, dR, dG, dB, 1.0f);
    }

    public void drawHexRect(float x, float y, float w, float h, int hexColor, float alpha) {
        float r = ((hexColor >> 16) & 0xFF) / 255f;
        float g = ((hexColor >> 8) & 0xFF) / 255f;
        float b = (hexColor & 0xFF) / 255f;
        drawRect(x, y, w, h, r, g, b, alpha);
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

        drawBuffer();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void drawTexturedQuadArbitrary(int textureId,
                                          float x0, float y0, float u0, float v0,
                                          float x1, float y1, float u1, float v1,
                                          float x2, float y2, float u2, float v2,
                                          float x3, float y3, float u3, float v3,
                                          float r, float g, float b, float a) {
        guiShader.setUniform("uUseTexture", 1);
        guiShader.setUniform("uTexture", 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);

        vertexBuffer.clear();
        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x1, y1, u1, v1, r, g, b, a);
        putVertex(x2, y2, u2, v2, r, g, b, a);

        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x2, y2, u2, v2, r, g, b, a);
        putVertex(x3, y3, u3, v3, r, g, b, a);
        vertexBuffer.flip();

        drawBuffer();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void putVertex(float x, float y, float u, float v, float r, float g, float b, float a) {
        vertexBuffer.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    private void drawBuffer() {
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

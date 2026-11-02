package com.mcjournal.client.gui;

import com.mcjournal.client.ShaderProgram;
import com.mcjournal.client.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * High-performance batched 2D GUI renderer with robust OpenGL state preservation,
 * dynamic quad buffering, Painter's-order consistency, and 1x1 white texture fallback.
 */
public class GuiRenderer {
    public static final int MAX_QUADS = 2048;
    public static final int VERTICES_PER_QUAD = 6;
    public static final int FLOATS_PER_VERTEX = 8; // (x, y, u, v, r, g, b, a)
    public static final int STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;
    public static final int BUFFER_CAPACITY_FLOATS = MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX;

    private final ShaderProgram guiShader;
    private final Matrix4f orthoMatrix = new Matrix4f();
    private final FloatBuffer vertexBuffer;

    private int vao = 0;
    private int vbo = 0;
    private int whiteTextureId = 0;

    // Batching state
    private boolean isDrawing = false;
    private boolean isCleanedUp = false;
    private int currentTextureId = -1;
    private int currentUseTexture = -1; // 0 = solid untextured, 1 = textured
    private int quadCount = 0;

    // Telemetry & metrics
    private int drawCallCount = 0;
    private int totalQuadCount = 0;

    // Saved OpenGL state for strict restoration
    private final int[] prevViewport = new int[4];
    private boolean prevDepthTest;
    private boolean prevCullFace;
    private boolean prevBlend;
    private boolean prevDepthMask;
    private int prevBlendSrcRgb;
    private int prevBlendDstRgb;
    private int prevBlendSrcAlpha;
    private int prevBlendDstAlpha;
    private int prevActiveTexture;
    private int prevTextureBinding;
    private int prevVao;
    private int prevVbo;
    private int prevProgram;

    public GuiRenderer() {
        this.guiShader = new ShaderProgram("/shaders/gui_vertex.glsl", "/shaders/gui_fragment.glsl");
        this.vertexBuffer = MemoryUtil.memAllocFloat(BUFFER_CAPACITY_FLOATS);
        initGL();
    }

    private void initGL() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Pre-allocate VBO on GPU to avoid per-flush reallocation
        glBufferData(GL_ARRAY_BUFFER, (long) BUFFER_CAPACITY_FLOATS * Float.BYTES, GL_DYNAMIC_DRAW);

        // Vertex layout: Location 0: vec2 aPos (8 floats stride, offset 0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, STRIDE_BYTES, 0);
        glEnableVertexAttribArray(0);

        // Vertex layout: Location 1: vec2 aUV (8 floats stride, offset 2 floats)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE_BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Vertex layout: Location 2: vec4 aColor (8 floats stride, offset 4 floats)
        glVertexAttribPointer(2, 4, GL_FLOAT, false, STRIDE_BYTES, 4L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Generate 1x1 solid white texture (RGBA: 255, 255, 255, 255)
        initWhiteTexture();
    }

    private void initWhiteTexture() {
        whiteTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, whiteTextureId);

        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        pixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF);
        pixel.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        MemoryUtil.memFree(pixel);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void begin(int screenWidth, int screenHeight) {
        begin(screenWidth, screenHeight, 0, 0, screenWidth, screenHeight);
    }

    public void begin(int logicalWidth, int logicalHeight, int viewportX, int viewportY, int viewportW, int viewportH) {
        if (isDrawing) {
            // Safe recovery if previous begin was not properly closed
            end();
        }

        // 1. Snapshot previous OpenGL state
        glGetIntegerv(GL_VIEWPORT, prevViewport);
        prevDepthTest = glIsEnabled(GL_DEPTH_TEST);
        prevCullFace = glIsEnabled(GL_CULL_FACE);
        prevBlend = glIsEnabled(GL_BLEND);
        prevDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
        prevBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
        prevBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
        prevBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        prevBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        prevActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        prevTextureBinding = glGetInteger(GL_TEXTURE_BINDING_2D);
        prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        prevVbo = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        prevProgram = glGetInteger(GL_CURRENT_PROGRAM);

        // 2. Set up deterministic 2D GUI pipeline state
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        // Configure viewport in OpenGL coordinates (Y=0 at bottom)
        int fbH = (prevViewport[3] > 0) ? prevViewport[3] : (viewportY + viewportH);
        int glY = fbH - (viewportY + viewportH);
        glViewport(viewportX, glY, viewportW, viewportH);

        // 3. Configure Orthographic Projection Matrix
        orthoMatrix.identity();
        orthoMatrix.ortho2D(0, logicalWidth, logicalHeight, 0);

        guiShader.bind();
        guiShader.setUniform("uOrtho", orthoMatrix);
        guiShader.setUniform("uTexture", 0);

        // 4. Reset batch trackers
        isDrawing = true;
        currentTextureId = -1;
        currentUseTexture = -1;
        quadCount = 0;
        vertexBuffer.clear();
    }

    public void flush() {
        if (quadCount == 0) return;

        // Apply shader uniform for texture usage
        guiShader.setUniform("uUseTexture", currentUseTexture);

        if (currentUseTexture == 1) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, currentTextureId != 0 ? currentTextureId : whiteTextureId);
        }

        // Transfer buffered vertex attributes to GPU
        vertexBuffer.flip();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glDrawArrays(GL_TRIANGLES, 0, quadCount * VERTICES_PER_QUAD);

        drawCallCount++;
        totalQuadCount += quadCount;

        // Reset buffer cursor for subsequent quads in this pass
        vertexBuffer.clear();
        quadCount = 0;
    }

    public void end() {
        if (!isDrawing) return;

        try {
            flush();
        } finally {
            // Restore previous bindings and shader
            glActiveTexture(prevActiveTexture);
            glBindTexture(GL_TEXTURE_2D, prevTextureBinding);
            glBindBuffer(GL_ARRAY_BUFFER, prevVbo);
            glBindVertexArray(prevVao);
            glUseProgram(prevProgram);

            // Restore previous OpenGL rasterizer and depth/blend states
            if (prevDepthTest) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
            if (prevCullFace) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
            if (prevBlend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
            glDepthMask(prevDepthMask);
            glBlendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
            if (prevViewport[2] > 0 && prevViewport[3] > 0) {
                glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            }

            isDrawing = false;
            currentTextureId = -1;
            currentUseTexture = -1;
        }
    }

    /**
     * Toggles inverted color blending mode (GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR).
     * Used for HUD crosshairs and high-contrast reticles that must invert cleanly
     * over both blindingly bright backgrounds (sky, snow) and deep shadows/caves.
     */
    public void setInvertedBlend(boolean inverted) {
        if (!isDrawing) return;
        flush();
        if (inverted) {
            glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);
        } else {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    public void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (!isDrawing) return;

        // Solid untextured rectangle
        if (currentUseTexture != 0) {
            flush();
            currentUseTexture = 0;
            currentTextureId = 0;
        }

        if (quadCount >= MAX_QUADS) {
            flush();
        }

        putVertex(x, y, 0f, 0f, r, g, b, a);
        putVertex(x + w, y, 1f, 0f, r, g, b, a);
        putVertex(x + w, y + h, 1f, 1f, r, g, b, a);

        putVertex(x, y, 0f, 0f, r, g, b, a);
        putVertex(x + w, y + h, 1f, 1f, r, g, b, a);
        putVertex(x, y + h, 0f, 1f, r, g, b, a);

        quadCount++;
    }

    public void drawTexturedQuad(int textureId, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        if (!isDrawing) return;

        int effectiveTex = (textureId != 0) ? textureId : whiteTextureId;

        if (currentUseTexture != 1 || currentTextureId != effectiveTex) {
            flush();
            currentUseTexture = 1;
            currentTextureId = effectiveTex;
        }

        if (quadCount >= MAX_QUADS) {
            flush();
        }

        putVertex(x, y, u0, v0, r, g, b, a);
        putVertex(x + w, y, u1, v0, r, g, b, a);
        putVertex(x + w, y + h, u1, v1, r, g, b, a);

        putVertex(x, y, u0, v0, r, g, b, a);
        putVertex(x + w, y + h, u1, v1, r, g, b, a);
        putVertex(x, y + h, u0, v1, r, g, b, a);

        quadCount++;
    }

    public void drawTexturedQuadArbitrary(int textureId,
                                          float x0, float y0, float u0, float v0,
                                          float x1, float y1, float u1, float v1,
                                          float x2, float y2, float u2, float v2,
                                          float x3, float y3, float u3, float v3,
                                          float r, float g, float b, float a) {
        if (!isDrawing) return;

        int effectiveTex = (textureId != 0) ? textureId : whiteTextureId;

        if (currentUseTexture != 1 || currentTextureId != effectiveTex) {
            flush();
            currentUseTexture = 1;
            currentTextureId = effectiveTex;
        }

        if (quadCount >= MAX_QUADS) {
            flush();
        }

        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x1, y1, u1, v1, r, g, b, a);
        putVertex(x2, y2, u2, v2, r, g, b, a);

        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x2, y2, u2, v2, r, g, b, a);
        putVertex(x3, y3, u3, v3, r, g, b, a);

        quadCount++;
    }

    private void putVertex(float x, float y, float u, float v, float r, float g, float b, float a) {
        vertexBuffer.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    public void drawHexRect(float x, float y, float w, float h, int hexColor, float alpha) {
        float r = ((hexColor >> 16) & 0xFF) / 255f;
        float g = ((hexColor >> 8) & 0xFF) / 255f;
        float b = (hexColor & 0xFF) / 255f;
        drawRect(x, y, w, h, r, g, b, alpha);
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

    public void drawDirtBackground(int textureId, int screenWidth, int screenHeight) {
        float[] uv = TextureAtlas.getTileUV(2); // BlockType DIRT = 2
        int tileSize = 64;
        for (int y = 0; y < screenHeight; y += tileSize) {
            int h = Math.min(tileSize, screenHeight - y);
            float vFraction = (float) h / (float) tileSize;
            float vEnd = uv[3] + (uv[2] - uv[3]) * vFraction;
            for (int x = 0; x < screenWidth; x += tileSize) {
                int w = Math.min(tileSize, screenWidth - x);
                float uFraction = (float) w / (float) tileSize;
                float uEnd = uv[0] + (uv[1] - uv[0]) * uFraction;
                drawTexturedQuad(textureId, x, y, w, h, uv[0], uv[3], uEnd, vEnd, 0.28f, 0.28f, 0.28f, 1.0f);
            }
        }
    }

    public boolean isDrawing() {
        return isDrawing;
    }

    public int getWhiteTextureId() {
        return whiteTextureId;
    }

    public int getDrawCallCount() {
        return drawCallCount;
    }

    public int getTotalQuadCount() {
        return totalQuadCount;
    }

    public void resetMetrics() {
        drawCallCount = 0;
        totalQuadCount = 0;
    }

    public void cleanup() {
        if (isCleanedUp) return;
        isCleanedUp = true;

        if (isDrawing) {
            end();
        }
        if (guiShader != null) guiShader.cleanup();
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (whiteTextureId != 0) {
            glDeleteTextures(whiteTextureId);
            whiteTextureId = 0;
        }
        MemoryUtil.memFree(vertexBuffer);
    }
}

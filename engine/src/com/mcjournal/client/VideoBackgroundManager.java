package com.mcjournal.client;

import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class VideoBackgroundManager {
    public static final String[] THEMES = {
        "Aurora Night",
        "Coral Reef",
        "Cozy Campfire"
    };

    private final int sessionThemeIndex;
    private ShaderProgram videoShader;
    private int vao;
    private int vbo;

    public VideoBackgroundManager() {
        // Randomly select one of the 3 themes for this entire session
        this.sessionThemeIndex = new Random().nextInt(3);
        System.out.println("[VideoBackground] 🎬 Selected Session Video Background: " + THEMES[sessionThemeIndex] + " (Randomly selected, fixed for session, loops infinitely)");
    }

    public void init() {
        this.videoShader = new ShaderProgram("/shaders/video_background_vertex.glsl", "/shaders/video_background_fragment.glsl");
        initQuad();
    }

    private void initQuad() {
        // Full screen quad (-1..1)
        float[] quadVertices = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
             1.0f, -1.0f,

            -1.0f,  1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(quadVertices.length);
        vertexBuffer.put(quadVertices).flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    public void render(int width, int height, float time) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        videoShader.bind();
        videoShader.setUniform("uTime", time);
        videoShader.setUniform("uResolution", new Vector2f(width, height));
        videoShader.setUniform("uTheme", sessionThemeIndex);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        videoShader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public String getCurrentThemeName() {
        return THEMES[sessionThemeIndex];
    }

    public int getSessionThemeIndex() {
        return sessionThemeIndex;
    }

    public void cleanup() {
        if (videoShader != null) videoShader.cleanup();
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
    }
}

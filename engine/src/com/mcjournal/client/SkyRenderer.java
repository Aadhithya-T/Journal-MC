package com.mcjournal.client;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SkyRenderer {
    private int skyVao = 0;
    private int skyVbo = 0;
    private ShaderProgram skyShader;

    // Cube vertices for sky box dome
    private static final float[] SKY_BOX_VERTICES = {
        -1.0f,  1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,

        -1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,

         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,

        -1.0f, -1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,

        -1.0f,  1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f, -1.0f,

        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f,  1.0f
    };

    public void init() {
        skyShader = new ShaderProgram("/shaders/sky_vertex.glsl", "/shaders/sky_fragment.glsl");

        skyVao = glGenVertexArrays();
        glBindVertexArray(skyVao);

        skyVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, skyVbo);
        FloatBuffer buf = MemoryUtil.memAllocFloat(SKY_BOX_VERTICES.length);
        buf.put(SKY_BOX_VERTICES).flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        MemoryUtil.memFree(buf);

        glBindVertexArray(0);
    }

    public void render(Camera camera, Vector3f sunDir, Vector3f zenithColor, Vector3f horizonColor, Vector3f sunColor, float timeOfDay) {
        if (skyVao == 0 || skyShader == null) return;

        // Render behind everything with depth func LEQUAL
        glDepthFunc(GL_LEQUAL);
        glDepthMask(false);

        skyShader.bind();
        skyShader.setUniform("uProjection", camera.getProjectionMatrix());
        skyShader.setUniform("uView", camera.getViewMatrix());
        skyShader.setUniform("uSunDir", sunDir);
        skyShader.setUniform("uZenithColor", zenithColor);
        skyShader.setUniform("uHorizonColor", horizonColor);
        skyShader.setUniform("uSunColor", sunColor);
        skyShader.setUniform("uTimeOfDay", timeOfDay);

        glBindVertexArray(skyVao);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);

        skyShader.unbind();

        glDepthMask(true);
        glDepthFunc(GL_LESS);
    }

    public void cleanup() {
        if (skyShader != null) skyShader.cleanup();
        if (skyVbo != 0) glDeleteBuffers(skyVbo);
        if (skyVao != 0) glDeleteVertexArrays(skyVao);
        skyVao = skyVbo = 0;
    }
}

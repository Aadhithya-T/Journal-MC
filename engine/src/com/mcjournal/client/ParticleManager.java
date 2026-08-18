package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ParticleManager {
    public static class Particle {
        public Vector3f pos = new Vector3f();
        public Vector3f velocity = new Vector3f();
        public Vector4f color = new Vector4f();
        public float scale;
        public float lifetime;
        public float maxLifetime;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private ShaderProgram shader;
    private int vao;
    private int vbo;

    public void init() {
        this.shader = new ShaderProgram("/shaders/particle_vertex.glsl", "/shaders/particle_fragment.glsl");
        initCube();
    }

    private void initCube() {
        // Small 3D unit cube centered at origin [-0.5..0.5]
        float min = -0.5f;
        float max = 0.5f;

        float[] vertices = {
            // Front
            min, min, max, 0, 0,  max, min, max, 1, 0,  max, max, max, 1, 1,
            min, min, max, 0, 0,  max, max, max, 1, 1,  min, max, max, 0, 1,
            // Back
            max, min, min, 0, 0,  min, min, min, 1, 0,  min, max, min, 1, 1,
            max, min, min, 0, 0,  min, max, min, 1, 1,  max, max, min, 0, 1,
            // Top
            min, max, max, 0, 0,  max, max, max, 1, 0,  max, max, min, 1, 1,
            min, max, max, 0, 0,  max, max, min, 1, 1,  min, max, min, 0, 1,
            // Bottom
            min, min, min, 0, 0,  max, min, min, 1, 0,  max, min, max, 1, 1,
            min, min, min, 0, 0,  max, min, max, 1, 1,  min, min, max, 0, 1,
            // Right
            max, min, max, 0, 0,  max, min, min, 1, 0,  max, max, min, 1, 1,
            max, min, max, 0, 0,  max, max, min, 1, 1,  max, max, max, 0, 1,
            // Left
            min, min, min, 0, 0,  min, min, max, 1, 0,  min, max, max, 1, 1,
            min, min, min, 0, 0,  min, max, max, 1, 1,  min, max, min, 0, 1
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    public void spawnBlockBreakParticles(int bx, int by, int bz, byte blockType) {
        Vector4f baseColor = parseHexColor(Block.getColor(blockType));
        int count = 28; // Disintegration cloud of ~28 debris fragments

        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            float rx = bx + 0.1f + random.nextFloat() * 0.8f;
            float ry = by + 0.1f + random.nextFloat() * 0.8f;
            float rz = bz + 0.1f + random.nextFloat() * 0.8f;
            p.pos.set(rx, ry, rz);

            // Explode outward in random directions
            float vx = (random.nextFloat() - 0.5f) * 3.5f;
            float vy = 1.5f + random.nextFloat() * 3.0f;
            float vz = (random.nextFloat() - 0.5f) * 3.5f;
            p.velocity.set(vx, vy, vz);

            // Particle shading variance
            float shade = 0.75f + random.nextFloat() * 0.35f;
            p.color.set(baseColor.x * shade, baseColor.y * shade, baseColor.z * shade, 1.0f);

            p.scale = 0.07f + random.nextFloat() * 0.07f;
            p.maxLifetime = 0.6f + random.nextFloat() * 0.6f;
            p.lifetime = p.maxLifetime;

            particles.add(p);
        }
    }

    public void spawnMiningHitParticles(int bx, int by, int bz, byte blockType, int normalX, int normalY, int normalZ) {
        Vector4f baseColor = parseHexColor(Block.getColor(blockType));
        int count = 3;

        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            float rx = bx + 0.5f + normalX * 0.52f + (random.nextFloat() - 0.5f) * 0.4f;
            float ry = by + 0.5f + normalY * 0.52f + (random.nextFloat() - 0.5f) * 0.4f;
            float rz = bz + 0.5f + normalZ * 0.52f + (random.nextFloat() - 0.5f) * 0.4f;
            p.pos.set(rx, ry, rz);

            float vx = normalX * 1.5f + (random.nextFloat() - 0.5f) * 1.5f;
            float vy = Math.max(0.5f, normalY * 1.5f + random.nextFloat() * 2.0f);
            float vz = normalZ * 1.5f + (random.nextFloat() - 0.5f) * 1.5f;
            p.velocity.set(vx, vy, vz);

            float shade = 0.8f + random.nextFloat() * 0.3f;
            p.color.set(baseColor.x * shade, baseColor.y * shade, baseColor.z * shade, 1.0f);

            p.scale = 0.05f + random.nextFloat() * 0.04f;
            p.maxLifetime = 0.3f + random.nextFloat() * 0.3f;
            p.lifetime = p.maxLifetime;

            particles.add(p);
        }
    }

    public void update(double deltaTime, ChunkManager world) {
        float dt = (float) deltaTime;
        float gravity = -18.0f; // Blocks fall naturally

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.lifetime -= dt;
            if (p.lifetime <= 0) {
                it.remove();
                continue;
            }

            p.velocity.y += gravity * dt;
            p.velocity.x *= 0.96f;
            p.velocity.z *= 0.96f;

            p.pos.x += p.velocity.x * dt;
            p.pos.y += p.velocity.y * dt;
            p.pos.z += p.velocity.z * dt;

            // Simple floor collision so debris bounces on solid ground
            int floorX = (int) Math.floor(p.pos.x);
            int floorY = (int) Math.floor(p.pos.y);
            int floorZ = (int) Math.floor(p.pos.z);

            if (world != null && Block.isSolid(world.getBlockAt(floorX, floorY, floorZ))) {
                p.pos.y = floorY + 1.01f;
                p.velocity.y = -p.velocity.y * 0.3f; // bounce damping
                p.velocity.x *= 0.6f;
                p.velocity.z *= 0.6f;
            }
        }
    }

    public void render(Camera camera) {
        if (particles.isEmpty()) return;

        shader.bind();
        shader.setUniform("uProjection", camera.getProjectionMatrix());
        shader.setUniform("uView", camera.getViewMatrix());

        glBindVertexArray(vao);

        for (Particle p : particles) {
            float alpha = Math.min(1.0f, p.lifetime / (p.maxLifetime * 0.4f));
            Vector4f col = new Vector4f(p.color.x, p.color.y, p.color.z, alpha);

            shader.setUniform("uParticlePos", p.pos);
            shader.setUniform("uParticleScale", p.scale);
            shader.setUniform("uParticleColor", col);

            glDrawArrays(GL_TRIANGLES, 0, 36);
        }

        glBindVertexArray(0);
        shader.unbind();
    }

    private Vector4f parseHexColor(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
        } catch (Exception e) {
            return new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
        }
    }

    public void cleanup() {
        if (shader != null) shader.cleanup();
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        particles.clear();
    }
}

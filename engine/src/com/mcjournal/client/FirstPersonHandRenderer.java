package com.mcjournal.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class FirstPersonHandRenderer {
    private static final int TEX_SIZE = 32;

    private ShaderProgram handShader;
    private int textureId;
    private int vao;
    private int vbo;
    private int vertexCount = 0;

    private final Matrix4f viewmodelProjection = new Matrix4f();
    private final Matrix4f viewmodelView = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();

    // Animation states
    private float swingProgress = 0.0f;       // 0.0 to 1.0
    private boolean isSwinging = false;
    private boolean isMining = false;         // Continuous block breaking
    private float miningTimer = 0.0f;
    private float bobTimer = 0.0f;            // Walk view-bobbing
    private float bobAmount = 0.0f;
    private float idleTime = 0.0f;

    public void init() {
        handShader = new ShaderProgram("/shaders/hand_vertex.glsl", "/shaders/hand_fragment.glsl");
        generateSteveSkinTexture();
        buildSteveArmMesh();
    }

    /**
     * Generates an authentic Minecraft skin texture for Steve's Arm
     * featuring a crisp white shirt sleeve and tanned Steve skin.
     */
    private void generateSteveSkinTexture() {
        ByteBuffer buffer = MemoryUtil.memAlloc(TEX_SIZE * TEX_SIZE * 4);
        int[] pixels = new int[TEX_SIZE * TEX_SIZE];

        // Color definitions (ARGB)
        // White Shirt Sleeve Colors
        int w0 = 0xFFFFFFFF; // Pure white
        int w1 = 0xFFF2F2F2; // High white
        int w2 = 0xFFE5E5E5; // Mid white
        int w3 = 0xFFD8D8D8; // Soft shaded white
        int w4 = 0xFFCBCBCB; // Shaded sleeve
        int wc = 0xFFBCBCBC; // Sleeve cuff seam

        // Steve Skin Colors
        int sH = 0xFFDCA273; // Skin highlight
        int s0 = 0xFFC88E5E; // Base Steve skin
        int s1 = 0xFFBD8353; // Midtone skin
        int s2 = 0xFFB07647; // Shaded skin
        int sW = 0xFFA56B3D; // Wrist shadow
        int sF = 0xFF9E6436; // Finger creases / knuckles
        int sD = 0xFF8D5528; // Deep knuckle shadow

        // 1. Outside Face (x: 0..3, y: 4..15)
        // Sleeve (y: 4..7)
        int[][] outsideSleeve = {
            {w0, w1, w0, w2},
            {w1, w2, w3, w1},
            {w2, w3, w4, w2},
            {wc, w4, wc, w4}
        };
        // Skin (y: 8..15)
        int[][] outsideSkin = {
            {sH, s0, sH, s1},
            {s0, sH, s0, s1},
            {s0, s1, s2, s1},
            {s1, s0, s1, s2},
            {sW, s1, sW, s2},
            {s1, s0, s1, s2},
            {sF, sD, sF, sD},
            {s2, sF, s2, sF}
        };

        // 2. Front Face (x: 4..7, y: 4..15)
        int[][] frontSleeve = {
            {w0, w1, w2, w0},
            {w1, w2, w3, w1},
            {w2, w3, w4, w3},
            {wc, w4, wc, w3}
        };
        int[][] frontSkin = {
            {s0, sH, s0, s1},
            {s1, s0, sH, s0},
            {s0, s1, s2, s1},
            {s1, s0, s1, s2},
            {s2, s1, s2, sW},
            {s1, s0, s1, s2},
            {sF, sD, sF, sD},
            {sW, sF, sW, sF}
        };

        // 3. Inside Face (x: 8..11, y: 4..15)
        int[][] insideSleeve = {
            {w2, w3, w4, w3},
            {w3, w4, w4, w3},
            {w4, w4, wc, w4},
            {wc, wc, wc, w4}
        };
        int[][] insideSkin = {
            {s1, s2, s1, sW},
            {s2, s1, s2, sW},
            {s2, sW, s2, sW},
            {sW, s2, sW, sF},
            {sW, s2, sW, sF},
            {s2, s1, s2, sW},
            {sF, sD, sF, sD},
            {sW, sF, sW, sF}
        };

        // 4. Back Face (x: 12..15, y: 4..15)
        int[][] backSleeve = {
            {w2, w3, w4, w3},
            {w3, w4, wc, w4},
            {w4, wc, wc, w4},
            {wc, wc, wc, wc}
        };
        int[][] backSkin = {
            {s2, sW, s2, sW},
            {sW, s2, sW, sF},
            {sW, sF, sW, sF},
            {sF, sW, sF, sD},
            {sF, sW, sF, sD},
            {sW, s2, sW, sF},
            {sF, sD, sF, sD},
            {sD, sF, sD, sF}
        };

        // 5. Top Shoulder Face (x: 4..7, y: 0..3)
        int[][] topSleeve = {
            {w0, w1, w0, w2},
            {w1, w0, w2, w1},
            {w0, w2, w1, w3},
            {w2, w1, w3, w2}
        };

        // 6. Bottom Fist/Knuckles Face (x: 8..11, y: 0..3)
        int[][] bottomFist = {
            {sF, sD, sF, sD},
            {sD, sF, sD, sF},
            {sW, s2, sW, s2},
            {s2, s1, s2, s1}
        };

        // Blit arrays into pixel buffer
        blitGrid(pixels, outsideSleeve, 0, 4);
        blitGrid(pixels, outsideSkin, 0, 8);
        blitGrid(pixels, frontSleeve, 4, 4);
        blitGrid(pixels, frontSkin, 4, 8);
        blitGrid(pixels, insideSleeve, 8, 4);
        blitGrid(pixels, insideSkin, 8, 8);
        blitGrid(pixels, backSleeve, 12, 4);
        blitGrid(pixels, backSkin, 12, 8);
        blitGrid(pixels, topSleeve, 4, 0);
        blitGrid(pixels, bottomFist, 8, 0);

        for (int argb : pixels) {
            byte a = (byte) ((argb >> 24) & 0xFF);
            byte r = (byte) ((argb >> 16) & 0xFF);
            byte g = (byte) ((argb >> 8) & 0xFF);
            byte b = (byte) (argb & 0xFF);

            buffer.put(r).put(g).put(b).put(a);
        }
        buffer.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, TEX_SIZE, TEX_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void blitGrid(int[] dst, int[][] src, int startX, int startY) {
        for (int y = 0; y < src.length; y++) {
            for (int x = 0; x < src[y].length; x++) {
                int dstIdx = (startY + y) * TEX_SIZE + (startX + x);
                dst[dstIdx] = src[y][x];
            }
        }
    }

    private void buildSteveArmMesh() {
        // Arm proportions: 4x12x4 Minecraft pixel units (W=0.16, D=0.16, L=0.48)
        float hw = 0.08f;
        float hd = 0.08f;
        float yBottom = -0.48f; // Shoulder end
        float yTop = 0.0f;       // Fist end

        // 6 faces * 6 vertices = 36 vertices, 11 floats each
        FloatBuffer buffer = MemoryUtil.memAllocFloat(36 * 11);

        float inv = 1.0f / TEX_SIZE;

        // 1. Top Face (Fist End, +Y) -> UV: (4..8, 0..4)
        putTexturedQuad(buffer,
                -hw, yTop, -hd,   hw, yTop, -hd,   hw, yTop, hd,   -hw, yTop, hd,
                4 * inv, 0 * inv,  8 * inv, 4 * inv,
                0, 1, 0);

        // 2. Bottom Face (Shoulder End, -Y) -> UV: (8..12, 0..4)
        putTexturedQuad(buffer,
                -hw, yBottom, hd,   hw, yBottom, hd,   hw, yBottom, -hd,   -hw, yBottom, -hd,
                8 * inv, 0 * inv,  12 * inv, 4 * inv,
                0, -1, 0);

        // 3. Front Face (+Z) -> UV: (4..8, 4..16)
        putTexturedQuad(buffer,
                -hw, yBottom, hd,   hw, yBottom, hd,   hw, yTop, hd,   -hw, yTop, hd,
                4 * inv, 4 * inv,   8 * inv, 16 * inv,
                0, 0, 1);

        // 4. Back Face (-Z) -> UV: (12..16, 4..16)
        putTexturedQuad(buffer,
                hw, yBottom, -hd,   -hw, yBottom, -hd,   -hw, yTop, -hd,   hw, yTop, -hd,
                12 * inv, 4 * inv,  16 * inv, 16 * inv,
                0, 0, -1);

        // 5. Left Face (Inner Arm, -X) -> UV: (8..12, 4..16)
        putTexturedQuad(buffer,
                -hw, yBottom, -hd,   -hw, yBottom, hd,   -hw, yTop, hd,   -hw, yTop, -hd,
                8 * inv, 4 * inv,    12 * inv, 16 * inv,
                -1, 0, 0);

        // 6. Right Face (Outer Arm, +X) -> UV: (0..4, 4..16)
        putTexturedQuad(buffer,
                hw, yBottom, hd,   hw, yBottom, -hd,   hw, yTop, -hd,   hw, yTop, hd,
                0 * inv, 4 * inv,  4 * inv, 16 * inv,
                1, 0, 0);

        buffer.flip();
        vertexCount = buffer.limit() / 11;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 11 * Float.BYTES;

        // aPos (loc 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // aUV (loc 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // aColor (loc 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // aNormal (loc 3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    private void putTexturedQuad(FloatBuffer buf,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float u0, float v0, float u1, float v1,
                                float nx, float ny, float nz) {
        // Triangle 1
        putV(buf, x1, y1, z1, u0, v1, nx, ny, nz);
        putV(buf, x2, y2, z2, u1, v1, nx, ny, nz);
        putV(buf, x3, y3, z3, u1, v0, nx, ny, nz);

        // Triangle 2
        putV(buf, x1, y1, z1, u0, v1, nx, ny, nz);
        putV(buf, x3, y3, z3, u1, v0, nx, ny, nz);
        putV(buf, x4, y4, z4, u0, v0, nx, ny, nz);
    }

    private void putV(FloatBuffer buf, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buf.put(x).put(y).put(z);
        buf.put(u).put(v);
        buf.put(1.0f).put(1.0f).put(1.0f); // Tint color (1, 1, 1)
        buf.put(nx).put(ny).put(nz);
    }

    public void triggerSwing() {
        this.swingProgress = 0.001f;
        this.isSwinging = true;
    }

    public void setMining(boolean mining) {
        this.isMining = mining;
    }

    public void update(float deltaTime, Player player) {
        idleTime += deltaTime;

        // 1. Continuous Block Breaking Animation (Minecraft Breaking Arc)
        if (isMining) {
            miningTimer += deltaTime * 4.8f; // ~4.8 chops/sec identical to vanilla Minecraft
            swingProgress = miningTimer % 1.0f;
        } else if (isSwinging) {
            // 2. Single Attack / Punch Swing Animation
            swingProgress += deltaTime * 4.4f; // ~0.23s duration
            if (swingProgress >= 1.0f) {
                swingProgress = 0.0f;
                isSwinging = false;
            }
        } else {
            swingProgress = 0.0f;
            miningTimer = 0.0f;
        }

        // 3. Walk & Sprint View-Bobbing
        float horizSpeed = (float) Math.sqrt(player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z);
        boolean isMoving = player.onGround && horizSpeed > 0.02f;

        if (isMoving) {
            float bobSpeed = player.isSprinting ? 14.0f : 9.5f;
            bobTimer += deltaTime * bobSpeed;
            float targetBob = Math.min(1.0f, horizSpeed / 4.0f);
            bobAmount += (targetBob - bobAmount) * Math.min(1.0f, deltaTime * 8.0f);
        } else {
            bobAmount += (0.0f - bobAmount) * Math.min(1.0f, deltaTime * 8.0f);
        }
    }

    public void render(Camera camera, Player player, Vector3f sunDir, Vector3f directLightColor, Vector3f skyAmbientColor, Vector3f groundAmbientColor, float aspectRatio) {
        // Base Viewmodel Anchor Position matching reference screenshot (Bottom right)
        float baseX = 0.44f;
        float baseY = -0.40f;
        float baseZ = -0.56f;

        // 1. Calculate Idle Breathing Bob
        float idleY = (float) Math.sin(idleTime * 1.8f) * 0.003f;

        // 2. Calculate Walk View-Bobbing
        float bobX = (float) Math.sin(bobTimer) * 0.028f * bobAmount;
        float bobY = -(float) Math.abs(Math.cos(bobTimer)) * 0.028f * bobAmount + idleY;
        float bobRoll = (float) Math.sin(bobTimer) * 3.0f * bobAmount;

        // 3. Calculate Swing / Breaking Animation Offsets & Rotations (Vanilla Minecraft Math)
        float animX = 0, animY = 0, animZ = 0;
        float animPitch = 0, animYaw = 0, animRoll = 0;

        if (swingProgress > 0.0f) {
            float t = swingProgress;
            float f1 = (float) Math.sin(t * t * Math.PI);
            float f2 = (float) Math.sin(Math.sqrt(t) * Math.PI);
            float f3 = (float) Math.sin(t * Math.PI);

            // Forward-down chopping translation
            animX = -f2 * 0.28f;
            animY = f3 * 0.16f;
            animZ = -f2 * 0.26f;

            // Authentic downward breaking chopping arc
            animPitch = -f2 * 72.0f;
            animYaw = -f1 * 24.0f;
            animRoll = -f2 * 36.0f;
        }

        // Setup Projection & View Matrices for First-Person Viewmodel
        viewmodelProjection.identity();
        viewmodelProjection.perspective((float) Math.toRadians(66.0f), aspectRatio, 0.01f, 20.0f);

        viewmodelView.identity(); // Anchored directly in front of eye camera

        // Build Model Matrix matching screenshot orientation
        modelMatrix.identity();

        // Translate to Hand Position
        modelMatrix.translate(baseX + bobX + animX, baseY + bobY + animY, baseZ + animZ);

        // Apply Rotations: Base Inward Yaw (-45 deg), Pitch (24 deg), Roll (-32 deg) + Active Animations
        modelMatrix.rotate((float) Math.toRadians(-45.0f + animYaw), new Vector3f(0, 1, 0));       // Yaw inward
        modelMatrix.rotate((float) Math.toRadians(24.0f + animPitch), new Vector3f(1, 0, 0));        // Pitch forward
        modelMatrix.rotate((float) Math.toRadians(-32.0f + bobRoll + animRoll), new Vector3f(0, 0, 1)); // Roll tilt

        // Render Hand with GL_DEPTH_TEST and depth buffer clearing so hand never clips into voxels
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glClear(GL_DEPTH_BUFFER_BIT);

        handShader.bind();
        handShader.setUniform("uProjection", viewmodelProjection);
        handShader.setUniform("uView", viewmodelView);
        handShader.setUniform("uModel", modelMatrix);
        handShader.setUniform("uSunDir", (sunDir.y >= -0.05f) ? sunDir : new Vector3f(sunDir).negate());
        handShader.setUniform("uDirectLightColor", directLightColor);
        handShader.setUniform("uSkyAmbientColor", skyAmbientColor);
        handShader.setUniform("uGroundAmbientColor", groundAmbientColor);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        handShader.setUniform("uTexture", 0);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D, 0);
        handShader.unbind();
    }

    public void cleanup() {
        if (handShader != null) handShader.cleanup();
        if (textureId != 0) glDeleteTextures(textureId);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
    }
}

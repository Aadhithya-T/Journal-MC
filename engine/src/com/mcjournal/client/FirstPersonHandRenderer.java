package com.mcjournal.client;

import com.mcjournal.Item;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
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

    // Bare Hand Mesh
    private int armVao;
    private int armVbo;
    private int armVertexCount = 0;

    // Dynamic Held Block Mesh
    private int heldBlockVao;
    private int heldBlockVbo;
    private int heldBlockVertexCount = 0;
    private byte currentHeldBlockType = -1;

    private final Matrix4f viewmodelProjection = new Matrix4f();
    private final Matrix4f viewmodelView = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();

    // Animation states
    private float swingProgress = 0.0f;       // 0.0 to 1.0
    private boolean isSwinging = false;
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
     * featuring a crisp white shirt sleeve and tanned skin.
     */
    private void generateSteveSkinTexture() {
        ByteBuffer buffer = MemoryUtil.memAlloc(TEX_SIZE * TEX_SIZE * 4);
        int[] pixels = new int[TEX_SIZE * TEX_SIZE];

        // Color definitions (ARGB)
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
        int[][] outsideSleeve = {
            {w0, w1, w0, w2},
            {w1, w2, w3, w1},
            {w2, w3, w4, w2},
            {wc, w4, wc, w4}
        };
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
            {s0, s1, s0, s1},
            {s1, s0, s1, s0},
            {s1, sW, s1, sW},
            {s0, s1, s0, s1},
            {sF, sD, sF, sD},
            {s1, s2, s1, s2}
        };

        // 3. Inside Face (x: 8..11, y: 4..15)
        int[][] insideSleeve = {
            {w1, w0, w1, w2},
            {w2, w1, w2, w3},
            {w3, w2, w3, w4},
            {w4, wc, w4, wc}
        };
        int[][] insideSkin = {
            {s0, s1, s0, s1},
            {s1, s2, s1, s2},
            {s1, s0, s1, s2},
            {s2, s1, s2, s1},
            {sW, s2, sW, s2},
            {s1, s2, s1, s0},
            {s0, s1, s0, s1},
            {s1, s0, s1, s0}
        };

        // 4. Back Face (x: 12..15, y: 4..16)
        int[][] backSleeve = {
            {w1, w2, w1, w0},
            {w2, w3, w2, w1},
            {w3, w4, w3, w2},
            {w4, wc, w4, wc}
        };
        int[][] backSkin = {
            {s1, s2, s1, s0},
            {s2, s1, s2, s1},
            {s1, s0, s1, s2},
            {s0, s1, s0, s1},
            {s2, sW, s2, sW},
            {s1, s0, s1, s0},
            {s0, s1, s0, s1},
            {s1, s0, s1, s0}
        };

        // 5. Top Shoulder End (x: 4..7, y: 0..3)
        int[][] topShoulder = {
            {w0, w1, w0, w1},
            {w1, w2, w1, w2},
            {w0, w1, w0, w1},
            {w1, w2, w1, w2}
        };

        // 6. Bottom Palm End (x: 8..11, y: 0..3)
        int[][] bottomPalm = {
            {s0, s1, s0, s1},
            {s1, s2, s1, s2},
            {s0, s1, s0, s1},
            {sF, sD, sF, sD}
        };

        blitGrid(pixels, topShoulder, 4, 0);
        blitGrid(pixels, bottomPalm, 8, 0);

        blitGrid(pixels, outsideSleeve, 0, 4);
        blitGrid(pixels, outsideSkin, 0, 8);

        blitGrid(pixels, frontSleeve, 4, 4);
        blitGrid(pixels, frontSkin, 4, 8);

        blitGrid(pixels, insideSleeve, 8, 4);
        blitGrid(pixels, insideSkin, 8, 8);

        blitGrid(pixels, backSleeve, 12, 4);
        blitGrid(pixels, backSkin, 12, 8);

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
        float yTop = 0.18f;      // Arm base extended past viewport edge

        FloatBuffer buffer = MemoryUtil.memAllocFloat(36 * 11);
        float inv = 1.0f / TEX_SIZE;

        // 1. Top Face (Shoulder Base, +Y) -> UV: (4..8, 0..4)
        putTexturedQuad(buffer,
                -hw, yTop, -hd,   hw, yTop, -hd,   hw, yTop, hd,   -hw, yTop, hd,
                4 * inv, 0 * inv,  8 * inv, 4 * inv,
                0, 1, 0);

        // 2. Bottom Face (Fist End, -Y) -> UV: (8..12, 0..4)
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
        armVertexCount = buffer.limit() / 11;

        armVao = glGenVertexArrays();
        armVbo = glGenBuffers();

        glBindVertexArray(armVao);
        glBindBuffer(GL_ARRAY_BUFFER, armVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        setupVertexAttributes();

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    private void setupVertexAttributes() {
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

    public void update(float deltaTime, Player player) {
        idleTime += deltaTime;

        // Swing animation
        if (isSwinging) {
            swingProgress += deltaTime * 4.4f;
            if (swingProgress >= 1.0f) {
                swingProgress = 0.0f;
                isSwinging = false;
            }
        } else {
            swingProgress = 0.0f;
        }

        // Walk & Sprint View-Bobbing
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

    private void updateHeldBlockMesh(byte blockType) {
        if (blockType == currentHeldBlockType && heldBlockVao != 0) return;
        currentHeldBlockType = blockType;

        if (blockType == 0 || Item.isLegacyTool(blockType)) {
            heldBlockVertexCount = 0;
            return;
        }

        FloatBuffer buffer = MemoryUtil.memAllocFloat(64 * 11);
        BlockType type = BlockStateRegistry.getBlockType(blockType);

        if (type.isPlant()) {
            int tile = type.getDisplayFaceTile();
            float[] uv = TextureAtlas.getTileUV(tile);
            float u0 = uv[0], u1 = uv[1], v0 = uv[2], v1 = uv[3];

            float s = 0.20f;
            float hs = s / 2.0f;
            float by0 = -0.62f;
            float by1 = by0 + s;
            float bz = 0.0f;

            // Quad 1: Diagonal / (Double-sided)
            putTexturedQuad(buffer, -hs, by0, bz - hs, hs, by0, bz + hs, hs, by1, bz + hs, -hs, by1, bz - hs, u0, v0, u1, v1, 0, 1, 0);
            putTexturedQuad(buffer, hs, by0, bz + hs, -hs, by0, bz - hs, -hs, by1, bz - hs, hs, by1, bz + hs, u0, v0, u1, v1, 0, 1, 0);

            // Quad 2: Diagonal \ (Double-sided)
            putTexturedQuad(buffer, -hs, by0, bz + hs, hs, by0, bz - hs, hs, by1, bz - hs, -hs, by1, bz + hs, u0, v0, u1, v1, 0, 1, 0);
            putTexturedQuad(buffer, hs, by0, bz - hs, -hs, by0, bz + hs, -hs, by1, bz + hs, hs, by1, bz - hs, u0, v0, u1, v1, 0, 1, 0);
        } else {
            // Render miniature 3D block cube held in fist
            float s = 0.11f;
            float cx = -0.01f;
            float cy = -0.56f;
            float cz = -0.03f;

            float x0 = cx - s, x1 = cx + s;
            float y0 = cy - s, y1 = cy + s;
            float z0 = cz - s, z1 = cz + s;

            BlockState defaultState = type.getDefaultState();

            for (int face = 0; face < 6; face++) {
                int slot = defaultState.getFaceTextureSlot(face);
                float[] uv = TextureAtlas.getTileUV(slot);
                float u0 = uv[0], u1 = uv[1], v0 = uv[2], v1 = uv[3];

                switch (face) {
                    case 0 -> putTexturedQuad(buffer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1, 1, 0, 0);
                    case 1 -> putTexturedQuad(buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1, -1, 0, 0);
                    case 2 -> putTexturedQuad(buffer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1, 0, 1, 0);
                    case 3 -> putTexturedQuad(buffer, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u0, v0, u1, v1, 0, -1, 0);
                    case 4 -> putTexturedQuad(buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, 0, 0, 1);
                    case 5 -> putTexturedQuad(buffer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1, 0, 0, -1);
                }
            }
        }

        buffer.flip();
        heldBlockVertexCount = buffer.limit() / 11;

        if (heldBlockVao == 0) {
            heldBlockVao = glGenVertexArrays();
            heldBlockVbo = glGenBuffers();
        }

        glBindVertexArray(heldBlockVao);
        glBindBuffer(GL_ARRAY_BUFFER, heldBlockVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        setupVertexAttributes();
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    public void render(Camera camera, Player player, TextureAtlas atlas, Vector3f sunDir, Vector3f directLightColor, Vector3f skyAmbientColor, Vector3f groundAmbientColor, float aspectRatio) {
        byte heldItem = player.getSelectedBlock();
        boolean isHoldingBlock = (heldItem != 0 && !Item.isLegacyTool(heldItem));

        if (isHoldingBlock) {
            updateHeldBlockMesh(heldItem);
        }

        // Base Viewmodel Anchor Position
        float baseX = isHoldingBlock ? 0.40f : 0.44f;
        float baseY = isHoldingBlock ? -0.36f : -0.42f;
        float baseZ = isHoldingBlock ? -0.50f : -0.56f;

        // 1. Idle Breathing Bob
        float idleY = (float) Math.sin(idleTime * 1.8f) * 0.003f;

        // 2. Walk View-Bobbing
        float bobX = (float) Math.sin(bobTimer) * 0.028f * bobAmount;
        float bobY = -(float) Math.abs(Math.cos(bobTimer)) * 0.028f * bobAmount + idleY;
        float bobRoll = (float) Math.sin(bobTimer) * 3.0f * bobAmount;

        // 3. Swing Animation Offsets & Rotations
        float animX = 0, animY = 0, animZ = 0;
        float animPitch = 0, animYaw = 0, animRoll = 0;

        if (swingProgress > 0.0f) {
            float t = swingProgress;
            float f1 = (float) Math.sin(t * t * Math.PI);
            float f2 = (float) Math.sin(Math.sqrt(t) * Math.PI);

            animX = -f1 * 0.18f;
            animY = -f2 * 0.18f;
            animZ = -f2 * 0.22f;

            animPitch = f2 * 68.0f;
            animYaw = f1 * 26.0f;
            animRoll = -f2 * 22.0f;
        }

        viewmodelProjection.identity();
        viewmodelProjection.perspective((float) Math.toRadians(66.0f), aspectRatio, 0.01f, 20.0f);

        viewmodelView.identity();

        modelMatrix.identity();
        modelMatrix.translate(baseX + bobX + animX, baseY + bobY + animY, baseZ + animZ);

        float baseYaw = isHoldingBlock ? -48.0f : -45.0f;
        float basePitch = isHoldingBlock ? 28.0f : 24.0f;
        float baseRoll = isHoldingBlock ? -32.0f : -32.0f;

        modelMatrix.rotate((float) Math.toRadians(baseYaw + animYaw), new Vector3f(0, 1, 0));
        modelMatrix.rotate((float) Math.toRadians(basePitch + animPitch), new Vector3f(1, 0, 0));
        modelMatrix.rotate((float) Math.toRadians(baseRoll + bobRoll + animRoll), new Vector3f(0, 0, 1));
        modelMatrix.scale(0.85f);

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

        // 1. Draw Steve Arm Mesh
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        handShader.setUniform("uTexture", 0);

        glBindVertexArray(armVao);
        glDrawArrays(GL_TRIANGLES, 0, armVertexCount);
        glBindVertexArray(0);

        // 2. If holding a Block, draw the 3D Held Block in fist using TextureAtlas
        if (isHoldingBlock && heldBlockVertexCount > 0 && atlas != null) {
            glBindTexture(GL_TEXTURE_2D, atlas.getTextureId());
            glBindVertexArray(heldBlockVao);
            glDrawArrays(GL_TRIANGLES, 0, heldBlockVertexCount);
            glBindVertexArray(0);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        handShader.unbind();
    }

    public void cleanup() {
        if (handShader != null) handShader.cleanup();
        if (textureId != 0) glDeleteTextures(textureId);
        if (armVbo != 0) glDeleteBuffers(armVbo);
        if (armVao != 0) glDeleteVertexArrays(armVao);
        if (heldBlockVbo != 0) glDeleteBuffers(heldBlockVbo);
        if (heldBlockVao != 0) glDeleteVertexArrays(heldBlockVao);
    }
}

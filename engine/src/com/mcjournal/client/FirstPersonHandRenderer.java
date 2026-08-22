package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.Item;
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

    // Iron Axe Held Mesh
    private int axeHandVao;
    private int axeHandVbo;
    private int axeHandVertexCount = 0;

    // Iron Shovel Held Mesh
    private int shovelHandVao;
    private int shovelHandVbo;
    private int shovelHandVertexCount = 0;

    // Iron Pickaxe Held Mesh
    private int pickaxeHandVao;
    private int pickaxeHandVbo;
    private int pickaxeHandVertexCount = 0;

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
    private boolean isMining = false;         // Continuous block breaking
    private float miningTimer = 0.0f;
    private float bobTimer = 0.0f;            // Walk view-bobbing
    private float bobAmount = 0.0f;
    private float idleTime = 0.0f;

    public void init() {
        handShader = new ShaderProgram("/shaders/hand_vertex.glsl", "/shaders/hand_fragment.glsl");
        generateSteveSkinTexture();
        buildSteveArmMesh();
        buildAxeHandMesh();
        buildShovelHandMesh();
        buildPickaxeHandMesh();
    }

    /**
     * Generates an authentic Minecraft skin texture for Steve's Arm
     * featuring a crisp white shirt sleeve, tanned skin, and Iron Axe tool textures.
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
            {sW, s1, sW, s1},
            {s0, s1, s0, s1},
            {sF, sF, sF, sF},
            {sD, sD, sD, sD}
        };

        // 3. Inside Face (x: 8..11, y: 4..15)
        int[][] insideSleeve = {
            {w1, w0, w1, w2},
            {w2, w1, w2, w3},
            {w3, w2, w3, w4},
            {wc, w3, wc, w4}
        };
        int[][] insideSkin = {
            {s0, s1, s0, sH},
            {sH, s0, s1, s0},
            {s1, s2, s1, s0},
            {s2, s1, s2, s1},
            {sW, s2, sW, s1},
            {s2, s1, s2, s1},
            {sD, sF, sD, sF},
            {sF, s2, sF, s2}
        };

        // 4. Back Face (x: 12..15, y: 4..15)
        int[][] backSleeve = {
            {w2, w1, w0, w1},
            {w3, w2, w1, w2},
            {w4, w3, w2, w3},
            {wc, w4, wc, w4}
        };
        int[][] backSkin = {
            {s1, s0, s1, s0},
            {s0, s1, s0, sH},
            {s1, s2, s1, s0},
            {s0, s1, s0, s1},
            {s1, sW, s1, sW},
            {s1, s0, s1, s0},
            {sF, sD, sF, sD},
            {sD, sF, sD, sF}
        };

        // 5. Top Shoulder Face (x: 4..7, y: 0..3)
        int[][] topSleeve = {
            {w0, w1, w0, w2},
            {w1, w0, w2, w1},
            {w2, w1, w3, w2},
            {w1, w2, w2, w3}
        };

        // 6. Bottom Fist Face (x: 8..11, y: 0..3)
        int[][] bottomFist = {
            {sF, sD, sF, sD},
            {sD, sF, sD, sF},
            {sF, sD, sF, sD},
            {sD, sF, sD, sF}
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

        // 7. Iron Axe Wood Handle (x: 16..19, y: 0..15)
        int wLight = 0xFFB8824A;
        int wMid   = 0xFF8F6030;
        int wDark  = 0xFF65411D;
        for (int y = 0; y < 16; y++) {
            pixels[y * TEX_SIZE + 16] = (y % 3 == 0) ? wDark : wMid;
            pixels[y * TEX_SIZE + 17] = (y % 2 == 0) ? wLight : wMid;
            pixels[y * TEX_SIZE + 18] = (y % 4 == 0) ? wDark : wMid;
            pixels[y * TEX_SIZE + 19] = (y % 3 == 1) ? wDark : wMid;
        }

        // 8. Iron Axe Metallic Head (x: 20..31, y: 0..15)
        int iDark  = 0xFF3D3D3D; // Dark steel outline
        int iShade = 0xFF7D7D7D; // Shaded iron
        int iBase  = 0xFFB8B8B8; // Base iron
        int iLight = 0xFFEDEDED; // Polished silver
        int iGlint = 0xFFFFFFFF; // Razor cutting edge

        for (int y = 0; y < 16; y++) {
            for (int x = 20; x < 32; x++) {
                if (x == 31 || y == 0 || y == 15) {
                    pixels[y * TEX_SIZE + x] = (x == 31 && y >= 2 && y <= 13) ? iGlint : iLight;
                } else if (x >= 28) {
                    pixels[y * TEX_SIZE + x] = (y % 2 == 0) ? iLight : iBase;
                } else if (x >= 24) {
                    pixels[y * TEX_SIZE + x] = (y % 3 == 0) ? iBase : iShade;
                } else {
                    pixels[y * TEX_SIZE + x] = (x == 20) ? iDark : iShade;
                }
            }
        }

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

        // 1. Top Face (Shoulder Base, +Y) -> UV: (4..8, 0..4) [White shirt sleeve]
        putTexturedQuad(buffer,
                -hw, yTop, -hd,   hw, yTop, -hd,   hw, yTop, hd,   -hw, yTop, hd,
                4 * inv, 0 * inv,  8 * inv, 4 * inv,
                0, 1, 0);

        // 2. Bottom Face (Fist End, -Y) -> UV: (8..12, 0..4) [Knuckles/Palm]
        putTexturedQuad(buffer,
                -hw, yBottom, hd,   hw, yBottom, hd,   hw, yBottom, -hd,   -hw, yBottom, -hd,
                8 * inv, 0 * inv,  12 * inv, 4 * inv,
                0, -1, 0);

        // 3. Front Face (+Z) -> UV: (4..8, 4..16) [yBottom: Knuckles, yTop: Sleeve]
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

    private void buildAxeHandMesh() {
        // Arm + 3D Iron Axe Mesh (Bold, Prominent Proportions)
        FloatBuffer buffer = MemoryUtil.memAllocFloat(256 * 11);
        float inv = 1.0f / TEX_SIZE;

        float hw = 0.08f;
        float hd = 0.08f;
        float yBottom = -0.48f;
        float yTop = 0.0f;

        // 1. Arm Faces (6 faces)
        putTexturedQuad(buffer, -hw, yTop, -hd, hw, yTop, -hd, hw, yTop, hd, -hw, yTop, hd, 4 * inv, 0 * inv, 8 * inv, 4 * inv, 0, 1, 0);
        putTexturedQuad(buffer, -hw, yBottom, hd, hw, yBottom, hd, hw, yBottom, -hd, -hw, yBottom, -hd, 8 * inv, 0 * inv, 12 * inv, 4 * inv, 0, -1, 0);
        putTexturedQuad(buffer, -hw, yBottom, hd, hw, yBottom, hd, hw, yTop, hd, -hw, yTop, hd, 4 * inv, 4 * inv, 8 * inv, 16 * inv, 0, 0, 1);
        putTexturedQuad(buffer, hw, yBottom, -hd, -hw, yBottom, -hd, -hw, yTop, -hd, hw, yTop, -hd, 12 * inv, 4 * inv, 16 * inv, 16 * inv, 0, 0, -1);
        putTexturedQuad(buffer, -hw, yBottom, -hd, -hw, yBottom, hd, -hw, yTop, hd, -hw, yTop, -hd, 8 * inv, 4 * inv, 12 * inv, 16 * inv, -1, 0, 0);
        putTexturedQuad(buffer, hw, yBottom, hd, hw, yBottom, -hd, hw, yTop, -hd, hw, yTop, hd, 0 * inv, 4 * inv, 4 * inv, 16 * inv, 1, 0, 0);

        // 2. Iron Axe Wooden Shaft (Robust oak handle through fist on far end)
        float hx = 0.024f;
        float hz = 0.024f;
        float hy0 = -0.88f;
        float hy1 = -0.20f;
        float hu0 = 16 * inv;
        float hu1 = 19 * inv;
        float hv0 = 0 * inv;
        float hv1 = 15 * inv;

        // Shaft: Front (+Z), Back (-Z), Left (-X), Right (+X), Top (+Y), Bottom (-Y)
        putTexturedQuad(buffer, -hx, hy0, hz, hx, hy0, hz, hx, hy1, hz, -hx, hy1, hz, hu0, hv0, hu1, hv1, 0, 0, 1);
        putTexturedQuad(buffer, hx, hy0, -hz, -hx, hy0, -hz, -hx, hy1, -hz, hx, hy1, -hz, hu0, hv0, hu1, hv1, 0, 0, -1);
        putTexturedQuad(buffer, -hx, hy0, -hz, -hx, hy0, hz, -hx, hy1, hz, -hx, hy1, -hz, hu0, hv0, hu1, hv1, -1, 0, 0);
        putTexturedQuad(buffer, hx, hy0, hz, hx, hy0, -hz, hx, hy1, -hz, hx, hy1, hz, hu0, hv0, hu1, hv1, 1, 0, 0);
        putTexturedQuad(buffer, -hx, hy1, -hz, hx, hy1, -hz, hx, hy1, hz, -hx, hy1, hz, hu0, hv0, hu1, 4 * inv, 0, 1, 0);
        putTexturedQuad(buffer, -hx, hy0, hz, hx, hy0, hz, hx, hy0, -hz, -hx, hy0, -hz, hu0, hv0, hu1, 4 * inv, 0, -1, 0);

        // 3. Iron Axe Socket (Head base collar)
        float sx = 0.038f;
        float sz = 0.055f;
        float sy0 = -0.80f;
        float sy1 = -0.56f;
        float su0 = 20 * inv;
        float su1 = 24 * inv;
        float sv0 = 0 * inv;
        float sv1 = 8 * inv;

        putTexturedQuad(buffer, -sx, sy0, sz, sx, sy0, sz, sx, sy1, sz, -sx, sy1, sz, su0, sv0, su1, sv1, 0, 0, 1);
        putTexturedQuad(buffer, sx, sy0, -sz, -sx, sy0, -sz, -sx, sy1, -sz, sx, sy1, -sz, su0, sv0, su1, sv1, 0, 0, -1);
        putTexturedQuad(buffer, -sx, sy0, -sz, -sx, sy0, sz, -sx, sy1, sz, -sx, sy1, -sz, su0, sv0, su1, sv1, -1, 0, 0);
        putTexturedQuad(buffer, sx, sy0, sz, sx, sy0, -sz, sx, sy1, -sz, sx, sy1, sz, su0, sv0, su1, sv1, 1, 0, 0);
        putTexturedQuad(buffer, -sx, sy1, -sz, sx, sy1, -sz, sx, sy1, sz, -sx, sy1, sz, su0, sv0, su1, sv1, 0, 1, 0);
        putTexturedQuad(buffer, -sx, sy0, sz, sx, sy0, sz, sx, sy0, -sz, -sx, sy0, -sz, su0, sv0, su1, sv1, 0, -1, 0);

        // 4. Iron Axe Cutting Blade (Large curved iron blade reaching forward in -Z direction)
        float bx = 0.024f;
        float bz0 = -0.045f;
        float bz1 = -0.32f; // Prominent forward blade reach
        float by0 = -0.84f;  // Lower beard
        float by1 = -0.44f;  // Upper blade edge
        float bu0 = 24 * inv;
        float bu1 = 31 * inv;
        float bv0 = 0 * inv;
        float bv1 = 15 * inv;

        putTexturedQuad(buffer, -bx, by0, bz0, -bx, by0, bz1, -bx, by1, bz1, -bx, by1, bz0, bu0, bv0, bu1, bv1, -1, 0, 0); // Left blade face
        putTexturedQuad(buffer, bx, by0, bz1, bx, by0, bz0, bx, by1, bz0, bx, by1, bz1, bu0, bv0, bu1, bv1, 1, 0, 0);   // Right blade face
        putTexturedQuad(buffer, -bx, by0, bz1, bx, by0, bz1, bx, by1, bz1, -bx, by1, bz1, 31 * inv, bv0, 32 * inv, bv1, 0, 0, -1); // Front razor edge (Bright White Glint)
        putTexturedQuad(buffer, -bx, by1, bz1, bx, by1, bz1, bx, by1, bz0, -bx, by1, bz0, bu0, bv0, bu1, 4 * inv, 0, 1, 0);  // Top blade slope
        putTexturedQuad(buffer, -bx, by0, bz0, bx, by0, bz0, bx, by0, bz1, -bx, by0, bz1, bu0, bv0, bu1, 4 * inv, 0, -1, 0); // Bottom beard slope

        // 5. Iron Axe Back Spur / Poll (Protrudes back in +Z direction)
        float px = 0.032f;
        float pz0 = 0.045f;
        float pz1 = 0.14f;
        float py0 = -0.76f;
        float py1 = -0.60f;

        putTexturedQuad(buffer, -px, py0, pz1, px, py0, pz1, px, py1, pz1, -px, py1, pz1, su0, sv0, su1, sv1, 0, 0, 1);
        putTexturedQuad(buffer, -px, py0, pz0, -px, py0, pz1, -px, py1, pz1, -px, py1, pz0, su0, sv0, su1, sv1, -1, 0, 0);
        putTexturedQuad(buffer, px, py0, pz1, px, py0, pz0, px, py1, pz0, px, py1, pz1, su0, sv0, su1, sv1, 1, 0, 0);
        putTexturedQuad(buffer, -px, py1, pz0, px, py1, pz0, px, py1, pz1, -px, py1, pz1, su0, sv0, su1, sv1, 0, 1, 0);
        putTexturedQuad(buffer, -px, py0, pz1, px, py0, pz1, px, py0, pz0, -px, py0, pz0, su0, sv0, su1, sv1, 0, -1, 0);

        buffer.flip();
        axeHandVertexCount = buffer.limit() / 11;

        axeHandVao = glGenVertexArrays();
        axeHandVbo = glGenBuffers();

        glBindVertexArray(axeHandVao);
        glBindBuffer(GL_ARRAY_BUFFER, axeHandVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        setupVertexAttributes();

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    private void buildShovelHandMesh() {
        // Arm + 3D Iron Shovel Mesh (Bold, Prominent Scoop)
        FloatBuffer buffer = MemoryUtil.memAllocFloat(256 * 11);
        float inv = 1.0f / TEX_SIZE;

        float hw = 0.08f;
        float hd = 0.08f;
        float yBottom = -0.48f;
        float yTop = 0.0f;

        // 1. Arm Faces (6 faces)
        putTexturedQuad(buffer, -hw, yTop, -hd, hw, yTop, -hd, hw, yTop, hd, -hw, yTop, hd, 4 * inv, 0 * inv, 8 * inv, 4 * inv, 0, 1, 0);
        putTexturedQuad(buffer, -hw, yBottom, hd, hw, yBottom, hd, hw, yBottom, -hd, -hw, yBottom, -hd, 8 * inv, 0 * inv, 12 * inv, 4 * inv, 0, -1, 0);
        putTexturedQuad(buffer, -hw, yBottom, hd, hw, yBottom, hd, hw, yTop, hd, -hw, yTop, hd, 4 * inv, 4 * inv, 8 * inv, 16 * inv, 0, 0, 1);
        putTexturedQuad(buffer, hw, yBottom, -hd, -hw, yBottom, -hd, -hw, yTop, -hd, hw, yTop, -hd, 12 * inv, 4 * inv, 16 * inv, 16 * inv, 0, 0, -1);
        putTexturedQuad(buffer, -hw, yBottom, -hd, -hw, yBottom, hd, -hw, yTop, hd, -hw, yTop, -hd, 8 * inv, 4 * inv, 12 * inv, 16 * inv, -1, 0, 0);
        putTexturedQuad(buffer, hw, yBottom, hd, hw, yBottom, -hd, hw, yTop, -hd, hw, yTop, hd, 0 * inv, 4 * inv, 4 * inv, 16 * inv, 1, 0, 0);

        // 2. Iron Shovel Wooden Shaft (Long slender oak handle through fist on far end)
        float hx = 0.024f;
        float hz = 0.024f;
        float hy0 = -0.90f;
        float hy1 = -0.20f;
        float hu0 = 16 * inv;
        float hu1 = 19 * inv;
        float hv0 = 0 * inv;
        float hv1 = 15 * inv;

        putTexturedQuad(buffer, -hx, hy0, hz, hx, hy0, hz, hx, hy1, hz, -hx, hy1, hz, hu0, hv0, hu1, hv1, 0, 0, 1);
        putTexturedQuad(buffer, hx, hy0, -hz, -hx, hy0, -hz, -hx, hy1, -hz, hx, hy1, -hz, hu0, hv0, hu1, hv1, 0, 0, -1);
        putTexturedQuad(buffer, -hx, hy0, -hz, -hx, hy0, hz, -hx, hy1, hz, -hx, hy1, -hz, hu0, hv0, hu1, hv1, -1, 0, 0);
        putTexturedQuad(buffer, hx, hy0, hz, hx, hy0, -hz, hx, hy1, -hz, hx, hy1, hz, hu0, hv0, hu1, hv1, 1, 0, 0);
        putTexturedQuad(buffer, -hx, hy1, -hz, hx, hy1, -hz, hx, hy1, hz, -hx, hy1, hz, hu0, hv0, hu1, 4 * inv, 0, 1, 0);
        putTexturedQuad(buffer, -hx, hy0, hz, hx, hy0, hz, hx, hy0, -hz, -hx, hy0, -hz, hu0, hv0, hu1, 4 * inv, 0, -1, 0);

        // 3. Shovel Socket Collar
        float sx = 0.036f;
        float sz = 0.036f;
        float sy0 = -0.84f;
        float sy1 = -0.68f;
        float su0 = 20 * inv;
        float su1 = 24 * inv;
        float sv0 = 0 * inv;
        float sv1 = 8 * inv;

        putTexturedQuad(buffer, -sx, sy0, sz, sx, sy0, sz, sx, sy1, sz, -sx, sy1, sz, su0, sv0, su1, sv1, 0, 0, 1);
        putTexturedQuad(buffer, sx, sy0, -sz, -sx, sy0, -sz, -sx, sy1, -sz, sx, sy1, -sz, su0, sv0, su1, sv1, 0, 0, -1);
        putTexturedQuad(buffer, -sx, sy0, -sz, -sx, sy0, sz, -sx, sy1, sz, -sx, sy1, -sz, su0, sv0, su1, sv1, -1, 0, 0);
        putTexturedQuad(buffer, sx, sy0, sz, sx, sy0, -sz, sx, sy1, -sz, sx, sy1, sz, su0, sv0, su1, sv1, 1, 0, 0);

        // 4. Shovel Spade Blade (Large curved metallic scoop)
        float bx = 0.065f;
        float bz0 = 0.020f;
        float bz1 = -0.16f;
        float by0 = -1.10f;
        float by1 = -0.74f;
        float bu0 = 24 * inv;
        float bu1 = 31 * inv;
        float bv0 = 0 * inv;
        float bv1 = 15 * inv;

        putTexturedQuad(buffer, -bx, by0, bz0, bx, by0, bz0, bx, by1, bz1, -bx, by1, bz1, bu0, bv0, bu1, bv1, 0, 0, 1);   // Back spade scoop
        putTexturedQuad(buffer, bx, by0, bz0, -bx, by0, bz0, -bx, by1, bz1, bx, by1, bz1, bu0, bv0, bu1, bv1, 0, 0, -1);  // Front spade scoop
        putTexturedQuad(buffer, -bx, by0, bz0, -bx, by1, bz1, bx, by1, bz1, bx, by0, bz0, 31 * inv, bv0, 32 * inv, bv1, 0, 1, 0); // Top scoop edge (Glint)
        putTexturedQuad(buffer, -bx, by0, bz0, bx, by0, bz0, bx, by1, bz1, -bx, by1, bz1, bu0, bv0, bu1, 4 * inv, 0, -1, 0);

        buffer.flip();
        shovelHandVertexCount = buffer.limit() / 11;

        shovelHandVao = glGenVertexArrays();
        shovelHandVbo = glGenBuffers();

        glBindVertexArray(shovelHandVao);
        glBindBuffer(GL_ARRAY_BUFFER, shovelHandVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        setupVertexAttributes();

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    private void buildPickaxeHandMesh() {
        // Arm + 3D Iron Pickaxe Mesh (Bold, Prominent Arch)
        FloatBuffer buffer = MemoryUtil.memAllocFloat(384 * 11);
        float inv = 1.0f / TEX_SIZE;

        float hw = 0.08f;
        float hd = 0.08f;
        float yBottom = -0.48f;
        float yTop = 0.0f;

        // 1. Arm Faces (6 faces)
        putTexturedQuad(buffer, -hw, yTop, -hd, hw, yTop, -hd, hw, yTop, hd, -hw, yTop, hd, 4 * inv, 0 * inv, 8 * inv, 4 * inv, 0, 1, 0);
        putTexturedQuad(buffer, -hw, yBottom, hd, hw, yBottom, hd, hw, yBottom, -hd, -hw, yBottom, -hd, 8 * inv, 0 * inv, 12 * inv, 4 * inv, 0, -1, 0);
        putTexturedQuad(buffer, -hw, yBottom, hd, hw, yBottom, hd, hw, yTop, hd, -hw, yTop, hd, 4 * inv, 4 * inv, 8 * inv, 16 * inv, 0, 0, 1);
        putTexturedQuad(buffer, hw, yBottom, -hd, -hw, yBottom, -hd, -hw, yTop, -hd, hw, yTop, -hd, 12 * inv, 4 * inv, 16 * inv, 16 * inv, 0, 0, -1);
        putTexturedQuad(buffer, -hw, yBottom, -hd, -hw, yBottom, hd, -hw, yTop, hd, -hw, yTop, -hd, 8 * inv, 4 * inv, 12 * inv, 16 * inv, -1, 0, 0);
        putTexturedQuad(buffer, hw, yBottom, hd, hw, yBottom, -hd, hw, yTop, -hd, hw, yTop, hd, 0 * inv, 4 * inv, 4 * inv, 16 * inv, 1, 0, 0);

        // 2. Wooden Handle Shaft (Long slender oak stick through fist on far end)
        float hx = 0.024f;
        float hz = 0.024f;
        float hy0 = -0.88f;
        float hy1 = -0.20f;
        float hu0 = 16 * inv;
        float hu1 = 19 * inv;
        float hv0 = 0 * inv;
        float hv1 = 15 * inv;

        putTexturedQuad(buffer, -hx, hy0, hz, hx, hy0, hz, hx, hy1, hz, -hx, hy1, hz, hu0, hv0, hu1, hv1, 0, 0, 1);
        putTexturedQuad(buffer, hx, hy0, -hz, -hx, hy0, -hz, -hx, hy1, -hz, hx, hy1, -hz, hu0, hv0, hu1, hv1, 0, 0, -1);
        putTexturedQuad(buffer, -hx, hy0, -hz, -hx, hy0, hz, -hx, hy1, hz, -hx, hy1, -hz, hu0, hv0, hu1, hv1, -1, 0, 0);
        putTexturedQuad(buffer, hx, hy0, hz, hx, hy0, -hz, hx, hy1, -hz, hx, hy1, hz, hu0, hv0, hu1, hv1, 1, 0, 0);
        putTexturedQuad(buffer, -hx, hy1, -hz, hx, hy1, -hz, hx, hy1, hz, -hx, hy1, hz, hu0, hv0, hu1, 4 * inv, 0, 1, 0);
        putTexturedQuad(buffer, -hx, hy0, hz, hx, hy0, hz, hx, hy0, -hz, -hx, hy0, -hz, hu0, hv0, hu1, 4 * inv, 0, -1, 0);

        // 3. Central Socket Collar
        float sx = 0.040f;
        float sz = 0.040f;
        float sy0 = -0.84f;
        float sy1 = -0.68f;
        float su0 = 20 * inv;
        float su1 = 24 * inv;
        float sv0 = 0 * inv;
        float sv1 = 8 * inv;

        putTexturedQuad(buffer, -sx, sy0, sz, sx, sy0, sz, sx, sy1, sz, -sx, sy1, sz, su0, sv0, su1, sv1, 0, 0, 1);
        putTexturedQuad(buffer, sx, sy0, -sz, -sx, sy0, -sz, -sx, sy1, -sz, sx, sy1, -sz, su0, sv0, su1, sv1, 0, 0, -1);
        putTexturedQuad(buffer, -sx, sy0, -sz, -sx, sy0, sz, -sx, sy1, sz, -sx, sy1, -sz, su0, sv0, su1, sv1, -1, 0, 0);
        putTexturedQuad(buffer, sx, sy0, sz, sx, sy0, -sz, sx, sy1, -sz, sx, sy1, sz, su0, sv0, su1, sv1, 1, 0, 0);
        putTexturedQuad(buffer, -sx, sy1, -sz, sx, sy1, -sz, sx, sy1, sz, -sx, sy1, sz, su0, sv0, su1, sv1, 0, 1, 0);

        // 4. Front Curved Pick Horn (Extending forward -Z with sharp pointed downward tip)
        float px = 0.032f;
        float pz0 = -0.015f;
        float pz1 = -0.26f;
        float py0 = -0.84f;
        float py1 = -0.70f;
        float bu0 = 24 * inv;
        float bu1 = 31 * inv;
        float bv0 = 0 * inv;
        float bv1 = 15 * inv;

        putTexturedQuad(buffer, -px, py0, pz0, px, py0, pz0, px, py1, pz1, -px, py1, pz1, bu0, bv0, bu1, bv1, 0, 0, 1);
        putTexturedQuad(buffer, px, py0, pz0, -px, py0, pz0, -px, py1, pz1, px, py1, pz1, bu0, bv0, bu1, bv1, 0, 0, -1);
        putTexturedQuad(buffer, -px, py1, pz0, px, py1, pz0, px, py1, pz1, -px, py1, pz1, bu0, bv0, bu1, bv1, 0, 1, 0);
        putTexturedQuad(buffer, -px, py0, pz1, px, py0, pz1, px, py0, pz0, -px, py0, pz0, bu0, bv0, bu1, bv1, 0, -1, 0);

        // Front Sharp Tip (-Z curved downwards)
        float tx = 0.022f;
        float tz0 = -0.26f;
        float tz1 = -0.38f;
        float ty0 = -0.80f;
        float ty1 = -0.64f;
        float gu0 = 31 * inv;
        float gu1 = 32 * inv;

        putTexturedQuad(buffer, -tx, ty0, tz0, tx, ty0, tz0, tx, ty1, tz1, -tx, ty1, tz1, gu0, bv0, gu1, bv1, 0, 0, 1);
        putTexturedQuad(buffer, tx, ty0, tz0, -tx, ty0, tz0, -tx, ty1, tz1, tx, ty1, tz1, gu0, bv0, gu1, bv1, 0, 0, -1);
        putTexturedQuad(buffer, -tx, ty1, tz0, tx, ty1, tz0, tx, ty1, tz1, -tx, ty1, tz1, gu0, bv0, gu1, bv1, 0, 1, 0);
        putTexturedQuad(buffer, -tx, ty0, tz1, tx, ty0, tz1, tx, ty0, tz0, -tx, ty0, tz0, gu0, bv0, gu1, bv1, 0, -1, 0);

        // 5. Back Curved Pick Horn (Extending backward +Z with sharp downward tip)
        float bpz0 = 0.015f;
        float bpz1 = 0.26f;

        putTexturedQuad(buffer, -px, py0, bpz1, px, py0, bpz1, px, py1, bpz0, -px, py1, bpz0, bu0, bv0, bu1, bv1, 0, 0, 1);
        putTexturedQuad(buffer, px, py0, bpz1, -px, py0, bpz1, -px, py1, bpz0, px, py1, bpz0, bu0, bv0, bu1, bv1, 0, 0, -1);
        putTexturedQuad(buffer, -px, py1, bpz1, px, py1, bpz1, px, py1, bpz0, -px, py1, bpz0, bu0, bv0, bu1, bv1, 0, 1, 0);
        putTexturedQuad(buffer, -px, py0, bpz0, px, py0, bpz0, px, py0, bpz1, -px, py0, bpz1, bu0, bv0, bu1, bv1, 0, -1, 0);

        // Back Sharp Tip (+Z curved downwards)
        float btz0 = 0.26f;
        float btz1 = 0.38f;

        putTexturedQuad(buffer, -tx, ty0, btz1, tx, ty0, btz1, tx, ty1, btz0, -tx, ty1, btz0, gu0, bv0, gu1, bv1, 0, 0, 1);
        putTexturedQuad(buffer, tx, ty0, btz1, -tx, ty0, btz1, -tx, ty1, btz0, tx, ty1, btz0, gu0, bv0, gu1, bv1, 0, 0, -1);
        putTexturedQuad(buffer, -tx, ty1, btz1, tx, ty1, btz1, tx, ty1, btz0, -tx, ty1, btz0, gu0, bv0, gu1, bv1, 0, 1, 0);
        putTexturedQuad(buffer, -tx, ty0, btz0, tx, ty0, btz0, tx, ty0, btz1, -tx, ty0, btz1, gu0, bv0, gu1, bv1, 0, -1, 0);

        buffer.flip();
        pickaxeHandVertexCount = buffer.limit() / 11;

        pickaxeHandVao = glGenVertexArrays();
        pickaxeHandVbo = glGenBuffers();

        glBindVertexArray(pickaxeHandVao);
        glBindBuffer(GL_ARRAY_BUFFER, pickaxeHandVbo);
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

    public void setMining(boolean mining) {
        this.isMining = mining;
    }

    public void update(float deltaTime, Player player) {
        idleTime += deltaTime;

        // 1. Continuous Block Breaking Animation (Faster chopping/digging/mining arc when holding Tools)
        byte heldItem = player.getSelectedBlock();
        boolean isHoldingTool = Item.isTool(heldItem);
        float chopRate = isHoldingTool ? 6.5f : 4.8f;

        if (isMining) {
            miningTimer += deltaTime * chopRate;
            swingProgress = miningTimer % 1.0f;
        } else if (isSwinging) {
            // 2. Single Attack / Punch Swing Animation
            float swingRate = isHoldingTool ? 5.2f : 4.4f;
            swingProgress += deltaTime * swingRate;
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

    private void updateHeldBlockMesh(byte blockType) {
        if (blockType == currentHeldBlockType && heldBlockVao != 0) return;
        currentHeldBlockType = blockType;

        if (blockType == Block.AIR || Item.isTool(blockType)) {
            heldBlockVertexCount = 0;
            return;
        }

        FloatBuffer buffer = MemoryUtil.memAllocFloat(64 * 11);

        if (Block.isPlant(blockType)) {
            // Render 2D crossed quads in fist for flowers & tall grass on the far end
            int tile = Block.getDisplayFaceTile(blockType);
            float[] uv = TextureAtlas.getTileUV(tile);
            float u0 = uv[0], u1 = uv[1], v0 = uv[2], v1 = uv[3];

            float s = 0.22f;
            float hs = s / 2.0f;
            float by0 = -0.66f;
            float by1 = by0 + s;
            float bz = 0.0f;

            // Quad 1: Diagonal / (Double-sided)
            putTexturedQuad(buffer, -hs, by0, bz - hs, hs, by0, bz + hs, hs, by1, bz + hs, -hs, by1, bz - hs, u0, v0, u1, v1, 0, 1, 0);
            putTexturedQuad(buffer, hs, by0, bz + hs, -hs, by0, bz - hs, -hs, by1, bz - hs, hs, by1, bz + hs, u0, v0, u1, v1, 0, 1, 0);

            // Quad 2: Diagonal \ (Double-sided)
            putTexturedQuad(buffer, -hs, by0, bz + hs, hs, by0, bz - hs, hs, by1, bz - hs, -hs, by1, bz + hs, u0, v0, u1, v1, 0, 1, 0);
            putTexturedQuad(buffer, hs, by0, bz - hs, -hs, by0, bz + hs, -hs, by1, bz + hs, hs, by1, bz - hs, u0, v0, u1, v1, 0, 1, 0);
        } else {
            // Render authentic 3D miniature block cube held right in Steve's fist on the far end (Slightly larger, bold proportions)
            float s = 0.11f; // half size -> 0.22 width block
            float cx = -0.01f;
            float cy = -0.56f;
            float cz = -0.03f;

            float x0 = cx - s, x1 = cx + s;
            float y0 = cy - s, y1 = cy + s;
            float z0 = cz - s, z1 = cz + s;

            // 6 Faces with authentic Block TextureAtlas UVs
            for (int face = 0; face < 6; face++) {
                int slot = Block.getBlockFaceSlot(blockType, face);
                float[] uv = TextureAtlas.getTileUV(slot);
                float u0 = uv[0], u1 = uv[1], v0 = uv[2], v1 = uv[3];

                switch (face) {
                    case 0 -> // East (+X)
                        putTexturedQuad(buffer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1, 1, 0, 0);
                    case 1 -> // West (-X)
                        putTexturedQuad(buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1, -1, 0, 0);
                    case 2 -> // Top (+Y)
                        putTexturedQuad(buffer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1, 0, 1, 0);
                    case 3 -> // Bottom (-Y)
                        putTexturedQuad(buffer, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u0, v0, u1, v1, 0, -1, 0);
                    case 4 -> // South (+Z)
                        putTexturedQuad(buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, 0, 0, 1);
                    case 5 -> // North (-Z)
                        putTexturedQuad(buffer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1, 0, 0, -1);
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
        boolean isHoldingAxe = (heldItem == Item.IRON_AXE);
        boolean isHoldingShovel = (heldItem == Item.IRON_SHOVEL);
        boolean isHoldingPickaxe = (heldItem == Item.IRON_PICKAXE);
        boolean isHoldingTool = isHoldingAxe || isHoldingShovel || isHoldingPickaxe;
        boolean isHoldingBlock = (heldItem != Block.AIR && !isHoldingTool);

        if (isHoldingBlock) {
            updateHeldBlockMesh(heldItem);
        }

        // Base Viewmodel Anchor Position (Prominently framed in first-person view)
        float baseX = isHoldingTool ? 0.34f : (isHoldingBlock ? 0.38f : 0.44f);
        float baseY = isHoldingTool ? -0.28f : (isHoldingBlock ? -0.32f : -0.40f);
        float baseZ = isHoldingTool ? -0.44f : (isHoldingBlock ? -0.48f : -0.56f);

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

            // Forward-down chopping/digging/mining translation
            animX = -f1 * 0.18f; // Inward sweep towards crosshair center
            animY = -f2 * 0.18f; // Downward punch/chop trajectory
            animZ = -f2 * 0.22f; // Forward extension into target block

            // Downward and forward chopping/mining arc (Positive pitch swings forward-down)
            animPitch = f2 * 68.0f;
            animYaw = f1 * 26.0f;
            animRoll = -f2 * 22.0f;
        }

        // Setup Projection & View Matrices for First-Person Viewmodel
        viewmodelProjection.identity();
        viewmodelProjection.perspective((float) Math.toRadians(66.0f), aspectRatio, 0.01f, 20.0f);

        viewmodelView.identity(); // Anchored directly in front of eye camera

        // Build Model Matrix matching screenshot orientation
        modelMatrix.identity();

        // Translate to Hand Position
        modelMatrix.translate(baseX + bobX + animX, baseY + bobY + animY, baseZ + animZ);

        // Apply Rotations: Base Inward Yaw, Pitch, Roll + Active Animations
        float baseYaw = isHoldingTool ? -50.0f : (isHoldingBlock ? -48.0f : -45.0f);
        float basePitch = isHoldingTool ? 30.0f : (isHoldingBlock ? 28.0f : 24.0f);
        float baseRoll = isHoldingTool ? -34.0f : (isHoldingBlock ? -32.0f : -32.0f);

        modelMatrix.rotate((float) Math.toRadians(baseYaw + animYaw), new Vector3f(0, 1, 0));       // Yaw inward
        modelMatrix.rotate((float) Math.toRadians(basePitch + animPitch), new Vector3f(1, 0, 0));     // Pitch forward
        modelMatrix.rotate((float) Math.toRadians(baseRoll + bobRoll + animRoll), new Vector3f(0, 0, 1)); // Roll tilt

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

        // 1. Draw Steve Arm / Hand or Tool Mesh
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        handShader.setUniform("uTexture", 0);

        int currentVao;
        int currentCount;
        if (isHoldingAxe) {
            currentVao = axeHandVao;
            currentCount = axeHandVertexCount;
        } else if (isHoldingShovel) {
            currentVao = shovelHandVao;
            currentCount = shovelHandVertexCount;
        } else if (isHoldingPickaxe) {
            currentVao = pickaxeHandVao;
            currentCount = pickaxeHandVertexCount;
        } else {
            currentVao = armVao;
            currentCount = armVertexCount;
        }

        glBindVertexArray(currentVao);
        glDrawArrays(GL_TRIANGLES, 0, currentCount);
        glBindVertexArray(0);

        // 2. If holding a Block, draw the 3D Held Block in Steve's fist using the TextureAtlas
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
        if (axeHandVbo != 0) glDeleteBuffers(axeHandVbo);
        if (axeHandVao != 0) glDeleteVertexArrays(axeHandVao);
        if (shovelHandVbo != 0) glDeleteBuffers(shovelHandVbo);
        if (shovelHandVao != 0) glDeleteVertexArrays(shovelHandVao);
        if (pickaxeHandVbo != 0) glDeleteBuffers(pickaxeHandVbo);
        if (pickaxeHandVao != 0) glDeleteVertexArrays(pickaxeHandVao);
        if (heldBlockVbo != 0) glDeleteBuffers(heldBlockVbo);
        if (heldBlockVao != 0) glDeleteVertexArrays(heldBlockVao);
    }
}

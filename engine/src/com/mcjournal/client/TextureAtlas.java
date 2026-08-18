package com.mcjournal.client;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

public class TextureAtlas {
    public static final int ATLAS_GRID = 4; // 4x4 slots = 16 slots
    public static final int TILE_SIZE = 64; // 64x64 pixels per tile (Faithful 64x)
    public static final int ATLAS_SIZE = ATLAS_GRID * TILE_SIZE; // 256x256 pixels

    private int textureId;
    private final Map<Integer, String> slotFiles = new HashMap<>();
    private String currentDir = "public/texturepacks/faithful64x";

    public TextureAtlas() {
        initSlotMap();
    }

    private void initSlotMap() {
        slotFiles.put(0, "grass_block_top.png");
        slotFiles.put(1, "grass_block_side.png");
        slotFiles.put(2, "dirt.png");
        slotFiles.put(3, "stone.png");
        slotFiles.put(4, "cobblestone.png");
        slotFiles.put(5, "sand.png");
        slotFiles.put(6, "bedrock.png");
        slotFiles.put(7, "oak_log.png");
        slotFiles.put(8, "oak_log_top.png");
        slotFiles.put(9, "oak_leaves.png");
        slotFiles.put(10, "diamond_ore.png");
        slotFiles.put(11, "water.png");
        slotFiles.put(12, "short_grass.png");
        slotFiles.put(13, "poppy.png");
        slotFiles.put(14, "dandelion.png");
        slotFiles.put(15, "birch_log.png");
    }

    public void load(String texturesDir) {
        this.currentDir = texturesDir;
        loadAtlas(0);
    }

    public void switchPack(int packIndex) {
        loadAtlas(packIndex);
        System.out.println("[TextureAtlas] Switched to Texture Pack Variant #" + packIndex);
    }

    private void loadAtlas(int variant) {
        ByteBuffer atlasBuffer = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);

        // Fill with default opaque background
        for (int i = 0; i < ATLAS_SIZE * ATLAS_SIZE * 4; i += 4) {
            atlasBuffer.put(i, (byte) 120);     // R
            atlasBuffer.put(i + 1, (byte) 120); // G
            atlasBuffer.put(i + 2, (byte) 120); // B
            atlasBuffer.put(i + 3, (byte) 255); // A
        }

        // Standard OpenGL bottom-up texture flip
        stbi_set_flip_vertically_on_load(true);

        for (Map.Entry<Integer, String> entry : slotFiles.entrySet()) {
            int slot = entry.getKey();
            String fileName = entry.getValue();

            int col = slot % ATLAS_GRID;
            int row = slot / ATLAS_GRID;

            int slotX = col * TILE_SIZE;
            int slotY = ((ATLAS_GRID - 1) - row) * TILE_SIZE;

            File file = new File(currentDir, fileName);
            if (!file.exists()) {
                file = new File("public/texturepacks/faithful64x", fileName);
            }

            if (file.exists()) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    IntBuffer comp = stack.mallocInt(1);

                    ByteBuffer image = stbi_load(file.getAbsolutePath(), w, h, comp, 4);
                    if (image != null) {
                        int imgW = w.get(0);
                        int imgH = h.get(0);

                        for (int y = 0; y < Math.min(imgH, TILE_SIZE); y++) {
                            for (int x = 0; x < Math.min(imgW, TILE_SIZE); x++) {
                                int srcIdx = (y * imgW + x) * 4;
                                int dstIdx = ((slotY + y) * ATLAS_SIZE + (slotX + x)) * 4;

                                byte r = image.get(srcIdx);
                                byte g = image.get(srcIdx + 1);
                                byte b = image.get(srcIdx + 2);
                                byte a = image.get(srcIdx + 3);

                                if (variant == 1) {
                                    // Stylized Clean: vibrant saturated tones
                                    r = (byte) Math.min(255, (r & 0xFF) * 1.15f);
                                    g = (byte) Math.min(255, (g & 0xFF) * 1.15f);
                                    b = (byte) Math.min(255, (b & 0xFF) * 1.15f);
                                } else if (variant == 2) {
                                    // Classic Retro 16x: pixelated 4x4 downsample effect
                                    int px = (x / 4) * 4;
                                    int py = (y / 4) * 4;
                                    int pSrcIdx = (py * imgW + px) * 4;
                                    r = image.get(pSrcIdx);
                                    g = image.get(pSrcIdx + 1);
                                    b = image.get(pSrcIdx + 2);
                                    a = image.get(pSrcIdx + 3);
                                }

                                atlasBuffer.put(dstIdx, r);
                                atlasBuffer.put(dstIdx + 1, g);
                                atlasBuffer.put(dstIdx + 2, b);
                                atlasBuffer.put(dstIdx + 3, a);
                            }
                        }
                        stbi_image_free(image);
                    }
                }
            }
        }

        atlasBuffer.flip();

        if (textureId == 0) {
            textureId = glGenTextures();
        }

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        MemoryUtil.memFree(atlasBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }

    public int getTextureId() {
        return textureId;
    }
}

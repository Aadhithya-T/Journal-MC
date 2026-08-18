package com.mcjournal.client;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.InputStream;
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
        ByteBuffer atlasBuffer = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);

        // Fill with default background color
        for (int i = 0; i < ATLAS_SIZE * ATLAS_SIZE * 4; i += 4) {
            atlasBuffer.put(i, (byte) 120);     // R
            atlasBuffer.put(i + 1, (byte) 120); // G
            atlasBuffer.put(i + 2, (byte) 120); // B
            atlasBuffer.put(i + 3, (byte) 255); // A
        }

        stbi_set_flip_vertically_on_load(false);

        for (Map.Entry<Integer, String> entry : slotFiles.entrySet()) {
            int slot = entry.getKey();
            String fileName = entry.getValue();

            int slotX = (slot % ATLAS_GRID) * TILE_SIZE;
            int slotY = (slot / ATLAS_GRID) * TILE_SIZE;

            File file = new File(texturesDir, fileName);
            if (!file.exists()) {
                // Try fallback relative path
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

                                atlasBuffer.put(dstIdx, image.get(srcIdx));         // R
                                atlasBuffer.put(dstIdx + 1, image.get(srcIdx + 1)); // G
                                atlasBuffer.put(dstIdx + 2, image.get(srcIdx + 2)); // B
                                atlasBuffer.put(dstIdx + 3, image.get(srcIdx + 3)); // A
                            }
                        }
                        stbi_image_free(image);
                    }
                }
            }
        }

        atlasBuffer.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        MemoryUtil.memFree(atlasBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        System.out.println("[TextureAtlas] OpenGL 256x256 Faithful 64x Texture Atlas Loaded (Texture ID: " + textureId + ")");
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

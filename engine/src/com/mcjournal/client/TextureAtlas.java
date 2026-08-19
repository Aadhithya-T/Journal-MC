package com.mcjournal.client;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class TextureAtlas {
    public static final int ATLAS_GRID = 4; // 4x4 tiles = 16 block types
    public static final int ATLAS_SIZE = ATLAS_GRID * ProceduralTextureGenerator.TILE_SIZE; // 256x256 atlas
    public static final float TILE_UV_SIZE = 1.0f / ATLAS_GRID; // 0.25 UV per block

    private static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;

    private int textureId;

    public void init() {
        ByteBuffer atlasBuffer = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);

        // Generate procedural pixel-art atlas in memory
        ProceduralTextureGenerator.generateAtlas(atlasBuffer, ATLAS_GRID);

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Enable 8x/16x Anisotropic Filtering if supported
        try {
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, 8.0f);
        } catch (Exception ignored) {}

        // Upload as sRGB texture: GPU automatically converts to Linear RGB during texture sampling
        glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        MemoryUtil.memFree(atlasBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        System.out.println("[TextureAtlas] Procedural 64x Pixel-Art Atlas Generated & Uploaded (Texture ID: " + textureId + ")");
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

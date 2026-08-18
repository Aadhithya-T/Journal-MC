package com.mcjournal.client;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class TextureAtlas {
    public static final int ATLAS_GRID = 4; // 4x4 slots = 16 slots
    public static final int TILE_SIZE = 64; // 64x64 pixels per tile
    public static final int ATLAS_SIZE = ATLAS_GRID * TILE_SIZE; // 256x256 pixels

    private int textureId;

    public void init() {
        ByteBuffer atlasBuffer = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);

        // Generate 100% Procedural Pixel-Art Texture Atlas
        ProceduralTextureGenerator.generateAtlas(atlasBuffer, ATLAS_GRID);

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

package com.mcjournal.client;

import com.mcjournal.client.gui.GuiRenderer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.stb.STBImage.*;

public class VideoBackgroundManager {
    public static final String[] VIDEO_NAMES = {
        "aurora-night",
        "coral-reef",
        "cozy-campfire"
    };

    public static final String[] DISPLAY_NAMES = {
        "Aurora Night",
        "Coral Reef",
        "Cozy Campfire"
    };

    private final int sessionIndex;
    private final String selectedVideoName;
    private final String selectedDisplayName;

    private int textureId;
    private final List<ByteBuffer> frameBuffers = new ArrayList<>();
    private int frameWidth = 640;
    private int frameHeight = 360;
    private int totalFrames = 0;

    private double frameTimer = 0;
    private int currentFrame = 0;
    private static final double FRAME_DURATION = 1.0 / 20.0; // 20 FPS video playback

    public VideoBackgroundManager() {
        // Randomly select one of the 3 video files in the folder for this entire session
        this.sessionIndex = new Random().nextInt(VIDEO_NAMES.length);
        this.selectedVideoName = VIDEO_NAMES[sessionIndex];
        this.selectedDisplayName = DISPLAY_NAMES[sessionIndex];
        System.out.println("[VideoBackground] 🎬 Selected Session Video: " + selectedDisplayName + " (" + selectedVideoName + ".mp4 - fixed for session, loops infinitely)");
    }

    public void init() {
        File dir = new File("engine/resources/backgrounds/" + selectedVideoName);
        if (!dir.exists()) {
            dir = new File("resources/backgrounds/" + selectedVideoName);
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith("frame_") && name.endsWith(".jpg"));
        if (files != null && files.length > 0) {
            java.util.Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));

            stbi_set_flip_vertically_on_load(false);

            for (File frameFile : files) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    IntBuffer comp = stack.mallocInt(1);

                    ByteBuffer image = stbi_load(frameFile.getAbsolutePath(), w, h, comp, 4);
                    if (image != null) {
                        this.frameWidth = w.get(0);
                        this.frameHeight = h.get(0);
                        frameBuffers.add(image);
                    }
                }
            }
            this.totalFrames = frameBuffers.size();
            System.out.println("[VideoBackground] Loaded " + totalFrames + " video frames for " + selectedVideoName + ".mp4 (" + frameWidth + "x" + frameHeight + ")");
        }

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        if (totalFrames > 0) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, frameWidth, frameHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, frameBuffers.get(0));
        }
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void update(double deltaTime) {
        if (totalFrames <= 0) return;

        frameTimer += deltaTime;
        if (frameTimer >= FRAME_DURATION) {
            int advance = (int) (frameTimer / FRAME_DURATION);
            currentFrame = (currentFrame + advance) % totalFrames; // Infinite video loop
            frameTimer -= advance * FRAME_DURATION;

            // Upload current frame to GPU
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, frameWidth, frameHeight, GL_RGBA, GL_UNSIGNED_BYTE, frameBuffers.get(currentFrame));
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    public void render(GuiRenderer gui, int screenWidth, int screenHeight) {
        if (textureId != 0 && totalFrames > 0) {
            gui.drawTexturedQuad(textureId, 0, 0, screenWidth, screenHeight, 0, 0, 1, 1, 1, 1, 1, 1);
        }
    }

    public String getCurrentThemeName() {
        return selectedDisplayName;
    }

    public String getSelectedVideoName() {
        return selectedVideoName;
    }

    public void cleanup() {
        for (ByteBuffer buf : frameBuffers) {
            stbi_image_free(buf);
        }
        frameBuffers.clear();
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }
}

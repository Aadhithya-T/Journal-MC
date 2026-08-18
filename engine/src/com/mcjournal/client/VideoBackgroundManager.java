package com.mcjournal.client;

import com.mcjournal.client.gui.GuiRenderer;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
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

    private ShaderProgram videoShader;
    private int vao;
    private int vbo;

    private int textureId0;
    private int textureId1;

    private final List<ByteBuffer> frameBuffers = new ArrayList<>();
    private int frameWidth = 1280;
    private int frameHeight = 720;
    private int totalFrames = 0;

    private double playbackTime = 0;
    private int lastUploadedFrame0 = -1;
    private int lastUploadedFrame1 = -1;

    // 20 FPS video source smoothly interpolated to 60+ FPS monitor refresh
    private static final double FPS = 20.0;
    private static final double FRAME_DURATION = 1.0 / FPS;

    public VideoBackgroundManager() {
        this.sessionIndex = new Random().nextInt(VIDEO_NAMES.length);
        this.selectedVideoName = VIDEO_NAMES[sessionIndex];
        this.selectedDisplayName = DISPLAY_NAMES[sessionIndex];
        System.out.println("[VideoBackground] 🎬 Selected Session HD Video: " + selectedDisplayName + " (" + selectedVideoName + ".mp4 - HD 1280x720)");
    }

    public void init() {
        // 1. Load Shaders
        this.videoShader = new ShaderProgram("/shaders/video_background_vertex.glsl", "/shaders/video_background_fragment.glsl");

        // 2. Full-Screen Quad (-1..1 in NDC)
        initQuad();

        // 3. Load all HD Frames for the selected video
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
            System.out.println("[VideoBackground] Loaded " + totalFrames + " HD Video Frames for " + selectedVideoName + " (" + frameWidth + "x" + frameHeight + ")");
        }

        // 4. Create Dual GPU Textures for Smooth Sub-Frame Blending
        textureId0 = createVideoTexture();
        textureId1 = createVideoTexture();

        if (totalFrames > 0) {
            uploadFrameToTexture(textureId0, 0);
            uploadFrameToTexture(textureId1, Math.min(1, totalFrames - 1));
            lastUploadedFrame0 = 0;
            lastUploadedFrame1 = Math.min(1, totalFrames - 1);
        }
    }

    private void initQuad() {
        float[] quadVertices = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
             1.0f, -1.0f,

            -1.0f,  1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(quadVertices.length);
        vertexBuffer.put(quadVertices).flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
    }

    private int createVideoTexture() {
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        if (frameBuffers.size() > 0) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, frameWidth, frameHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, frameBuffers.get(0));
        }
        glBindTexture(GL_TEXTURE_2D, 0);
        return texId;
    }

    private void uploadFrameToTexture(int texId, int frameIndex) {
        if (frameIndex >= 0 && frameIndex < totalFrames) {
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, frameWidth, frameHeight, GL_RGBA, GL_UNSIGNED_BYTE, frameBuffers.get(frameIndex));
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    public void update(double deltaTime) {
        if (totalFrames <= 0) return;

        playbackTime += deltaTime;
        double totalDuration = totalFrames * FRAME_DURATION;
        double loopedTime = playbackTime % totalDuration;

        double currentFrameFloat = loopedTime / FRAME_DURATION;
        int frame0 = (int) Math.floor(currentFrameFloat) % totalFrames;
        int frame1 = (frame0 + 1) % totalFrames;

        if (frame0 != lastUploadedFrame0) {
            uploadFrameToTexture(textureId0, frame0);
            lastUploadedFrame0 = frame0;
        }

        if (frame1 != lastUploadedFrame1) {
            uploadFrameToTexture(textureId1, frame1);
            lastUploadedFrame1 = frame1;
        }
    }

    public void render(int screenWidth, int screenHeight) {
        if (totalFrames <= 0 || textureId0 == 0) return;

        double totalDuration = totalFrames * FRAME_DURATION;
        double loopedTime = playbackTime % totalDuration;
        double currentFrameFloat = loopedTime / FRAME_DURATION;
        float blendFactor = (float) (currentFrameFloat - Math.floor(currentFrameFloat));

        // Aspect Ratio "Cover" calculation
        float screenAspect = (float) screenWidth / (float) screenHeight;
        float videoAspect = (float) frameWidth / (float) frameHeight;

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (screenAspect > videoAspect) {
            scaleY = screenAspect / videoAspect;
        } else {
            scaleX = videoAspect / screenAspect;
        }

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        videoShader.bind();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId0);
        videoShader.setUniform("uTex0", 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, textureId1);
        videoShader.setUniform("uTex1", 1);

        videoShader.setUniform("uBlend", blendFactor);
        videoShader.setUniform("uUvScale", new Vector2f(scaleX, scaleY));
        videoShader.setUniform("uUvOffset", new Vector2f(0.0f, 0.0f));

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);

        videoShader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public String getCurrentThemeName() {
        return selectedDisplayName;
    }

    public void cleanup() {
        if (videoShader != null) videoShader.cleanup();
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);

        for (ByteBuffer buf : frameBuffers) {
            stbi_image_free(buf);
        }
        frameBuffers.clear();

        if (textureId0 != 0) glDeleteTextures(textureId0);
        if (textureId1 != 0) glDeleteTextures(textureId1);
    }
}

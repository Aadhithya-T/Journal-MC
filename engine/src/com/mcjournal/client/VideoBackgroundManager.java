package com.mcjournal.client;

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

    private int textureArrayId;
    private int frameWidth = 1280;
    private int frameHeight = 720;
    private int totalFrames = 0;

    private double playbackTime = 0;

    // 20 FPS video source smoothly interpolated across GPU monitor refresh
    private static final double FPS = 20.0;
    private static final double FRAME_DURATION = 1.0 / FPS;

    public VideoBackgroundManager() {
        this.sessionIndex = new Random().nextInt(VIDEO_NAMES.length);
        this.selectedVideoName = VIDEO_NAMES[sessionIndex];
        this.selectedDisplayName = DISPLAY_NAMES[sessionIndex];
        System.out.println("[VideoBackground] 🎬 Selected Session HD Video: " + selectedDisplayName + " (" + selectedVideoName + ".mp4 - GPU Texture Array)");
    }

    public void init() {
        // 1. Load Shaders
        this.videoShader = new ShaderProgram("/shaders/video_background_vertex.glsl", "/shaders/video_background_fragment.glsl");

        // 2. Full-Screen Quad (-1..1 in NDC)
        initQuad();

        // 3. Load all HD Frames from disk into memory
        List<ByteBuffer> loadedBuffers = new ArrayList<>();
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
                        loadedBuffers.add(image);
                    }
                }
            }
            this.totalFrames = loadedBuffers.size();
        }

        // 4. Pre-Upload All Frames into a single OpenGL 2D Texture Array on GPU VRAM
        if (totalFrames > 0) {
            textureArrayId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D_ARRAY, textureArrayId);

            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, frameWidth, frameHeight, totalFrames, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            for (int i = 0; i < totalFrames; i++) {
                glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, frameWidth, frameHeight, 1, GL_RGBA, GL_UNSIGNED_BYTE, loadedBuffers.get(i));
                stbi_image_free(loadedBuffers.get(i)); // Immediately free CPU RAM!
            }
            loadedBuffers.clear();

            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
            System.out.println("[VideoBackground] ⚡ Uploaded " + totalFrames + " HD frames to GPU VRAM Texture Array. CPU RAM freed. Zero PCI-e stalls.");
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

    public void update(double deltaTime) {
        if (totalFrames <= 0) return;
        playbackTime += deltaTime;
    }

    public void render(int screenWidth, int screenHeight) {
        if (totalFrames <= 0 || textureArrayId == 0) return;

        double totalDuration = totalFrames * FRAME_DURATION;
        double loopedTime = playbackTime % totalDuration;
        double currentFrameFloat = loopedTime / FRAME_DURATION;

        int frame0 = (int) Math.floor(currentFrameFloat) % totalFrames;
        int frame1 = (frame0 + 1) % totalFrames;
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
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureArrayId);
        videoShader.setUniform("uVideoArray", 0);

        videoShader.setUniform("uFrame0", (float) frame0);
        videoShader.setUniform("uFrame1", (float) frame1);
        videoShader.setUniform("uBlend", blendFactor);
        videoShader.setUniform("uUvScale", new Vector2f(scaleX, scaleY));
        videoShader.setUniform("uUvOffset", new Vector2f(0.0f, 0.0f));

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
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
        if (textureArrayId != 0) glDeleteTextures(textureArrayId);
    }
}

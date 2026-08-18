package com.mcjournal.client;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkMeshBuilder;
import org.joml.Vector3f;

import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class MCJournalApp {
    private final Window window;
    private final InputHandler input;
    private final Camera camera;
    private final TextureAtlas atlas;
    private final ChunkRenderer chunkRenderer;
    private ChunkManager chunkManager;
    private ShaderProgram chunkShader;

    // Fixed 20-TPS Tick timing
    private static final double TICK_DURATION = 0.050; // 50ms = 20 TPS
    private double lastFrameTime = 0;
    private double tickAccumulator = 0;

    public MCJournalApp() {
        this.window = new Window("Minecraft Journal - Native Java & OpenGL Edition", 1280, 760);
        this.input = new InputHandler();
        this.camera = new Camera();
        this.atlas = new TextureAtlas();
        this.chunkRenderer = new ChunkRenderer();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window.init();
        input.init(window.getHandle());

        // Lock cursor by default for FPS camera look
        window.lockCursor(true);

        camera.setPosition(6, 9, -10);
        camera.updateProjection(window.getAspectRatio());

        // 1. Load Shaders
        chunkShader = new ShaderProgram("/shaders/chunk_vertex.glsl", "/shaders/chunk_fragment.glsl");

        // 2. Load Faithful 64x Texture Atlas
        atlas.load("public/texturepacks/faithful64x");

        // 3. Initialize 100-Chunk Voxel World in Parallel (Java 26 Virtual Threads)
        System.out.println("[MCJournalApp] Generating 100-chunk voxel world with Java 26 Loom virtual threads...");
        chunkManager = new ChunkManager(5, 4242);

        // 4. Upload all 100 precomputed meshes to OpenGL GPU VAO/VBOs
        Map<String, ChunkMeshBuilder.MeshData> meshes = chunkManager.getAllMeshes();
        for (Map.Entry<String, ChunkMeshBuilder.MeshData> entry : meshes.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            chunkRenderer.uploadChunkMesh(cx, cz, entry.getValue());
        }

        System.out.println("[MCJournalApp] 🚀 Uploaded " + meshes.size() + " Chunks to GPU VAOs!");

        lastFrameTime = glfwGetTime();
        System.out.println("[MCJournalApp] Native OpenGL 3.3 Engine Ready!");
    }

    private void loop() {
        while (!window.shouldClose()) {
            double currentFrameTime = glfwGetTime();
            double deltaTime = Math.min(currentFrameTime - lastFrameTime, 0.1);
            lastFrameTime = currentFrameTime;

            tickAccumulator += deltaTime;

            // Handle Input & Free/Lock Cursor
            handleInput((float) deltaTime);

            // Run Fixed 20-TPS Game Ticks
            while (tickAccumulator >= TICK_DURATION) {
                tick();
                tickAccumulator -= TICK_DURATION;
            }

            // Render Frame
            render((float) deltaTime);

            window.update();
        }
    }

    private void handleInput(float deltaTime) {
        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            window.lockCursor(!window.isCursorLocked());
        }

        if (window.isCursorLocked()) {
            double mouseDx = input.consumeMouseDeltaX();
            double mouseDy = input.consumeMouseDeltaY();

            float sensitivity = 0.12f;
            camera.setYaw(camera.getYaw() + (float) (mouseDx * sensitivity));
            camera.setPitch(camera.getPitch() + (float) (mouseDy * sensitivity));

            // Free-fly / Spectator locomotion
            float speed = input.isKeyDown(GLFW_KEY_LEFT_CONTROL) ? 16.0f : 6.5f;
            float moveStep = speed * deltaTime;

            Vector3f lookDir = camera.getLookDirection();
            Vector3f rightDir = new Vector3f(lookDir.z, 0, -lookDir.x).normalize();

            Vector3f pos = camera.getPosition();

            if (input.isKeyDown(GLFW_KEY_W)) {
                pos.add(lookDir.x * moveStep, lookDir.y * moveStep, lookDir.z * moveStep);
            }
            if (input.isKeyDown(GLFW_KEY_S)) {
                pos.sub(lookDir.x * moveStep, lookDir.y * moveStep, lookDir.z * moveStep);
            }
            if (input.isKeyDown(GLFW_KEY_A)) {
                pos.add(rightDir.x * moveStep, 0, rightDir.z * moveStep);
            }
            if (input.isKeyDown(GLFW_KEY_D)) {
                pos.sub(rightDir.x * moveStep, 0, rightDir.z * moveStep);
            }
            if (input.isKeyDown(GLFW_KEY_SPACE)) {
                pos.y += moveStep;
            }
            if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                pos.y -= moveStep;
            }
        }
    }

    private void tick() {
        // Fixed 20-TPS game logic
    }

    private void render(float deltaTime) {
        if (window.isResized()) {
            camera.updateProjection(window.getAspectRatio());
            window.setResized(false);
        }

        camera.updateView();

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Bind Shader & Textures
        chunkShader.bind();
        atlas.bind(0);
        chunkShader.setUniform("uAtlas", 0);
        chunkShader.setUniform("uProjection", camera.getProjectionMatrix());
        chunkShader.setUniform("uView", camera.getViewMatrix());
        chunkShader.setUniform("uFogColor", new Vector3f(0.47f, 0.65f, 1.0f));
        chunkShader.setUniform("uFogStart", 25.0f);
        chunkShader.setUniform("uFogEnd", 120.0f);

        // 1. Render Solid Opaque Chunks
        chunkRenderer.renderSolid();

        // 2. Render Translucent Water Surfaces
        chunkRenderer.renderWater();

        chunkShader.unbind();
        atlas.unbind();
    }

    private void cleanup() {
        if (chunkShader != null) chunkShader.cleanup();
        if (atlas != null) atlas.cleanup();
        if (chunkRenderer != null) chunkRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new MCJournalApp().run();
    }
}

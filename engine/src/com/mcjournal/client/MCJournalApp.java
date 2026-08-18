package com.mcjournal.client;

import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkMeshBuilder;
import com.mcjournal.client.gui.*;
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
    private final GuiRenderer guiRenderer;
    private final FontRenderer fontRenderer;

    private ChunkManager chunkManager;
    private ShaderProgram chunkShader;

    private Screen currentScreen;
    private boolean isInWorld = false;

    // Fixed 20-TPS Tick timing
    private static final double TICK_DURATION = 0.050; // 50ms = 20 TPS
    private double lastFrameTime = 0;
    private double tickAccumulator = 0;

    public MCJournalApp() {
        this.window = new Window("Minecraft Journal - Native Java Edition", 1280, 760);
        this.input = new InputHandler();
        this.camera = new Camera();
        this.atlas = new TextureAtlas();
        this.chunkRenderer = new ChunkRenderer();
        this.guiRenderer = new GuiRenderer();
        this.fontRenderer = new FontRenderer();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        window.init();
        input.init(window.getHandle(), this);

        fontRenderer.init();

        camera.setPosition(6, 9, -10);
        camera.updateProjection(window.getAspectRatio());

        // 1. Load Shaders
        chunkShader = new ShaderProgram("/shaders/chunk_vertex.glsl", "/shaders/chunk_fragment.glsl");

        // 2. Load Faithful 64x Texture Atlas
        atlas.load("public/texturepacks/faithful64x");

        // 3. Start on Title Screen
        setScreen(new TitleScreen(this));

        lastFrameTime = glfwGetTime();
        System.out.println("[MCJournalApp] Native OpenGL 3.3 Engine & GUI Ready!");
    }

    public void setScreen(Screen screen) {
        this.currentScreen = screen;
        if (screen != null) {
            window.lockCursor(false);
            screen.init(window.getWidth(), window.getHeight());
        } else {
            window.lockCursor(true);
        }
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public Window getWindow() {
        return window;
    }

    public void enterWorld(long seed, String worldName, String biome) {
        System.out.println("[MCJournalApp] Generating 100-chunk world: '" + worldName + "' (Seed: " + seed + ", Biome: " + biome + ")...");
        this.chunkManager = new ChunkManager(5, seed);

        // Upload all 100 precomputed meshes to OpenGL GPU VAO/VBOs
        Map<String, ChunkMeshBuilder.MeshData> meshes = chunkManager.getAllMeshes();
        for (Map.Entry<String, ChunkMeshBuilder.MeshData> entry : meshes.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            chunkRenderer.uploadChunkMesh(cx, cz, entry.getValue());
        }

        this.isInWorld = true;
        setScreen(null); // Switch directly into 3D gameplay!
        System.out.println("[MCJournalApp] 🚀 Spawned into World!");
    }

    private void loop() {
        while (!window.shouldClose()) {
            double currentFrameTime = glfwGetTime();
            double deltaTime = Math.min(currentFrameTime - lastFrameTime, 0.1);
            lastFrameTime = currentFrameTime;

            tickAccumulator += deltaTime;

            if (window.isResized()) {
                camera.updateProjection(window.getAspectRatio());
                if (currentScreen != null) {
                    currentScreen.init(window.getWidth(), window.getHeight());
                }
                window.setResized(false);
            }

            // Handle Input & Screen Updates
            if (currentScreen != null) {
                currentScreen.update(input.getMouseX(), input.getMouseY());
            } else {
                handleInGameInput((float) deltaTime);
            }

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

    private void handleInGameInput(float deltaTime) {
        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            setScreen(new TitleScreen(this));
            return;
        }

        if (window.isCursorLocked()) {
            double mouseDx = input.consumeMouseDeltaX();
            double mouseDy = input.consumeMouseDeltaY();

            float sensitivity = 0.12f;
            camera.setYaw(camera.getYaw() + (float) (mouseDx * sensitivity));
            camera.setPitch(camera.getPitch() + (float) (mouseDy * sensitivity));

            // Locomotion
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
        // Fixed 20-TPS tick logic
    }

    private void render(float deltaTime) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (isInWorld && currentScreen == null) {
            // --- RENDER 3D VOXEL WORLD ---
            camera.updateView();

            chunkShader.bind();
            atlas.bind(0);
            chunkShader.setUniform("uAtlas", 0);
            chunkShader.setUniform("uProjection", camera.getProjectionMatrix());
            chunkShader.setUniform("uView", camera.getViewMatrix());
            chunkShader.setUniform("uFogColor", new Vector3f(0.47f, 0.65f, 1.0f));
            chunkShader.setUniform("uFogStart", 25.0f);
            chunkShader.setUniform("uFogEnd", 120.0f);

            chunkRenderer.renderSolid();
            chunkRenderer.renderWater();

            chunkShader.unbind();
            atlas.unbind();

            // Render In-Game Crosshair Overlay
            guiRenderer.begin(window.getWidth(), window.getHeight());
            int cx = window.getWidth() / 2;
            int cy = window.getHeight() / 2;
            guiRenderer.drawRect(cx - 8, cy - 1, 16, 2, 1.0f, 1.0f, 1.0f, 0.85f);
            guiRenderer.drawRect(cx - 1, cy - 8, 2, 16, 1.0f, 1.0f, 1.0f, 0.85f);

            // Coordinates Display
            Vector3f p = camera.getPosition();
            String coords = String.format("XYZ: %.1f / %.1f / %.1f", p.x, p.y, p.z);
            fontRenderer.drawString(guiRenderer, coords, 12, 12, 0.85f, 0xffffff, true);
            fontRenderer.drawString(guiRenderer, "ESC: Menu", 12, 32, 0.75f, 0xaaaaaa, true);
            guiRenderer.end();
        }

        // --- RENDER ACTIVE GUI MENU ---
        if (currentScreen != null) {
            guiRenderer.begin(window.getWidth(), window.getHeight());
            currentScreen.render(guiRenderer, fontRenderer, input.getMouseX(), input.getMouseY(), deltaTime);
            guiRenderer.end();
        }
    }

    private void cleanup() {
        if (chunkShader != null) chunkShader.cleanup();
        if (atlas != null) atlas.cleanup();
        if (chunkRenderer != null) chunkRenderer.cleanup();
        if (guiRenderer != null) guiRenderer.cleanup();
        if (fontRenderer != null) fontRenderer.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new MCJournalApp().run();
    }
}

package com.mcjournal.client;

import com.mcjournal.Block;
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
    private final Player player;

    private TextureAtlas atlas;
    private ChunkRenderer chunkRenderer;
    private GuiRenderer guiRenderer;
    private FontRenderer fontRenderer;
    private HardcoreHUD hud;

    private ChunkManager chunkManager;
    private ShaderProgram chunkShader;
    private String currentBiome = "Plains";

    private Screen currentScreen;
    private boolean isInWorld = false;

    // Fixed 20-TPS Tick timing
    private static final double TICK_DURATION = 0.050; // 50ms = 20 TPS
    private double lastFrameTime = 0;
    private double tickAccumulator = 0;

    public MCJournalApp() {
        this.window = new Window("Minecraft Journal - Native Hardcore Edition", 1280, 760);
        this.input = new InputHandler();
        this.camera = new Camera();
        this.player = new Player();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // 1. Initialize GLFW Window & OpenGL Context FIRST
        window.init();
        input.init(window.getHandle(), this);

        // 2. Initialize OpenGL GPU Resources AFTER context is active
        guiRenderer = new GuiRenderer();
        fontRenderer = new FontRenderer();
        fontRenderer.init();
        hud = new HardcoreHUD();

        atlas = new TextureAtlas();
        chunkRenderer = new ChunkRenderer();

        camera.updateProjection(window.getAspectRatio());

        // 3. Load Shaders
        chunkShader = new ShaderProgram("/shaders/chunk_vertex.glsl", "/shaders/chunk_fragment.glsl");

        // 4. Load Faithful 64x Texture Atlas
        atlas.load("public/texturepacks/faithful64x");

        // 5. Start on Title Screen
        setScreen(new TitleScreen(this));

        lastFrameTime = glfwGetTime();
        System.out.println("[MCJournalApp] Native OpenGL 3.3 Hardcore Engine Ready!");
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
        this.currentBiome = biome;
        System.out.println("[MCJournalApp] Generating 100-chunk Hardcore world: '" + worldName + "' (Seed: " + seed + ")...");
        this.chunkManager = new ChunkManager(5, seed);

        // Upload all 100 precomputed meshes to OpenGL GPU VAO/VBOs
        Map<String, ChunkMeshBuilder.MeshData> meshes = chunkManager.getAllMeshes();
        for (Map.Entry<String, ChunkMeshBuilder.MeshData> entry : meshes.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            chunkRenderer.uploadChunkMesh(cx, cz, entry.getValue());
        }

        // Find safe spawn point above ground at (8, 8)
        int spawnX = 8;
        int spawnZ = 8;
        int spawnY = 16;
        for (int y = 31; y >= 0; y--) {
            if (Block.isSolid(chunkManager.getBlockAt(spawnX, y, spawnZ))) {
                spawnY = y + 2;
                break;
            }
        }

        player.pos.set(spawnX, spawnY, spawnZ);
        player.prevPos.set(spawnX, spawnY, spawnZ);
        player.velocity.set(0, 0, 0);
        player.health = 20;
        player.hunger = 20;
        player.isDead = false;

        this.isInWorld = true;
        setScreen(null); // Switch directly into 3D Hardcore gameplay!
        System.out.println("[MCJournalApp] 🚀 Spawned into Hardcore World at Y=" + spawnY + "!");
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

            // Handle Input
            if (currentScreen != null) {
                currentScreen.update(input.getMouseX(), input.getMouseY());
            } else {
                handleInGameMouseLook();
            }

            // Run Fixed 20-TPS Game Ticks
            while (tickAccumulator >= TICK_DURATION) {
                tick();
                tickAccumulator -= TICK_DURATION;
            }

            // Render Frame
            float partialTick = (float) (tickAccumulator / TICK_DURATION);
            render(deltaTime, partialTick);

            window.update();
        }
    }

    private void handleInGameMouseLook() {
        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            setScreen(new TitleScreen(this));
            return;
        }

        if (window.isCursorLocked()) {
            double mouseDx = input.consumeMouseDeltaX();
            double mouseDy = input.consumeMouseDeltaY();

            float sensitivity = 0.12f;
            player.yaw += (float) (mouseDx * sensitivity);
            player.pitch += (float) (mouseDy * sensitivity);
            player.pitch = Math.clamp(player.pitch, -89.5f, 89.5f);

            camera.setYaw(player.yaw);
            camera.setPitch(player.pitch);

            // Hotbar selection via number keys 1-9
            for (int k = GLFW_KEY_1; k <= GLFW_KEY_9; k++) {
                if (input.isKeyDown(k)) {
                    player.selectedSlot = k - GLFW_KEY_1;
                }
            }

            // Hotbar selection via scroll wheel
            double scroll = input.consumeScrollDelta();
            if (scroll != 0) {
                player.selectedSlot = (player.selectedSlot - (int) Math.signum(scroll) + 9) % 9;
            }
        }
    }

    private void tick() {
        if (isInWorld && currentScreen == null && chunkManager != null) {
            // Check for death / permadeath
            if (player.isDead) {
                setScreen(new GameOverScreen(this));
                return;
            }

            boolean forward = input.isKeyDown(GLFW_KEY_W);
            boolean backward = input.isKeyDown(GLFW_KEY_S);
            boolean left = input.isKeyDown(GLFW_KEY_A);
            boolean right = input.isKeyDown(GLFW_KEY_D);
            boolean jump = input.isKeyDown(GLFW_KEY_SPACE);
            boolean sprint = input.isKeyDown(GLFW_KEY_LEFT_CONTROL);
            boolean sneak = input.isKeyDown(GLFW_KEY_LEFT_SHIFT);

            player.updateTick(chunkManager, forward, backward, left, right, jump, sprint, sneak);
        }
    }

    private void render(double deltaTime, float partialTick) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (isInWorld && currentScreen == null) {
            // --- RENDER 3D VOXEL WORLD ---
            Vector3f eyePos = player.getEyePosition(partialTick);
            camera.setPosition(eyePos.x, eyePos.y, eyePos.z);
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

            // --- RENDER HARDCORE SURVIVAL HUD ---
            guiRenderer.begin(window.getWidth(), window.getHeight());
            hud.render(guiRenderer, fontRenderer, player, window.getWidth(), window.getHeight(), currentBiome);
            guiRenderer.end();
        }

        // --- RENDER ACTIVE GUI MENU ---
        if (currentScreen != null) {
            guiRenderer.begin(window.getWidth(), window.getHeight());
            currentScreen.render(guiRenderer, fontRenderer, input.getMouseX(), input.getMouseY(), (float) deltaTime);
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

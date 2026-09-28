package com.mcjournal.client;

import com.mcjournal.client.gui.*;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

/**
 * MCJournalApp is the lean top-level application coordinator.
 * It manages the primary application lifecycle:
 *   - {@link #tick()}
 *   - {@link #render(double, float)}
 *   - {@link #shutdown()}
 *
 * Specific engine responsibilities are delegated to dedicated subsystems:
 *   - Input & Inventory: {@link GameInputSystem}
 *   - World, Fluids, Saving: {@link WorldSession}
 *   - Multi-pass Rendering: {@link GameRenderer}
 *   - GUI Menus: {@link ScreenManager}
 *   - Day/Night & Solar Time: {@link AtmosphericTimeSystem}
 *   - Debug Modes: {@link DebugController}
 */
public class MCJournalApp {
    private static final double TICK_DURATION = 0.050; // 50ms = 20 TPS authoritative tick rate

    private final Window window;
    private final InputHandler input;
    private final Camera camera;
    private final Player player;
    private final GameSettings settings = GameSettings.load();

    // Dedicated Subsystems
    private final ScreenManager screenManager = new ScreenManager();
    private final AtmosphericTimeSystem atmosphericSystem = new AtmosphericTimeSystem();
    private final DebugController debugController = new DebugController();
    private final GameInputSystem gameInputSystem = new GameInputSystem();
    private final WorldSession worldSession = new WorldSession();
    private final GameRenderer gameRenderer = new GameRenderer();

    // Loop timing & FPS tracking
    private double lastFrameTime = 0;
    private double tickAccumulator = 0;
    private int currentFps = 60;
    private int frameCount = 0;
    private double fpsTimer = 0.0;

    public MCJournalApp() {
        this.window = new Window("Minecraft Journal - Native Hardcore Edition", 1280, 760);
        this.input = new InputHandler();
        this.camera = new Camera();
        this.player = new Player();
    }

    public void run() {
        init();
        loop();
        shutdown();
    }

    private void init() {
        window.init();
        input.init(window.getHandle(), this);
        applySettings();

        gameRenderer.init();
        worldSession.init();
        camera.updateProjection(window.getAspectRatio());

        setScreen(new TitleScreen(this));
        lastFrameTime = glfwGetTime();
        System.out.println("[MCJournalApp] Native OpenGL 3.3 Hardcore Engine Ready!");
    }

    private void loop() {
        while (!window.shouldClose()) {
            double currentFrameTime = glfwGetTime();
            double deltaTime = Math.min(currentFrameTime - lastFrameTime, 0.1);
            lastFrameTime = currentFrameTime;

            tickAccumulator += deltaTime;

            frameCount++;
            fpsTimer += deltaTime;
            if (fpsTimer >= 1.0) {
                currentFps = frameCount;
                frameCount = 0;
                fpsTimer -= 1.0;
            }

            if (window.isResized()) {
                camera.updateProjection(window.getAspectRatio());
                screenManager.onResize(window.getWidth(), window.getHeight());
                window.setResized(false);
            }

            // Subsystem updates
            gameRenderer.updateMenuBackground(deltaTime);
            atmosphericSystem.update(deltaTime, worldSession.isInWorld(), screenManager.hasScreen());
            debugController.update(input);

            if (screenManager.hasScreen()) {
                screenManager.update(input);
            } else {
                gameInputSystem.handleInGameMouseLook(this, window, input, player, camera);
            }

            // Fixed 20-TPS authoritative ticks during active gameplay
            if (worldSession.isInWorld() && !screenManager.hasScreen()) {
                while (tickAccumulator >= TICK_DURATION) {
                    tick();
                    tickAccumulator -= TICK_DURATION;
                }
            } else {
                tickAccumulator = 0;
            }

            // Render Frame
            float partialTick = (float) (tickAccumulator / TICK_DURATION);
            render(deltaTime, partialTick);

            window.update();

            // Frame Rate Limiter
            if (!settings.vsync && settings.maxFps > 0) {
                double targetFrameDuration = 1.0 / settings.maxFps;
                double elapsed = glfwGetTime() - currentFrameTime;
                while (elapsed < targetFrameDuration) {
                    double remaining = targetFrameDuration - elapsed;
                    if (remaining > 0.002) {
                        try {
                            Thread.sleep((long) ((remaining - 0.001) * 1000));
                        } catch (InterruptedException ignored) {}
                    } else {
                        Thread.onSpinWait();
                    }
                    elapsed = glfwGetTime() - currentFrameTime;
                }
            }
        }
    }

    public void tick() {
        if (!worldSession.isInWorld() || screenManager.hasScreen()) {
            return;
        }

        if (player.isDead) {
            setScreen(new GameOverScreen(this));
            return;
        }

        worldSession.tick(player, camera, input, gameRenderer.getHandRenderer(), gameRenderer.getChunkRenderer(), gameInputSystem);
    }

    public void render(double deltaTime, float partialTick) {
        gameRenderer.render(deltaTime, partialTick, worldSession, atmosphericSystem, player, camera,
                screenManager, window, input, currentFps, settings);
    }

    public void shutdown() {
        worldSession.shutdown(player, atmosphericSystem.getWorldTimeTicks());
        gameRenderer.cleanup();
        window.destroy();
    }

    // --- High-Level UI & System Delegates ---

    public void enterWorld(long seed, String worldName, String biome, WorldSaveManager.SavedWorld existingSave) {
        worldSession.enterWorld(seed, worldName, biome, existingSave, player, camera,
                gameRenderer.getChunkRenderer(), settings, atmosphericSystem);
        setScreen(null);
    }

    public void resumeGame() {
        setScreen(null);
    }

    public void saveAndQuitToTitle() {
        worldSession.saveAndQuit(player, atmosphericSystem.getWorldTimeTicks());
        setScreen(new TitleScreen(this));
    }

    public void quitGame() {
        window.close();
    }

    public void applySettings() {
        if (window != null) {
            window.setVsync(settings.vsync || settings.maxFps == 0);
        }
        if (camera != null) {
            camera.setFov(settings.fov);
            camera.updateProjection(window.getAspectRatio());
        }
        worldSession.setRenderDistance(settings.renderDistance);
    }

    public void setScreen(Screen screen) {
        screenManager.setScreen(screen, window, worldSession.getBlockBreakingManager());
    }

    public Screen getCurrentScreen() {
        return screenManager.getCurrentScreen();
    }

    public GameSettings getSettings() {
        return settings;
    }

    public Window getWindow() {
        return window;
    }

    public Camera getCamera() {
        return camera;
    }

    public Player getPlayer() {
        return player;
    }

    public int getFps() {
        return currentFps;
    }

    public TextureAtlas getAtlas() {
        return gameRenderer.getAtlas();
    }

    public VideoBackgroundManager getVideoBackgroundManager() {
        return gameRenderer.getVideoBackgroundManager();
    }

    public static void main(String[] args) {
        new MCJournalApp().run();
    }
}

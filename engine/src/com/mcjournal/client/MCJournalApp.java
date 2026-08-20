package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkMeshBuilder;
import com.mcjournal.FluidPhysicsManager;
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
    private SkyRenderer skyRenderer;
    private GuiRenderer guiRenderer;
    private FontRenderer fontRenderer;
    private HardcoreHUD hud;
    private VideoBackgroundManager videoBackgroundManager;
    private BlockBreakingManager blockBreakingManager;
    private BlockSelectionRenderer blockSelectionRenderer;
    private ParticleManager particleManager;
    private FluidPhysicsManager fluidPhysicsManager;
    private FirstPersonHandRenderer handRenderer;

    private ChunkManager chunkManager;
    private ShaderProgram chunkShader;
    private String currentBiome = "Plains";
    private String currentWorldName = "Hardcore World";
    private long currentSeed = 4242;

    // Time of Day & Continuous Solar Orbital Cycle (0.0 = Noon, 0.25 = Sunset, 0.5 = Midnight, 0.75 = Sunrise)
    private double worldTimeTicks = 6000; // Starts at crisp clear Mid-Morning / Day 1 Baseline
    private final Vector3f sunDir = new Vector3f();
    private final Vector3f directLightColor = new Vector3f();
    private final Vector3f skyAmbientColor = new Vector3f();
    private final Vector3f groundAmbientColor = new Vector3f();
    private final Vector3f zenithColor = new Vector3f();
    private final Vector3f horizonColor = new Vector3f();
    private final Vector3f sunColor = new Vector3f();
    private final Vector3f underwaterFogColor = new Vector3f();

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
        this.skyRenderer = new SkyRenderer();
        this.videoBackgroundManager = new VideoBackgroundManager();
        this.blockBreakingManager = new BlockBreakingManager();
        this.blockSelectionRenderer = new BlockSelectionRenderer();
        this.particleManager = new ParticleManager();
        this.fluidPhysicsManager = new FluidPhysicsManager();
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
        hud.init();

        skyRenderer.init();
        videoBackgroundManager.init();
        blockSelectionRenderer.init();
        particleManager.init();
        handRenderer = new FirstPersonHandRenderer();
        handRenderer.init();

        atlas = new TextureAtlas();
        chunkRenderer = new ChunkRenderer();

        camera.updateProjection(window.getAspectRatio());

        // 3. Load Shaders
        chunkShader = new ShaderProgram("/shaders/chunk_vertex.glsl", "/shaders/chunk_fragment.glsl");

        // 4. Generate Procedural 64x Pixel-Art Texture Atlas
        atlas.init();

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
            if (blockBreakingManager != null) blockBreakingManager.resetBreak();
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

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public VideoBackgroundManager getVideoBackgroundManager() {
        return videoBackgroundManager;
    }

    public void resumeGame() {
        setScreen(null); // Closes escape menu and returns directly to 3D world
    }

    public void saveAndQuitToTitle() {
        if (isInWorld) {
            WorldSaveManager.saveWorld(
                currentWorldName,
                currentBiome,
                currentSeed,
                player,
                this.worldTimeTicks,
                chunkManager != null ? chunkManager.getModifiedBlocks() : null
            );
            this.isInWorld = false;
        }
        setScreen(new TitleScreen(this));
    }

    public void quitGame() {
        window.close();
    }

    public void enterWorld(long seed, String worldName, String biome, WorldSaveManager.SavedWorld existingSave) {
        this.currentBiome = biome;
        this.currentWorldName = worldName;
        this.currentSeed = seed;

        System.out.println("[MCJournalApp] Generating 500+ chunk Hardcore world: '" + worldName + "' (Seed: " + seed + ")...");
        this.chunkManager = new ChunkManager(11, seed);

        // 1. If loading an existing save, apply all persisted voxel block changes!
        if (existingSave != null && existingSave.modifiedBlocks != null && !existingSave.modifiedBlocks.isEmpty()) {
            System.out.println("[MCJournalApp] Restoring " + existingSave.modifiedBlocks.size() + " modified world blocks from save file...");
            chunkManager.applyModifiedBlocks(existingSave.modifiedBlocks);
        }

        // 2. Upload all computed chunk meshes to GPU
        chunkRenderer.cleanup();
        Map<String, ChunkMeshBuilder.MeshData> meshes = chunkManager.getAllMeshes();
        for (Map.Entry<String, ChunkMeshBuilder.MeshData> entry : meshes.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            chunkRenderer.uploadChunkMesh(cx, cz, entry.getValue());
        }

        // 3. Restore or compute player spawn position, stats & time of day
        if (existingSave != null) {
            this.worldTimeTicks = existingSave.worldTime;
            player.pos.set(existingSave.playerX, existingSave.playerY, existingSave.playerZ);
            player.prevPos.set(existingSave.playerX, existingSave.playerY, existingSave.playerZ);
            player.yaw = existingSave.playerYaw;
            player.pitch = existingSave.playerPitch;
            player.health = existingSave.health;
            player.hunger = existingSave.hunger;
            player.selectedSlot = Math.clamp(existingSave.selectedSlot, 0, 8);
            player.velocity.set(0, 0, 0);
            player.isDead = false;

            camera.setYaw(player.yaw);
            camera.setPitch(player.pitch);
            System.out.println("[MCJournalApp] 🚀 Restored player state at (" +
                    String.format("%.1f, %.1f, %.1f", player.pos.x, player.pos.y, player.pos.z) + ", HP: " + player.health + "/20, Time: " +
                    String.format("%.0f", this.worldTimeTicks) + " ticks)!");
        } else {
            this.worldTimeTicks = 6000.0; // Day 1 baseline

            // Safe surface spawn scan for new world
            int spawnX = 8;
            int spawnZ = 8;
            int spawnY = 66;
            for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                if (Block.isSolid(chunkManager.getBlockAt(spawnX, y, spawnZ))) {
                    spawnY = y + 2;
                    break;
                }
            }

            player.pos.set(spawnX, spawnY, spawnZ);
            player.prevPos.set(spawnX, spawnY, spawnZ);
            player.yaw = 0;
            player.pitch = 0;
            player.velocity.set(0, 0, 0);
            player.health = 20;
            player.hunger = 20;
            player.selectedSlot = 0;
            player.isDead = false;

            camera.setYaw(0);
            camera.setPitch(0);

            // Immediately save initial state
            WorldSaveManager.saveWorld(worldName, biome, seed, player, this.worldTimeTicks, null);
            System.out.println("[MCJournalApp] 🚀 Spawned into fresh Hardcore World at Y=" + spawnY + "!");
        }

        this.isInWorld = true;
        setScreen(null); // Switch directly into 3D Hardcore gameplay!
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static Vector3f lerpVec(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        );
    }

    private void updateAtmosphericSolarCycle(double deltaTime) {
        // Continuous 24,000 tick Minecraft Day/Night Cycle (advances only during active in-world gameplay)
        if (isInWorld && currentScreen == null) {
            worldTimeTicks = (worldTimeTicks + deltaTime * 20.0) % 24000.0;
        }
        float dayFraction = (float) (worldTimeTicks / 24000.0);

        // Astronomical solar orbital angle
        float sunAngle = (dayFraction * 2.0f * (float) Math.PI) - ((float) Math.PI / 2.0f);
        float sinElev = (float) Math.sin(sunAngle);
        float cosElev = (float) Math.cos(sunAngle);

        sunDir.set(cosElev * 0.75f, sinElev, 0.35f).normalize();
        float sunElevation = sunDir.y;

        // Smooth continuous interpolation between Day, Sunset, and Night states
        // Day factor: 1.0 at high noon, 0.0 below elevation 0.05
        float dayWeight = smoothstep(0.05f, 0.35f, sunElevation);

        // Twilight / Sunset factor: Peaks around elevation 0.0, zero at noon & midnight
        float twilightWeight = (1.0f - Math.abs(sunElevation - 0.05f) / 0.30f);
        twilightWeight = Math.clamp(twilightWeight, 0.0f, 1.0f);
        twilightWeight = smoothstep(0.0f, 1.0f, twilightWeight) * (1.0f - dayWeight);

        // Night weight: Remaining weight when sun is below horizon
        float nightWeight = Math.clamp(1.0f - dayWeight - twilightWeight, 0.0f, 1.0f);

        // 1. Direct Sun / Moon Illuminance
        Vector3f dayDirect = new Vector3f(RenderingConfig.DAY_SUN_COLOR).mul(RenderingConfig.DAY_SUN_INTENSITY);
        Vector3f sunsetDirect = new Vector3f(RenderingConfig.SUNSET_SUN_COLOR).mul(RenderingConfig.SUNSET_SUN_INTENSITY);
        Vector3f nightDirect = new Vector3f(RenderingConfig.NIGHT_MOON_COLOR).mul(RenderingConfig.NIGHT_MOON_INTENSITY);

        directLightColor.set(
            dayDirect.x * dayWeight + sunsetDirect.x * twilightWeight + nightDirect.x * nightWeight,
            dayDirect.y * dayWeight + sunsetDirect.y * twilightWeight + nightDirect.y * nightWeight,
            dayDirect.z * dayWeight + sunsetDirect.z * twilightWeight + nightDirect.z * nightWeight
        );

        // 2. Hemisphere Sky Ambient
        Vector3f daySky = new Vector3f(RenderingConfig.DAY_SKY_AMBIENT_COLOR).mul(RenderingConfig.DAY_SKY_AMBIENT_STRENGTH);
        Vector3f sunsetSky = new Vector3f(RenderingConfig.SUNSET_SKY_AMBIENT_COLOR).mul(RenderingConfig.SUNSET_SKY_AMBIENT_STRENGTH);
        Vector3f nightSky = new Vector3f(RenderingConfig.NIGHT_SKY_AMBIENT_COLOR).mul(RenderingConfig.NIGHT_SKY_AMBIENT_STRENGTH);

        skyAmbientColor.set(
            daySky.x * dayWeight + sunsetSky.x * twilightWeight + nightSky.x * nightWeight,
            daySky.y * dayWeight + sunsetSky.y * twilightWeight + nightSky.y * nightWeight,
            daySky.z * dayWeight + sunsetSky.z * twilightWeight + nightSky.z * nightWeight
        );

        // 3. Hemisphere Ground Ambient
        Vector3f dayGround = new Vector3f(RenderingConfig.DAY_GROUND_AMBIENT_COLOR).mul(RenderingConfig.DAY_GROUND_AMBIENT_STRENGTH);
        Vector3f sunsetGround = new Vector3f(RenderingConfig.SUNSET_GROUND_AMBIENT_COLOR).mul(RenderingConfig.SUNSET_GROUND_AMBIENT_STRENGTH);
        Vector3f nightGround = new Vector3f(RenderingConfig.NIGHT_GROUND_AMBIENT_COLOR).mul(RenderingConfig.NIGHT_GROUND_AMBIENT_STRENGTH);

        groundAmbientColor.set(
            dayGround.x * dayWeight + sunsetGround.x * twilightWeight + nightGround.x * nightWeight,
            dayGround.y * dayWeight + sunsetGround.y * twilightWeight + nightGround.y * nightWeight,
            dayGround.z * dayWeight + sunsetGround.z * twilightWeight + nightGround.z * nightWeight
        );

        // 4. Sky Dome Zenith & Horizon Colors
        zenithColor.set(
            RenderingConfig.DAY_ZENITH_COLOR.x * dayWeight + RenderingConfig.SUNSET_ZENITH_COLOR.x * twilightWeight + RenderingConfig.NIGHT_ZENITH_COLOR.x * nightWeight,
            RenderingConfig.DAY_ZENITH_COLOR.y * dayWeight + RenderingConfig.SUNSET_ZENITH_COLOR.y * twilightWeight + RenderingConfig.NIGHT_ZENITH_COLOR.y * nightWeight,
            RenderingConfig.DAY_ZENITH_COLOR.z * dayWeight + RenderingConfig.SUNSET_ZENITH_COLOR.z * twilightWeight + RenderingConfig.NIGHT_ZENITH_COLOR.z * nightWeight
        );

        horizonColor.set(
            RenderingConfig.DAY_HORIZON_COLOR.x * dayWeight + RenderingConfig.SUNSET_HORIZON_COLOR.x * twilightWeight + RenderingConfig.NIGHT_HORIZON_COLOR.x * nightWeight,
            RenderingConfig.DAY_HORIZON_COLOR.y * dayWeight + RenderingConfig.SUNSET_HORIZON_COLOR.y * twilightWeight + RenderingConfig.NIGHT_HORIZON_COLOR.y * nightWeight,
            RenderingConfig.DAY_HORIZON_COLOR.z * dayWeight + RenderingConfig.SUNSET_HORIZON_COLOR.z * twilightWeight + RenderingConfig.NIGHT_HORIZON_COLOR.z * nightWeight
        );

        sunColor.set(
            RenderingConfig.DAY_SUN_COLOR.x * dayWeight + RenderingConfig.SUNSET_SUN_COLOR.x * twilightWeight + RenderingConfig.NIGHT_MOON_COLOR.x * nightWeight,
            RenderingConfig.DAY_SUN_COLOR.y * dayWeight + RenderingConfig.SUNSET_SUN_COLOR.y * twilightWeight + RenderingConfig.NIGHT_MOON_COLOR.y * nightWeight,
            RenderingConfig.DAY_SUN_COLOR.z * dayWeight + RenderingConfig.SUNSET_SUN_COLOR.z * twilightWeight + RenderingConfig.NIGHT_MOON_COLOR.z * nightWeight
        );

        underwaterFogColor.set(
            RenderingConfig.UNDERWATER_DAY_FOG_COLOR.x * dayWeight + RenderingConfig.UNDERWATER_SUNSET_FOG_COLOR.x * twilightWeight + RenderingConfig.UNDERWATER_NIGHT_FOG_COLOR.x * nightWeight,
            RenderingConfig.UNDERWATER_DAY_FOG_COLOR.y * dayWeight + RenderingConfig.UNDERWATER_SUNSET_FOG_COLOR.y * twilightWeight + RenderingConfig.UNDERWATER_NIGHT_FOG_COLOR.y * nightWeight,
            RenderingConfig.UNDERWATER_DAY_FOG_COLOR.z * dayWeight + RenderingConfig.UNDERWATER_SUNSET_FOG_COLOR.z * twilightWeight + RenderingConfig.UNDERWATER_NIGHT_FOG_COLOR.z * nightWeight
        );

        // Dynamic exposure adaptation: slightly higher at night to preserve silhouette readability
        RenderingConfig.exposure = 1.0f * dayWeight + 1.15f * twilightWeight + 1.35f * nightWeight;
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

            // Update Video Background Animation (Menu Screens)
            videoBackgroundManager.update(deltaTime);

            // Update Dynamic Solar & Atmospheric Cycle
            updateAtmosphericSolarCycle(deltaTime);

            // Handle Input & Debug Mode Toggles (F1 - F9)
            handleDebugInput();

            if (currentScreen != null) {
                currentScreen.update(input.getMouseX(), input.getMouseY());
            } else {
                handleInGameMouseLook();
            }

            // Run Fixed 20-TPS Game Ticks (only during active gameplay)
            if (isInWorld && currentScreen == null) {
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
        }
    }

    private void handleDebugInput() {
        if (input.isKeyDown(GLFW_KEY_F1)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_NORMAL;
        if (input.isKeyDown(GLFW_KEY_F2)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_ALBEDO;
        if (input.isKeyDown(GLFW_KEY_F3)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_NORMALS;
        if (input.isKeyDown(GLFW_KEY_F4)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_DIRECT_LIGHT;
        if (input.isKeyDown(GLFW_KEY_F5)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_AMBIENT_LIGHT;
        if (input.isKeyDown(GLFW_KEY_F6)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_AO;
        if (input.isKeyDown(GLFW_KEY_F7)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_TOTAL_LIGHT;
        if (input.isKeyDown(GLFW_KEY_F8)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_PRE_TONEMAP;
        if (input.isKeyDown(GLFW_KEY_F9)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_FOG;
        if (input.isKeyDown(GLFW_KEY_F10)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_WATER_ALBEDO;
        if (input.isKeyDown(GLFW_KEY_F11)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_WATER_DEPTH;
        if (input.isKeyDown(GLFW_KEY_F12)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_WATER_TRANSMISSION;
    }

    private void handleInGameMouseLook() {
        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            setScreen(new EscapeMenuScreen(this));
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

            // Hotbar keys 1-9
            for (int k = GLFW_KEY_1; k <= GLFW_KEY_9; k++) {
                if (input.isKeyDown(k)) {
                    player.selectedSlot = k - GLFW_KEY_1;
                }
            }

            double scroll = input.consumeScrollDelta();
            if (scroll != 0) {
                player.selectedSlot = (player.selectedSlot - (int) Math.signum(scroll) + 9) % 9;
            }
        }
    }

    private void tick() {
        if (isInWorld && currentScreen == null && chunkManager != null) {
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

            // Mine blocks by hand (LMB) & Place blocks (RMB) with particles and fluid physics
            boolean lmb = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_1);
            boolean rmb = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_2);
            blockBreakingManager.update(chunkManager, chunkRenderer, particleManager, fluidPhysicsManager, handRenderer, player, camera, lmb, rmb);

            // Update Fluid Physics Simulation (Water flows, cascades, and fills cavities)
            fluidPhysicsManager.updateTicks(chunkManager, chunkRenderer, particleManager);

            // Update Particle simulation
            particleManager.update(TICK_DURATION, chunkManager);
        }
    }

    private void render(double deltaTime, float partialTick) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (isInWorld && chunkManager != null) {
            Vector3f eyePos = player.getEyePosition(partialTick);
            camera.setPosition(eyePos.x, eyePos.y, eyePos.z);
            camera.updateView();

            float curTime = (float) glfwGetTime();
            float timeOfDayFraction = (float) (worldTimeTicks / 24000.0);

            // Check if player's camera eye position is submerged in water
            int eyeBlockX = (int) Math.floor(eyePos.x);
            int eyeBlockY = (int) Math.floor(eyePos.y);
            int eyeBlockZ = (int) Math.floor(eyePos.z);
            boolean isUnderwater = (chunkManager.getBlockAt(eyeBlockX, eyeBlockY, eyeBlockZ) == Block.WATER);

            float curFogStart = isUnderwater ? RenderingConfig.UNDERWATER_FOG_START : RenderingConfig.FOG_START;
            float curFogEnd = isUnderwater ? RenderingConfig.UNDERWATER_FOG_END : RenderingConfig.FOG_END;
            Vector3f curFogColor = isUnderwater ? underwaterFogColor : horizonColor;

            // --- 1. RENDER ATMOSPHERIC SKY GRADIENT, SUN, MOON & STARS ---
            if (isUnderwater) {
                skyRenderer.render(camera, sunDir, underwaterFogColor, underwaterFogColor, sunColor, timeOfDayFraction);
            } else {
                skyRenderer.render(camera, sunDir, zenithColor, horizonColor, sunColor, timeOfDayFraction);
            }

            // --- 2. RENDER 3D VOXEL WORLD WITH UNIFIED LIGHTING & FRESNEL WATER ---
            chunkShader.bind();
            atlas.bind(0);
            chunkShader.setUniform("uAtlas", 0);
            chunkShader.setUniform("uProjection", camera.getProjectionMatrix());
            chunkShader.setUniform("uView", camera.getViewMatrix());
            chunkShader.setUniform("uSunDir", (sunDir.y >= -0.05f) ? sunDir : new Vector3f(sunDir).negate());
            chunkShader.setUniform("uDirectLightColor", directLightColor);
            chunkShader.setUniform("uSkyAmbientColor", skyAmbientColor);
            chunkShader.setUniform("uGroundAmbientColor", groundAmbientColor);
            chunkShader.setUniform("uFogColor", curFogColor);
            chunkShader.setUniform("uFogStart", curFogStart);
            chunkShader.setUniform("uFogEnd", curFogEnd);
            chunkShader.setUniform("uCameraPos", camera.getPosition());
            chunkShader.setUniform("uTime", curTime);
            chunkShader.setUniform("uExposure", RenderingConfig.exposure);
            chunkShader.setUniform("uDebugMode", RenderingConfig.currentDebugMode);
            chunkShader.setUniform("uIsUnderwater", isUnderwater ? 1 : 0);

            // Water uniforms from RenderingConfig
            chunkShader.setUniform("uWaterShallowColor", RenderingConfig.WATER_SHALLOW_COLOR);
            chunkShader.setUniform("uWaterMidColor", RenderingConfig.WATER_MID_COLOR);
            chunkShader.setUniform("uWaterDeepColor", RenderingConfig.WATER_DEEP_COLOR);
            chunkShader.setUniform("uWaterFresnelF0", RenderingConfig.WATER_FRESNEL_F0);
            chunkShader.setUniform("uWaterSpecularPower", RenderingConfig.WATER_SPECULAR_POWER);
            chunkShader.setUniform("uWaterSpecularStrength", RenderingConfig.WATER_SPECULAR_STRENGTH);
            chunkShader.setUniform("uWaterAbsorptionMu", RenderingConfig.WATER_ABSORPTION_MU);
            chunkShader.setUniform("uAoMinClamp", RenderingConfig.AO_MIN_CLAMP);

            // Render Solid Geometry (Opaque Voxel Blocks)
            chunkShader.setUniform("uIsWater", 0);
            chunkRenderer.renderSolid();

            // Render Water Geometry (Stylized Depth Absorption & Fresnel Reflections)
            chunkShader.setUniform("uIsWater", 1);
            chunkRenderer.renderWater(isUnderwater);

            chunkShader.unbind();
            atlas.unbind();

            // --- 3. RENDER MINECRAFT BLOCK CRACKING OVERLAY & SELECTION OUTLINE ---
            if (currentScreen == null) {
                Raycast.Hit hit = blockBreakingManager.getCurrentHit();
                if (hit != null) {
                    blockSelectionRenderer.render(camera, hit.bx, hit.by, hit.bz, blockBreakingManager.getBreakProgress());
                }
            }

            // --- 4. RENDER DISINTEGRATION PARTICLES ---
            particleManager.render(camera);

            // --- 4.5 RENDER FIRST-PERSON STEVE HAND & VIEWMODEL ---
            if (currentScreen == null) {
                handRenderer.update((float) deltaTime, player);
                handRenderer.render(camera, player, sunDir, directLightColor, skyAmbientColor, groundAmbientColor, window.getAspectRatio());
            }

            // --- 5. IN-GAME HARDCORE HUD ---
            if (currentScreen == null) {
                guiRenderer.begin(window.getWidth(), window.getHeight());
                hud.render(guiRenderer, fontRenderer, player, window.getWidth(), window.getHeight(), currentBiome);
                guiRenderer.end();
            }
        } else {
            // --- LIVE MP4 VIDEO BACKGROUND (Title / World Select Menus) ---
            videoBackgroundManager.render(window.getWidth(), window.getHeight());
        }

        // --- 6. RENDER ACTIVE GUI MENU OVERLAY ---
        if (currentScreen != null) {
            guiRenderer.begin(window.getWidth(), window.getHeight());
            currentScreen.render(guiRenderer, fontRenderer, input.getMouseX(), input.getMouseY(), (float) deltaTime);
            guiRenderer.end();
        }
    }

    private void cleanup() {
        if (isInWorld) {
            WorldSaveManager.saveWorld(
                currentWorldName,
                currentBiome,
                currentSeed,
                player,
                this.worldTimeTicks,
                chunkManager != null ? chunkManager.getModifiedBlocks() : null
            );
        }
        if (chunkShader != null) chunkShader.cleanup();
        if (skyRenderer != null) skyRenderer.cleanup();
        if (atlas != null) atlas.cleanup();
        if (chunkRenderer != null) chunkRenderer.cleanup();
        if (guiRenderer != null) guiRenderer.cleanup();
        if (fontRenderer != null) fontRenderer.cleanup();
        if (videoBackgroundManager != null) videoBackgroundManager.cleanup();
        if (blockSelectionRenderer != null) blockSelectionRenderer.cleanup();
        if (particleManager != null) particleManager.cleanup();
        if (handRenderer != null) handRenderer.cleanup();
        if (hud != null) hud.cleanup();
        window.destroy();
    }

    public static void main(String[] args) {
        new MCJournalApp().run();
    }
}

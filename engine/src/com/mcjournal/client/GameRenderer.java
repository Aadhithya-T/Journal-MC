package com.mcjournal.client;

import com.mcjournal.client.gui.FontRenderer;
import com.mcjournal.client.gui.GuiRenderer;
import com.mcjournal.client.gui.HardcoreHUD;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * GameRenderer orchestrates the multi-pass 3D and 2D rendering pipeline:
 * sky atmosphere, solid/cutout/water voxel geometry, dropped 3D items,
 * selection outline, particle effects, first-person hand viewmodel,
 * in-game HUD & hurt vignette, and menu background.
 */
public class GameRenderer {
    private TextureAtlas atlas;
    private ChunkRenderer chunkRenderer;
    private SkyRenderer skyRenderer;
    private GuiRenderer guiRenderer;
    private FontRenderer fontRenderer;
    private HardcoreHUD hud;
    private BlockSelectionRenderer blockSelectionRenderer;
    private FirstPersonHandRenderer handRenderer;

    private ShaderProgram chunkShader;
    private final FrustumCuller frustumCuller = new FrustumCuller();

    public void init() {
        guiRenderer = new GuiRenderer();
        fontRenderer = new FontRenderer();
        fontRenderer.init();
        hud = new HardcoreHUD();
        hud.init();

        skyRenderer = new SkyRenderer();
        skyRenderer.init();

        blockSelectionRenderer = new BlockSelectionRenderer();
        blockSelectionRenderer.init();

        handRenderer = new FirstPersonHandRenderer();
        handRenderer.init();

        atlas = new TextureAtlas();
        chunkRenderer = new ChunkRenderer();

        chunkShader = new ShaderProgram("/shaders/chunk_vertex.glsl", "/shaders/chunk_fragment.glsl");
        atlas.init();
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }

    public FirstPersonHandRenderer getHandRenderer() {
        return handRenderer;
    }

    public GuiRenderer getGuiRenderer() {
        return guiRenderer;
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    public void render(double deltaTime, float partialTick,
                       WorldSession worldSession, AtmosphericTimeSystem timeSystem,
                       Player player, Camera camera, ScreenManager screenManager,
                       Window window, InputHandler input, int currentFps, GameSettings settings) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (worldSession != null && worldSession.isInWorld() && worldSession.getChunkManager() != null) {
            worldSession.prepareRender(chunkRenderer);

            Vector3f eyePos = player.getEyePosition(partialTick);
            camera.setPosition(eyePos.x, eyePos.y, eyePos.z);

            // Damage Screen Shake / Hurt Camera Tilt
            float damageTilt = 0.0f;
            if (player.hurtTime > 0) {
                float hurtFraction = Math.clamp(((float) player.hurtTime - partialTick) / (float) player.maxHurtTime, 0.0f, 1.0f);
                if (hurtFraction > 0.0f) {
                    damageTilt = (float) Math.sin(hurtFraction * Math.PI) * player.hurtAngle;
                }
            }
            camera.setRoll(damageTilt);
            camera.updateView();
            frustumCuller.update(camera.getProjectionMatrix(), camera.getViewMatrix());
            chunkRenderer.updateVisibility(frustumCuller, eyePos.x, eyePos.z, worldSession.getChunkManager().getRenderDistance());

            float curTime = (float) glfwGetTime();
            float timeOfDayFraction = (float) (timeSystem.getWorldTimeTicks() / 24000.0);

            // Check if player's camera eye position is submerged in water
            int eyeBlockX = (int) Math.floor(eyePos.x);
            int eyeBlockY = (int) Math.floor(eyePos.y);
            int eyeBlockZ = (int) Math.floor(eyePos.z);
            boolean isUnderwater = worldSession.getChunkManager().getBlockStateAt(eyeBlockX, eyeBlockY, eyeBlockZ).isWater();

            // Calibrate atmospheric horizon fog
            float curFogStart = isUnderwater ? RenderingConfig.UNDERWATER_FOG_START : Math.max(32.0f, (worldSession.getChunkManager().getRenderDistance() - 2.5f) * 16.0f);
            float curFogEnd = isUnderwater ? RenderingConfig.UNDERWATER_FOG_END : (worldSession.getChunkManager().getRenderDistance() - 0.5f) * 16.0f;
            Vector3f curFogColor = isUnderwater ? timeSystem.getUnderwaterFogColor() : timeSystem.getHorizonColor();

            // 1. Render Sky Gradient, Sun, Moon & Stars
            if (isUnderwater) {
                skyRenderer.render(camera, timeSystem.getSunDir(), timeSystem.getUnderwaterFogColor(), timeSystem.getUnderwaterFogColor(), timeSystem.getSunColor(), timeOfDayFraction);
            } else {
                skyRenderer.render(camera, timeSystem.getSunDir(), timeSystem.getZenithColor(), timeSystem.getHorizonColor(), timeSystem.getSunColor(), timeOfDayFraction);
            }

            // 2. Render 3D Voxel World With Unified Lighting & Fresnel Water
            chunkShader.bind();
            atlas.bind(0);
            chunkShader.setUniform("uAtlas", 0);
            chunkShader.setUniform("uProjection", camera.getProjectionMatrix());
            chunkShader.setUniform("uView", camera.getViewMatrix());
            Vector3f sunDir = timeSystem.getSunDir();
            chunkShader.setUniform("uSunDir", (sunDir.y >= -0.05f) ? sunDir : new Vector3f(sunDir).negate());
            chunkShader.setUniform("uDirectLightColor", timeSystem.getDirectLightColor());
            chunkShader.setUniform("uSkyAmbientColor", timeSystem.getSkyAmbientColor());
            chunkShader.setUniform("uGroundAmbientColor", timeSystem.getGroundAmbientColor());
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

            // 1. Render Solid Geometry
            chunkShader.setUniform("uIsWater", 0);
            chunkShader.setUniform("uIsCutout", 0);
            chunkRenderer.renderSolid();

            // 2. Render Cutout Geometry
            chunkShader.setUniform("uIsCutout", 1);
            chunkRenderer.renderCutout();

            // 3. Render Dropped 3D Item Entities
            chunkShader.setUniform("uIsCutout", 0);
            worldSession.getItemEntityManager().render(chunkShader, camera, partialTick);

            // 4. Render Water Geometry
            chunkShader.setUniform("uIsCutout", 0);
            chunkShader.setUniform("uIsWater", 1);
            chunkRenderer.renderWater(isUnderwater);

            chunkShader.unbind();
            atlas.unbind();

            // 3. Render Block Cracking Overlay & Selection Outline
            if (!screenManager.hasScreen()) {
                Raycast.Hit hit = worldSession.getBlockBreakingManager().getCurrentHit();
                if (hit != null) {
                    blockSelectionRenderer.render(camera, hit.bx, hit.by, hit.bz, worldSession.getBlockBreakingManager().getBreakProgress());
                }
            }

            // 4. Render Disintegration Particles
            worldSession.getParticleManager().render(camera);

            // 4.5 Render First-Person Hand & Viewmodel
            if (!screenManager.hasScreen()) {
                handRenderer.update((float) deltaTime, player);
                handRenderer.render(camera, player, atlas, timeSystem.getSunDir(),
                        timeSystem.getDirectLightColor(), timeSystem.getSkyAmbientColor(),
                        timeSystem.getGroundAmbientColor(), window.getAspectRatio());
            }

            // 5. In-Game HUD
            if (!screenManager.hasScreen()) {
                guiRenderer.begin(window.getWidth(), window.getHeight());
                int renderedCount = (chunkRenderer != null) ? chunkRenderer.getLastRenderedChunks() : 0;
                int loadedCount = (chunkRenderer != null) ? chunkRenderer.getLoadedMeshCount() : 0;
                hud.render(guiRenderer, fontRenderer, player, window.getWidth(), window.getHeight(),
                        worldSession.getCurrentBiome(), atlas != null ? atlas.getTextureId() : 0,
                        currentFps, settings.showFps, renderedCount, loadedCount);

                // Red damage hurt vignette
                if (player.hurtTime > 0) {
                    float hurtFraction = Math.clamp(((float) player.hurtTime - partialTick) / (float) player.maxHurtTime, 0.0f, 1.0f);
                    if (hurtFraction > 0.0f) {
                        float alpha = hurtFraction * 0.32f;
                        guiRenderer.drawRect(0, 0, window.getWidth(), window.getHeight(), 0.90f, 0.05f, 0.05f, alpha);
                    }
                }
                guiRenderer.end();
            }
        } else {
            // Authentic Minecraft dirt background (Title / Menus)
            if (atlas != null) {
                guiRenderer.begin(window.getWidth(), window.getHeight());
                guiRenderer.drawDirtBackground(atlas.getTextureId(), window.getWidth(), window.getHeight());
                guiRenderer.end();
            }
        }

        // 6. Active Screen Menu Overlay
        screenManager.render(guiRenderer, fontRenderer, input, (float) deltaTime, window.getWidth(), window.getHeight());

        // Latch frame statistics for the debug overlay
        EngineMetrics.getInstance().endFrame();
    }

    public void cleanup() {
        if (chunkShader != null) chunkShader.cleanup();
        if (skyRenderer != null) skyRenderer.cleanup();
        if (atlas != null) atlas.cleanup();
        if (chunkRenderer != null) chunkRenderer.cleanup();
        if (guiRenderer != null) guiRenderer.cleanup();
        if (fontRenderer != null) fontRenderer.cleanup();
        if (blockSelectionRenderer != null) blockSelectionRenderer.cleanup();
        if (handRenderer != null) handRenderer.cleanup();
        if (hud != null) hud.cleanup();
    }
}

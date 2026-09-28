package com.mcjournal.client;

import org.joml.Vector3f;

/**
 * AtmosphericTimeSystem manages the continuous 24,000 tick Minecraft Day/Night Cycle,
 * astronomical solar orbital kinematics, and dynamic atmospheric lighting interpolation.
 */
public class AtmosphericTimeSystem {
    // Continuous Solar Orbital Cycle (0.0 = Noon, 0.25 = Sunset, 0.5 = Midnight, 0.75 = Sunrise)
    private double worldTimeTicks = 6000.0; // Day 1 Mid-Morning baseline

    private final Vector3f sunDir = new Vector3f();
    private final Vector3f directLightColor = new Vector3f();
    private final Vector3f skyAmbientColor = new Vector3f();
    private final Vector3f groundAmbientColor = new Vector3f();
    private final Vector3f zenithColor = new Vector3f();
    private final Vector3f horizonColor = new Vector3f();
    private final Vector3f sunColor = new Vector3f();
    private final Vector3f underwaterFogColor = new Vector3f();

    public AtmosphericTimeSystem() {
        update(0.0, true, true); // Initialize lighting vectors for baseline time
    }

    public void update(double deltaTime, boolean inWorld, boolean isPaused) {
        if (inWorld && !isPaused) {
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
            RenderingConfig.UNDERWATER_DAY_FOG_COLOR.y * dayWeight + RenderingConfig.UNDERWATER_SUNSET_FOG_COLOR.y * twilightWeight + RenderingConfig.UNDERWATER_NIGHT_FOG_COLOR.z * nightWeight,
            RenderingConfig.UNDERWATER_DAY_FOG_COLOR.z * dayWeight + RenderingConfig.UNDERWATER_SUNSET_FOG_COLOR.z * twilightWeight + RenderingConfig.UNDERWATER_NIGHT_FOG_COLOR.z * nightWeight
        );

        // Dynamic exposure adaptation: slightly higher at night to preserve silhouette readability
        RenderingConfig.exposure = 1.0f * dayWeight + 1.15f * twilightWeight + 1.35f * nightWeight;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public double getWorldTimeTicks() {
        return worldTimeTicks;
    }

    public void setWorldTimeTicks(double worldTimeTicks) {
        this.worldTimeTicks = worldTimeTicks;
    }

    public Vector3f getSunDir() {
        return sunDir;
    }

    public Vector3f getDirectLightColor() {
        return directLightColor;
    }

    public Vector3f getSkyAmbientColor() {
        return skyAmbientColor;
    }

    public Vector3f getGroundAmbientColor() {
        return groundAmbientColor;
    }

    public Vector3f getZenithColor() {
        return zenithColor;
    }

    public Vector3f getHorizonColor() {
        return horizonColor;
    }

    public Vector3f getSunColor() {
        return sunColor;
    }

    public Vector3f getUnderwaterFogColor() {
        return underwaterFogColor;
    }
}

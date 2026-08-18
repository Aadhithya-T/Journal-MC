package com.mcjournal.client;

import org.joml.Vector3f;

/**
 * Centralized rendering configuration for the Minecraft Journal native OpenGL engine.
 * Eliminates scattered magic numbers and provides clean physical & artistic calibration parameters.
 */
public class RenderingConfig {

    // ==========================================
    // 1. COLOR MANAGEMENT & TONE MAPPING
    // ==========================================
    public static final float GAMMA = 2.2f;
    public static final float INV_GAMMA = 1.0f / GAMMA;
    public static final float TONE_MAP_WHITE_POINT = 1.8f;
    public static float exposure = 1.0f;

    // ==========================================
    // 2. DAYLIGHT BASELINE ILLUMINATION (UNTOUCHED)
    // ==========================================
    public static final Vector3f DAY_SUN_COLOR = new Vector3f(1.08f, 1.00f, 0.90f);
    public static final float DAY_SUN_INTENSITY = 0.58f;

    public static final Vector3f DAY_SKY_AMBIENT_COLOR = new Vector3f(0.55f, 0.65f, 0.80f);
    public static final float DAY_SKY_AMBIENT_STRENGTH = 0.46f;

    public static final Vector3f DAY_GROUND_AMBIENT_COLOR = new Vector3f(0.38f, 0.36f, 0.32f);
    public static final float DAY_GROUND_AMBIENT_STRENGTH = 0.42f;

    public static final Vector3f DAY_ZENITH_COLOR = new Vector3f(0.20f, 0.44f, 0.86f);
    public static final Vector3f DAY_HORIZON_COLOR = new Vector3f(0.72f, 0.84f, 0.98f);

    // ==========================================
    // 3. SUNSET / SUNRISE (UNTOUCHED)
    // ==========================================
    public static final Vector3f SUNSET_SUN_COLOR = new Vector3f(1.15f, 0.62f, 0.26f);
    public static final float SUNSET_SUN_INTENSITY = 0.48f;

    public static final Vector3f SUNSET_SKY_AMBIENT_COLOR = new Vector3f(0.46f, 0.38f, 0.52f);
    public static final float SUNSET_SKY_AMBIENT_STRENGTH = 0.40f;

    public static final Vector3f SUNSET_GROUND_AMBIENT_COLOR = new Vector3f(0.32f, 0.26f, 0.22f);
    public static final float SUNSET_GROUND_AMBIENT_STRENGTH = 0.35f;

    public static final Vector3f SUNSET_ZENITH_COLOR = new Vector3f(0.18f, 0.22f, 0.52f);
    public static final Vector3f SUNSET_HORIZON_COLOR = new Vector3f(0.92f, 0.56f, 0.34f);

    // ==========================================
    // 4. NIGHT (UNTOUCHED)
    // ==========================================
    public static final Vector3f NIGHT_MOON_COLOR = new Vector3f(0.55f, 0.70f, 0.95f);
    public static final float NIGHT_MOON_INTENSITY = 0.28f;

    public static final Vector3f NIGHT_SKY_AMBIENT_COLOR = new Vector3f(0.14f, 0.18f, 0.32f);
    public static final float NIGHT_SKY_AMBIENT_STRENGTH = 0.35f;

    public static final Vector3f NIGHT_GROUND_AMBIENT_COLOR = new Vector3f(0.08f, 0.10f, 0.18f);
    public static final float NIGHT_GROUND_AMBIENT_STRENGTH = 0.30f;

    public static final Vector3f NIGHT_ZENITH_COLOR = new Vector3f(0.04f, 0.06f, 0.14f);
    public static final Vector3f NIGHT_HORIZON_COLOR = new Vector3f(0.10f, 0.14f, 0.24f);

    // ==========================================
    // 5. ATMOSPHERIC PERSPECTIVE FOG (UNTOUCHED)
    // ==========================================
    public static final float FOG_START = 75.0f;
    public static final float FOG_END = 240.0f;

    // ==========================================
    // 6. STYLIZED WATER OPTICS & ABSORPTION (CALIBRATED)
    // ==========================================
    // Calibrated linear water colors (low red/green scattering to eliminate milky haze)
    public static final Vector3f WATER_SHALLOW_COLOR = new Vector3f(0.012f, 0.140f, 0.420f); // Pure crystal cyan-azure
    public static final Vector3f WATER_MID_COLOR = new Vector3f(0.005f, 0.065f, 0.380f);     // Vibrant natural cobalt
    public static final Vector3f WATER_DEEP_COLOR = new Vector3f(0.001f, 0.022f, 0.260f);    // Saturated rich sapphire
    public static final float WATER_FRESNEL_F0 = 0.02f;                                       // Dielectric water IOR ~ 1.333
    public static final float WATER_SPECULAR_POWER = 36.0f;                                   // Soft broad glisten
    public static final float WATER_SPECULAR_STRENGTH = 0.32f;
    public static final float WATER_ABSORPTION_MU = 0.42f;                                    // Beer-Lambert transmission rate

    // ==========================================
    // 7. AMBIENT OCCLUSION & FOLIAGE (UNTOUCHED)
    // ==========================================
    public static final float AO_MIN_CLAMP = 0.60f;
    public static final float LEAF_ALPHA_CUTOFF = 0.50f;

    // ==========================================
    // 8. DEBUG VISUALIZATION MODES
    // ==========================================
    public static final int DEBUG_MODE_NORMAL = 0;
    public static final int DEBUG_MODE_ALBEDO = 1;
    public static final int DEBUG_MODE_NORMALS = 2;
    public static final int DEBUG_MODE_DIRECT_LIGHT = 3;
    public static final int DEBUG_MODE_AMBIENT_LIGHT = 4;
    public static final int DEBUG_MODE_AO = 5;
    public static final int DEBUG_MODE_TOTAL_LIGHT = 6;
    public static final int DEBUG_MODE_PRE_TONEMAP = 7;
    public static final int DEBUG_MODE_FOG = 8;

    // Water-Specific Debug Modes
    public static final int DEBUG_MODE_WATER_ALBEDO = 11;
    public static final int DEBUG_MODE_WATER_DEPTH = 12;
    public static final int DEBUG_MODE_WATER_TRANSMISSION = 13;

    public static int currentDebugMode = DEBUG_MODE_NORMAL;
}

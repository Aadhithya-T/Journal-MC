#version 330 core

in vec3 vWorldPos;
in vec2 vUV;
in vec3 vColor;
in vec3 vNormal;
in float vFogDist;

uniform sampler2D uAtlas;
uniform vec3 uSunDir;
uniform vec3 uDirectLightColor;
uniform vec3 uSkyAmbientColor;
uniform vec3 uGroundAmbientColor;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform vec3 uCameraPos;
uniform float uTime;
uniform int uIsWater;
uniform int uIsUnderwater;
uniform float uExposure;
uniform int uDebugMode;

// Water parameters from RenderingConfig
uniform vec3 uWaterShallowColor;
uniform vec3 uWaterMidColor;
uniform vec3 uWaterDeepColor;
uniform float uWaterFresnelF0;
uniform float uWaterSpecularPower;
uniform float uWaterSpecularStrength;
uniform float uWaterAbsorptionMu;
uniform float uAoMinClamp;

out vec4 fragColor;

void main() {
    // 1. Texture Sample (Hardware decodes sRGB -> Linear RGB via GL_SRGB8_ALPHA8)
    vec4 texColor = texture(uAtlas, vUV);
    
    // Strict Alpha Cutout for Foliage / Leaves (Binary 0 or 255 alpha)
    if (uIsWater == 0 && texColor.a < 0.5) {
        discard;
    }

    vec3 norm = length(vNormal) > 0.1 ? normalize(vNormal) : vec3(0.0, 1.0, 0.0);
    vec3 lightDir = normalize(uSunDir);

    // 2. Physical Linear Lighting Calculations
    // Directional Half-Lambert / Diffuse component
    float NdotL = clamp(dot(norm, lightDir), 0.0, 1.0);
    vec3 directIllum = uDirectLightColor * NdotL;

    // Environmental Hemisphere Sky Light (Normals pointing up get sky color, down get ground bounce)
    float upFactor = clamp(norm.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 ambientIllum = mix(uGroundAmbientColor, uSkyAmbientColor, upFactor);

    // Total incident lighting (un-occluded)
    vec3 totalIllum = directIllum + ambientIllum;

    // Baked Ambient Occlusion (vColor carries pure vertex AO for solid: 0.42 to 1.0 -> scaled so corners never crush to zero)
    vec3 clampedAO = (uIsWater == 1) ? vec3(1.0) : mix(vec3(uAoMinClamp), vec3(1.0), vColor);
    vec3 finalLight = totalIllum * clampedAO;

    vec3 linearAlbedo = texColor.rgb;
    vec3 linearRadiance;
    float waterAlpha = 1.0;
    float waterDepth = 0.0;
    float transmission = 1.0;
    float fresnel = 0.0;
    vec3 specularReflection = vec3(0.0);
    vec3 waterBodyColor = vec3(0.0);

    // 3. Water Optical Shading (Beer-Lambert Absorption, 3-Stop Depth Gradient, Fresnel Reflection, Specular)
    if (uIsWater == 1) {
        // vColor.r = physical water column depth in normalized 8-block units (0.0 to 2.0)
        // vColor.g = shoreline adjacency factor (1.0 at shore, 0.0 in open water)
        waterDepth = clamp(vColor.r * 8.0, 0.4, 16.0);
        float shorelineFactor = vColor.g;

        // A. Subtle Organic World-Space Surface Wave Normal (Continuous world coords: NO UV grid artifacts!)
        vec2 waveCoord1 = vWorldPos.xz * 0.16 + vec2(uTime * 0.06, uTime * 0.04);
        vec2 waveCoord2 = vWorldPos.zx * 0.22 - vec2(uTime * 0.04, uTime * 0.07);
        float waveN1 = sin(waveCoord1.x * 2.5 + waveCoord1.y * 3.0);
        float waveN2 = cos(waveCoord2.x * 3.0 - waveCoord2.y * 2.6);
        vec3 surfaceNorm = normalize(vec3(waveN1 * 0.040, 1.0, waveN2 * 0.040));

        // B. Beer-Lambert Optical Transmission / Transparency: T = exp(-mu * depth)
        // Shallow shoreline (depth ~ 0.4-1.0): transmission 0.65-0.84 -> alpha 0.38-0.52 (riverbed clearly visible)
        // Medium depth (depth ~ 2.5-4.0): transmission 0.18-0.35 -> alpha 0.68-0.78 (soft bottom visibility)
        // Deep lake/ocean (depth >= 6.0): transmission 0.01-0.08 -> alpha 0.88-0.92 (rich, saturated, dark absorption)
        transmission = exp(-uWaterAbsorptionMu * waterDepth);
        waterAlpha = mix(0.90, 0.38, transmission);

        // C. 3-Stop Chromatic Depth Gradient (Linear Space)
        // depth < 2.5: Shallow crystal cyan -> Mid natural cobalt blue
        // depth >= 2.5: Mid cobalt blue -> Deep saturated sapphire
        float depthFactor = clamp(waterDepth / 6.0, 0.0, 1.0);
        if (depthFactor < 0.45) {
            waterBodyColor = mix(uWaterShallowColor, uWaterMidColor, depthFactor / 0.45);
        } else {
            waterBodyColor = mix(uWaterMidColor, uWaterDeepColor, (depthFactor - 0.45) / 0.55);
        }

        // Shoreline soft transition (subtle turquoise clarity at land edge)
        if (shorelineFactor > 0.1) {
            waterBodyColor = mix(waterBodyColor, uWaterShallowColor * 1.10, shorelineFactor * 0.25);
        }

        // D. Schlick's Fresnel Surface Reflection (F0 = 0.02 for dielectric water)
        vec3 viewDir = normalize(uCameraPos - vWorldPos);
        float NdotV = clamp(dot(surfaceNorm, viewDir), 0.0, 1.0);
        fresnel = uWaterFresnelF0 + (1.0 - uWaterFresnelF0) * pow(1.0 - NdotV, 5.0);

        // Sky Reflection on surface (stronger at glancing angles, subtler at top-down view)
        vec3 skyReflectColor = mix(uGroundAmbientColor, uSkyAmbientColor, 0.90);
        vec3 waterSurface = mix(waterBodyColor * finalLight, skyReflectColor, fresnel * 0.48);

        // E. Subtle Sun/Moon Glisten Highlight
        vec3 halfDir = normalize(lightDir + viewDir);
        float spec = pow(max(0.0, dot(surfaceNorm, halfDir)), uWaterSpecularPower);
        specularReflection = uDirectLightColor * (spec * uWaterSpecularStrength);

        linearRadiance = waterSurface + specularReflection;
    } else {
        linearRadiance = linearAlbedo * finalLight;
    }

    // 4. Atmospheric & Underwater Perspective Fog (Linear HDR Space before Tone Mapping)
    float fogFactor = smoothstep(uFogStart, uFogEnd, vFogDist);
    vec3 linearSceneColor;

    if (uIsUnderwater == 1) {
        // Dense aquatic depth fog and light absorption
        vec3 aquaticTintedRadiance = linearRadiance * vec3(0.65, 0.88, 1.10);
        linearSceneColor = mix(aquaticTintedRadiance, uFogColor, fogFactor);
    } else if (uIsWater == 1) {
        // Preserves the distinct blue identity of distant water so lakes don't bleach to white horizon haze
        vec3 waterFogColor = mix(uFogColor, vec3(0.015, 0.08, 0.32), 0.32);
        linearSceneColor = mix(linearRadiance, waterFogColor, fogFactor * 0.80);
    } else {
        linearSceneColor = mix(linearRadiance, uFogColor, fogFactor);
    }

    // 5. Exposure Adaptation
    vec3 exposedColor = linearSceneColor * uExposure;

    // 6. Filmic / Soft-Shoulder Extended Reinhard Tone Mapping (HDR -> [0, 1] range)
    const float W = 1.8; // White point
    vec3 toneMapped = (exposedColor * (vec3(1.0) + exposedColor / (W * W))) / (exposedColor + vec3(1.0));

    // 7. Gamma Correction: Convert Linear RGB -> sRGB for Display Output
    vec3 finalDisplayColor = pow(toneMapped, vec3(1.0 / 2.2));

    // ==========================================
    // DEBUG VISUALIZATION SUITE (F1 - F9, F10 - F12)
    // ==========================================
    if (uDebugMode == 1) {
        // F2: Albedo Only
        fragColor = vec4(pow(linearAlbedo, vec3(1.0 / 2.2)), 1.0);
        return;
    } else if (uDebugMode == 2) {
        // F3: World Normals
        fragColor = vec4(norm * 0.5 + 0.5, 1.0);
        return;
    } else if (uDebugMode == 3) {
        // F4: Direct Sunlight Only
        fragColor = vec4(pow(directIllum, vec3(1.0 / 2.2)), 1.0);
        return;
    } else if (uDebugMode == 4) {
        // F5: Ambient Hemisphere Sky Light Only
        fragColor = vec4(pow(ambientIllum, vec3(1.0 / 2.2)), 1.0);
        return;
    } else if (uDebugMode == 5) {
        // F6: Baked Ambient Occlusion Only
        fragColor = vec4(clampedAO, 1.0);
        return;
    } else if (uDebugMode == 6) {
        // F7: Total Incident Lighting (Pre-Albedo)
        fragColor = vec4(pow(finalLight, vec3(1.0 / 2.2)), 1.0);
        return;
    } else if (uDebugMode == 7) {
        // F8: Linear HDR Pre-Tonemap
        fragColor = vec4(clamp(exposedColor, 0.0, 1.0), 1.0);
        return;
    } else if (uDebugMode == 8) {
        // F9: Fog Factor Grayscale
        fragColor = vec4(vec3(fogFactor), 1.0);
        return;
    } else if (uDebugMode == 11) {
        // F10: Water Albedo / Base Depth Color
        if (uIsWater == 1) fragColor = vec4(pow(waterBodyColor, vec3(1.0 / 2.2)), 1.0);
        else fragColor = vec4(0.1, 0.1, 0.1, 1.0);
        return;
    } else if (uDebugMode == 12) {
        // F11: Water Depth Map (0 to 8 blocks normalized)
        if (uIsWater == 1) fragColor = vec4(vec3(waterDepth / 8.0), 1.0);
        else fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    } else if (uDebugMode == 13) {
        // F12: Beer-Lambert Transmission (T = exp(-mu * d))
        if (uIsWater == 1) fragColor = vec4(vec3(transmission), 1.0);
        else fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    float alpha = (uIsWater == 1) ? waterAlpha : 1.0;
    fragColor = vec4(finalDisplayColor, alpha);
}

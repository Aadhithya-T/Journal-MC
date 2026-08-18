#version 330 core

in vec3 vWorldDir;

uniform vec3 uSunDir;
uniform vec3 uZenithColor;
uniform vec3 uHorizonColor;
uniform vec3 uSunColor;
uniform float uTimeOfDay; // 0.0 = Noon, 0.25 = Sunset, 0.5 = Midnight, 0.75 = Sunrise

out vec4 fragColor;

// Fast procedural pseudo-random hash for night stars
float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

void main() {
    vec3 dir = normalize(vWorldDir);
    float height = dir.y;
    
    // 1. Dynamic Atmosphere Sky Gradient
    vec3 skyColor;
    if (height > 0.0) {
        float hFactor = clamp(height, 0.0, 1.0);
        skyColor = mix(uHorizonColor, uZenithColor, pow(hFactor, 0.65));
    } else {
        // Subtle below-horizon haze
        skyColor = mix(uHorizonColor, uHorizonColor * 0.65, clamp(-height * 2.5, 0.0, 1.0));
    }

    // 2. Night Sky Stars (Visible when sun is below horizon)
    float sunElevation = uSunDir.y;
    if (sunElevation < 0.15 && height > 0.08) {
        float nightFactor = clamp((0.15 - sunElevation) / 0.30, 0.0, 1.0);
        vec3 starGrid = floor(dir * 220.0);
        float starVal = hash(starGrid);
        if (starVal > 0.988) {
            float twinkle = sin(starVal * 60.0 + dir.x * 20.0) * 0.3 + 0.7;
            skyColor += vec3(0.9, 0.95, 1.0) * (starVal - 0.988) * 60.0 * twinkle * nightFactor;
        }
    }

    // 3. Sun Disc & Sunset Corona (Day & Twilight)
    vec3 sunDir = normalize(uSunDir);
    float sunDot = dot(dir, sunDir);

    if (sunElevation > -0.15 && sunDot > 0.70) {
        float sunAngle = acos(clamp(sunDot, -1.0, 1.0));
        
        vec3 sunUp = vec3(0.0, 1.0, 0.0);
        vec3 sunRight = normalize(cross(sunDir, sunUp));
        sunUp = cross(sunRight, sunDir);
        
        float x = dot(dir, sunRight);
        float y = dot(dir, sunUp);
        
        // Square Minecraft Sun Disc
        if (abs(x) < 0.042 && abs(y) < 0.042 && sunDot > 0.99) {
            skyColor = uSunColor * 1.35;
        } else {
            // Warm atmospheric corona glow
            float glow = exp(-sunAngle * 7.5) * 0.60;
            skyColor += uSunColor * glow;
        }
    }

    // 4. Moon Disc (Night)
    vec3 moonDir = -sunDir;
    float moonDot = dot(dir, moonDir);
    if (sunElevation < 0.20 && moonDot > 0.85) {
        vec3 moonUp = vec3(0.0, 1.0, 0.0);
        vec3 moonRight = normalize(cross(moonDir, moonUp));
        moonUp = cross(moonRight, moonDir);
        
        float mx = dot(dir, moonRight);
        float my = dot(dir, moonUp);
        
        // Square Minecraft Moon Disc
        if (abs(mx) < 0.038 && abs(my) < 0.038 && moonDot > 0.992) {
            skyColor = vec3(0.92, 0.95, 1.0);
        } else {
            float moonGlow = exp(-acos(clamp(moonDot, -1.0, 1.0)) * 9.0) * 0.25;
            skyColor += vec3(0.70, 0.80, 1.0) * moonGlow;
        }
    }

    // Convert linear sky radiance to sRGB for display
    vec3 displaySky = pow(clamp(skyColor, 0.0, 1.5), vec3(1.0 / 2.2));
    fragColor = vec4(displaySky, 1.0);
}

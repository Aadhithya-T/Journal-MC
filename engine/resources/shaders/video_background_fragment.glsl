#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform float uTime;
uniform vec2 uResolution;
uniform int uTheme; // 0 = Aurora Night, 1 = Coral Reef, 2 = Cozy Campfire

// Noise helper functions
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 5; ++i) {
        v += a * noise(p);
        p = rot * p * 2.0 + vec2(100.0);
        a *= 0.5;
    }
    return v;
}

// -------------------------------------------------------------
// THEME 0: Aurora Night (Shimmering green/cyan curtains & stars)
// -------------------------------------------------------------
vec3 renderAurora(vec2 uv, float time) {
    vec3 sky = mix(vec3(0.02, 0.03, 0.08), vec3(0.06, 0.08, 0.18), uv.y);

    // Stars
    float stars = pow(hash(floor(uv * 180.0)), 28.0) * 1.5;
    sky += vec3(stars);

    // Aurora ribbons
    for (int i = 0; i < 3; i++) {
        float speed = time * 0.2 + float(i) * 1.5;
        float wave = sin(uv.x * 3.0 + speed) * 0.15 + cos(uv.x * 5.0 - speed * 0.7) * 0.08;
        float yPos = 0.45 + float(i) * 0.12 + wave;
        float d = abs(uv.y - yPos);
        
        float ribbon = exp(-d * 12.0) * fbm(uv * 4.0 + vec2(speed * 0.4, speed * 0.1));
        vec3 auroraCol = mix(vec3(0.1, 0.9, 0.5), vec3(0.1, 0.4, 0.9), float(i) * 0.4);
        sky += ribbon * auroraCol * 1.8;
    }

    // Mountain silhouettes at bottom
    float mountain = sin(uv.x * 4.0) * 0.08 + sin(uv.x * 12.0) * 0.03 + 0.12;
    if (uv.y < mountain) {
        sky = mix(vec3(0.01, 0.01, 0.03), sky, uv.y / mountain * 0.3);
    }

    return sky;
}

// -------------------------------------------------------------
// THEME 1: Coral Reef (Underwater caustics, sunrays & deep turquoise)
// -------------------------------------------------------------
vec3 renderCoralReef(vec2 uv, float time) {
    // Deep underwater gradient
    vec3 deepWater = vec3(0.01, 0.08, 0.18);
    vec3 shallowWater = vec3(0.04, 0.32, 0.52);
    vec3 col = mix(deepWater, shallowWater, uv.y);

    // Caustics wave pattern
    vec2 p = uv * 6.0;
    p.y += time * 0.15;
    float c1 = sin(p.x + sin(p.y + time * 0.4));
    float c2 = cos(p.y + cos(p.x + time * 0.3));
    float caustics = pow((c1 + c2) * 0.5 + 0.5, 3.0);

    // Sunrays from surface
    float rays = sin(uv.x * 5.0 + uv.y * 3.0 + time * 0.3) * 0.5 + 0.5;
    rays *= pow(uv.y, 2.0) * 0.6;

    col += vec3(0.1, 0.65, 0.8) * caustics * 0.45;
    col += vec3(0.3, 0.8, 0.95) * rays;

    // Distant coral silhouette at bottom
    float seaFloor = sin(uv.x * 6.0) * 0.06 + 0.10;
    if (uv.y < seaFloor) {
        col = mix(vec3(0.01, 0.05, 0.10), col, 0.2);
    }

    return col;
}

// -------------------------------------------------------------
// THEME 2: Cozy Campfire (Night forest with flickering fire & embers)
// -------------------------------------------------------------
vec3 renderCozyCampfire(vec2 uv, float time) {
    // Midnight dark forest sky
    vec3 col = mix(vec3(0.04, 0.02, 0.02), vec3(0.01, 0.01, 0.02), uv.y);

    // Campfire position at bottom-center
    vec2 firePos = vec2(0.5, 0.15);
    vec2 d = uv - firePos;

    // Flickering radial warm glow
    float flicker = sin(time * 12.0) * 0.08 + sin(time * 23.0) * 0.05 + 1.0;
    float dist = length(d);
    float glow = exp(-dist * 3.0) * flicker;
    vec3 warmLight = mix(vec3(1.0, 0.25, 0.02), vec3(1.0, 0.7, 0.1), exp(-dist * 6.0));
    col += warmLight * glow * 1.5;

    // Rising Flame shape
    if (abs(d.x) < 0.15 && d.y > -0.05 && d.y < 0.35) {
        float flameNoise = fbm(uv * 8.0 - vec2(0.0, time * 3.0));
        float flameShape = (1.0 - (d.y / 0.35)) * (1.0 - abs(d.x) / 0.15);
        float flame = pow(flameShape * flameNoise, 1.5) * 2.5;
        col += vec3(1.0, 0.6, 0.1) * flame;
    }

    // Rising glowing ember particles
    for (int i = 0; i < 15; i++) {
        float seed = float(i) * 13.37;
        float px = 0.5 + sin(seed + time * 0.8) * (0.1 + float(i) * 0.015);
        float py = fract(seed + time * (0.2 + hash(vec2(seed, 0.0)) * 0.3));
        float pDist = length(uv - vec2(px, py));
        float ember = exp(-pDist * 80.0);
        col += vec3(1.0, 0.5, 0.05) * ember * 1.8;
    }

    return col;
}

void main() {
    vec3 finalColor;

    if (uTheme == 0) {
        finalColor = renderAurora(vUv, uTime);
    } else if (uTheme == 1) {
        finalColor = renderCoralReef(vUv, uTime);
    } else {
        finalColor = renderCozyCampfire(vUv, uTime);
    }

    FragColor = vec4(finalColor, 1.0);
}

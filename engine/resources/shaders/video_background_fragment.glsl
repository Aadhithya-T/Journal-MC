#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uTex0;
uniform sampler2D uTex1;
uniform float uBlend;
uniform vec2 uUvScale;
uniform vec2 uUvOffset;

void main() {
    // 1. Aspect Ratio Cover Fit UV mapping
    vec2 fittedUv = (vUv - 0.5) * uUvScale + 0.5 + uUvOffset;

    // 2. High-Fidelity Dual-Frame Temporal Crossfading
    vec4 col0 = texture(uTex0, fittedUv);
    vec4 col1 = texture(uTex1, fittedUv);
    vec4 videoCol = mix(col0, col1, uBlend);

    // 3. Cinematic Subtle Vignette
    vec2 d = vUv - vec2(0.5, 0.5);
    float vignette = 1.0 - dot(d, d) * 0.45;
    videoCol.rgb *= vignette;

    FragColor = videoCol;
}

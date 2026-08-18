#version 330 core

in vec2 vUV;
in vec3 vColor;
in float vFogDist;

uniform sampler2D uAtlas;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uAtlas, vUV);
    
    // Discard transparent pixels (foliage cutouts)
    if (texColor.a < 0.1) {
        discard;
    }

    // Multiply texture color with baked Ambient Occlusion and directional base shade
    vec3 litColor = texColor.rgb * vColor;

    // Linear distance fog (vanilla Minecraft formula)
    float fogFactor = clamp((uFogEnd - vFogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
    vec3 finalColor = mix(uFogColor, litColor, fogFactor);

    fragColor = vec4(finalColor, texColor.a);
}

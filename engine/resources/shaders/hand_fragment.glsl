#version 330 core

in vec3 vWorldPos;
in vec2 vUV;
in vec3 vColor;
in vec3 vNormal;

uniform sampler2D uTexture;
uniform vec3 uSunDir;
uniform vec3 uDirectLightColor;
uniform vec3 uSkyAmbientColor;
uniform vec3 uGroundAmbientColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vUV);
    if (texColor.a < 0.1) discard;

    vec3 normal = normalize(vNormal);

    // Directional sunlight diffuse
    float NdotL = max(dot(normal, uSunDir), 0.0);

    // Hemispherical ambient light (Sky vs Ground)
    float hemi = normal.y * 0.5 + 0.5;
    vec3 ambient = mix(uGroundAmbientColor, uSkyAmbientColor, hemi);

    // Total lighting model for hand viewmodel (Authentic Minecraft illumination)
    vec3 totalLight = ambient * 1.05 + uDirectLightColor * (NdotL * 0.72 + 0.28);

    vec3 finalColor = texColor.rgb * totalLight;

    fragColor = vec4(finalColor, 1.0);
}

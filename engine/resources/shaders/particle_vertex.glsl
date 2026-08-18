#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aUv;

out vec2 vUv;
out vec4 vColor;

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uParticlePos;
uniform float uParticleScale;
uniform vec4 uParticleColor;

void main() {
    vUv = aUv;
    vColor = uParticleColor;

    // Billboard quad or 3D mini-cube oriented in world space
    vec3 worldPos = aPos * uParticleScale + uParticlePos;
    gl_Position = uProjection * uView * vec4(worldPos, 1.0);
}

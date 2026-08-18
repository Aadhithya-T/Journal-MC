#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aColor;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 vUV;
out vec3 vColor;
out float vFogDist;

void main() {
    vec4 viewPos = uView * vec4(aPos, 1.0);
    gl_Position = uProjection * viewPos;
    vUV = aUV;
    vColor = aColor;
    vFogDist = length(viewPos.xyz);
}

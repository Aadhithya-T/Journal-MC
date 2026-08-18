#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec3 aNormal;

uniform mat4 uProjection;
uniform mat4 uView;

out vec3 vWorldPos;
out vec2 vUV;
out vec3 vColor;
out vec3 vNormal;
out float vFogDist;

void main() {
    vWorldPos = aPos;
    vec4 viewPos = uView * vec4(aPos, 1.0);
    gl_Position = uProjection * viewPos;
    
    vUV = aUV;
    vColor = aColor;
    vNormal = aNormal;
    vFogDist = length(viewPos.xyz);
}

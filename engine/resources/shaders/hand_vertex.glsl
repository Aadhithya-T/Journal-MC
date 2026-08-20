#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec3 aNormal;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec3 vWorldPos;
out vec2 vUV;
out vec3 vColor;
out vec3 vNormal;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;
    gl_Position = uProjection * uView * worldPos;

    vUV = aUV;
    vColor = aColor;
    vNormal = normalize(mat3(uModel) * aNormal);
}

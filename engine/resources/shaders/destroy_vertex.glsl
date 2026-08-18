#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aUv;

out vec2 vUv;

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uBlockPos;

void main() {
    vUv = aUv;
    gl_Position = uProjection * uView * vec4(aPos + uBlockPos, 1.0);
}

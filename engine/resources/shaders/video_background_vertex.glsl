#version 330 core
layout (location = 0) in vec2 aPos;

out vec2 vUv;

void main() {
    // Correct upright UV orientation (top-left is (0, 0), bottom-right is (1, 1))
    vUv = vec2((aPos.x + 1.0) * 0.5, 1.0 - (aPos.y + 1.0) * 0.5);
    gl_Position = vec4(aPos, 0.0, 1.0);
}

#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uDestroyTex;

void main() {
    vec4 tex = texture(uDestroyTex, vUv);
    if (tex.a < 0.05) discard;
    FragColor = tex;
}

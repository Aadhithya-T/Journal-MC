#version 330 core

in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTexture;
uniform int uUseTexture;

out vec4 fragColor;

void main() {
    if (uUseTexture == 1) {
        vec4 texColor = texture(uTexture, vUV);
        if (texColor.a < 0.05) {
            discard;
        }
        fragColor = texColor * vColor;
    } else {
        fragColor = vColor;
    }
}

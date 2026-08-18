#version 330 core

layout(location = 0) in vec3 aPos;

uniform mat4 uProjection;
uniform mat4 uView;

out vec3 vWorldDir;

void main() {
    // Remove translation from view matrix so sky dome remains centered around player
    mat4 rotView = mat4(mat3(uView));
    vec4 clipPos = uProjection * rotView * vec4(aPos, 1.0);
    
    // Set z to w so depth is always 1.0 (furthest background)
    gl_Position = clipPos.xyww;
    vWorldDir = aPos;
}

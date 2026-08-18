package com.mcjournal.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String vertexResourcePath, String fragmentResourcePath) {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create OpenGL Shader Program");
        }

        String vertexSource = loadResource(vertexResourcePath);
        String fragmentSource = loadResource(fragmentResourcePath);

        int vShader = createShader(vertexSource, GL_VERTEX_SHADER);
        int fShader = createShader(fragmentSource, GL_FRAGMENT_SHADER);

        glAttachShader(programId, vShader);
        glAttachShader(programId, fShader);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking Shader Program: " + glGetProgramInfoLog(programId, 1024));
        }

        glDetachShader(programId, vShader);
        glDetachShader(programId, fShader);
        glDeleteShader(vShader);
        glDeleteShader(fShader);
    }

    private int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader of type " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling Shader: " + glGetShaderInfoLog(shaderId, 1024));
        }

        return shaderId;
    }

    public static String loadResource(String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

        // 1. Try Classpath
        try (InputStream in = ShaderProgram.class.getResourceAsStream("/" + cleanPath)) {
            if (in != null) {
                return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception ignored) {}

        // 2. Try Local File System
        java.io.File file = new java.io.File("engine/resources/" + cleanPath);
        if (file.exists()) {
            try {
                return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read shader file: " + file.getAbsolutePath(), e);
            }
        }

        throw new RuntimeException("Shader resource not found: " + path);
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    public int getUniformLocation(String uniformName) {
        if (uniforms.containsKey(uniformName)) {
            return uniforms.get(uniformName);
        }
        int location = glGetUniformLocation(programId, uniformName);
        uniforms.put(uniformName, location);
        return location;
    }

    public void setUniform(String uniformName, int value) {
        glUniform1i(getUniformLocation(uniformName), value);
    }

    public void setUniform(String uniformName, float value) {
        glUniform1f(getUniformLocation(uniformName), value);
    }

    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(getUniformLocation(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, Vector4f value) {
        glUniform4f(getUniformLocation(uniformName), value.x, value.y, value.z, value.w);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(getUniformLocation(uniformName), false, fb);
        }
    }
}

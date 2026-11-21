package com.mcjournal.client;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {
    private long handle;
    private int windowWidth;
    private int windowHeight;
    private int framebufferWidth;
    private int framebufferHeight;
    private String title;
    private boolean resized;
    private boolean isCursorLocked = false;

    public Window(String title, int width, int height) {
        this.title = title;
        this.windowWidth = width;
        this.windowHeight = height;
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.resized = false;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        handle = glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetWindowSizeCallback(handle, (window, w, h) -> {
            this.windowWidth = Math.max(1, w);
            this.windowHeight = Math.max(1, h);
            this.resized = true;
        });

        glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            this.framebufferWidth = Math.max(1, w);
            this.framebufferHeight = Math.max(1, h);
            this.resized = true;
            glViewport(0, 0, this.framebufferWidth, this.framebufferHeight);
        });

        // Query initial physical framebuffer and logical window sizes
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWinW = stack.mallocInt(1);
            IntBuffer pWinH = stack.mallocInt(1);
            IntBuffer pFbW = stack.mallocInt(1);
            IntBuffer pFbH = stack.mallocInt(1);

            glfwGetWindowSize(handle, pWinW, pWinH);
            glfwGetFramebufferSize(handle, pFbW, pFbH);

            this.windowWidth = Math.max(1, pWinW.get(0));
            this.windowHeight = Math.max(1, pWinH.get(0));
            this.framebufferWidth = Math.max(1, pFbW.get(0));
            this.framebufferHeight = Math.max(1, pFbH.get(0));

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                    handle,
                    (vidmode.width() - this.windowWidth) / 2,
                    (vidmode.height() - this.windowHeight) / 2
                );
            }
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1); // Enable V-Sync (60 FPS smooth)
        glfwShowWindow(handle);

        GL.createCapabilities();

        // Initial OpenGL State
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Alpha Blending for water / foliage
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glClearColor(0.47f, 0.65f, 1.0f, 1.0f); // Minecraft sky blue
    }

    public void lockCursor(boolean lock) {
        this.isCursorLocked = lock;
        if (lock) {
            glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    public void setVsync(boolean vsync) {
        glfwSwapInterval(vsync ? 1 : 0);
    }

    public boolean isCursorLocked() {
        return isCursorLocked;
    }

    public void update() {
        glfwSwapBuffers(handle);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void close() {
        glfwSetWindowShouldClose(handle, true);
    }

    public void destroy() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) callback.free();
    }

    public long getHandle() {
        return handle;
    }

    /**
     * Returns framebuffer width in physical pixels (OpenGL viewport).
     */
    public int getWidth() {
        return framebufferWidth;
    }

    /**
     * Returns framebuffer height in physical pixels (OpenGL viewport).
     */
    public int getHeight() {
        return framebufferHeight;
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public float getAspectRatio() {
        return (float) framebufferWidth / (float) (framebufferHeight > 0 ? framebufferHeight : 1);
    }

    public boolean isResized() {
        return resized;
    }

    public void setResized(boolean resized) {
        this.resized = resized;
    }
}

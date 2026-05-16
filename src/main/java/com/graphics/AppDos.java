package com.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class AppDos {

    private long window;

    public void run() {
        init();
        loop();
        destroy();
    }

    private void init() {
        GLFW.glfwInit();

        window = GLFW.glfwCreateWindow(
                800,
                600,
                "Mi Juego",
                0,
                0
        );

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GLFW.glfwShowWindow(window);
    }

    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void destroy() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new AppDos().run();
    }
}
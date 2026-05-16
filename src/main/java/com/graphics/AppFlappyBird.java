package com.graphics;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class AppFlappyBird {

    private static final int ANCHO = 1100;
    private static final int ALTO  = 720;

    private static final float BIRD_X     = -0.45f;
    private static final float BIRD_ANCHO = 0.10f;
    private static final float BIRD_ALTO  = 0.09f;

    private static final float GRAVEDAD            = -1.9f;
    private static final float IMPULSO_SALTO       = 0.85f;
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    private static final float TUBERIA_ANCHO  = 0.18f;
    private static final float GAP_ALTO_BASE  = 0.48f;
    private static final float GAP_ALTO_MIN   = 0.28f;
    private static final float GAP_MIN_CENTRO = -0.38f;
    private static final float GAP_MAX_CENTRO =  0.38f;

    // Dificultad progresiva.
    private static final float VEL_BASE         = 0.62f;
    private static final float SPAWN_BASE        = 1.50f;
    private static final float VEL_PASO          = 0.15f;
    private static final float SPAWN_PASO        = 0.13f;
    private static final int   NIVEL_MAX         = 5;
    private static final int   PUNTOS_POR_NIVEL  = 5;

    // Suelo (zona muerta visual, los pajaros colisionan al llegar aqui).
    private static final float SUELO_Y    = -0.88f;
    private static final float SUELO_ALTO = 0.14f;

    // Nubes: cada nube = {x, y, escala}.
    private static final float CLOUD_SPEED = 0.07f;
    private final float[][] nubes = {
        {-0.50f, 0.65f, 1.0f},
        { 0.15f, 0.74f, 0.7f},
        { 0.70f, 0.60f, 1.2f},
        {-0.85f, 0.70f, 0.8f},
        { 1.10f, 0.67f, 0.9f},
    };

    // -------------------------------------------------------------------------
    // OpenGL.
    // -------------------------------------------------------------------------
    private long window;
    private int  programaShader;
    private int  vaoQuad, vboQuad, vaoTri, vboTri;
    private int  uOffsetLoc, uScaleLoc, uColorLoc, uRotLoc;

    // -------------------------------------------------------------------------
    // Estado del juego.
    // -------------------------------------------------------------------------
    private final Bird[] birds = {
        new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE),
        new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP)
    };

    private float   timerGeneracion;
    private boolean started, gameOver, teclaReinicioPulsada;
    private boolean enCuentaRegresiva;
    private float   tiempoCuentaRegresiva;
    private float   alturaHueco             = GAP_ALTO_BASE;
    private int     nivelActual         = 1;
    private float   velocidadTuberias         = VEL_BASE;
    private float   tiempoEntreTuberias = SPAWN_BASE;

    private final List<Tuberia> tuberias = new ArrayList<>();
    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Ciclo principal.
    // -------------------------------------------------------------------------
    public void run() { 
        inicializar(); 
        resetGame(); 
        bucleDeJuego();
        liberarRecursos(); 
    }

    private void inicializar() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("No se pudo iniciar GLFW");
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,   GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "", 0, 0);
        if (window == 0) throw new RuntimeException("No se pudo crear la ventana");
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        crearShaders();
        crearGeometriaRectangulo();
        crearGeometriaTriangulo();
    }

    // -------------------------------------------------------------------------
    // Shaders con uRotation para inclinar el pajaro.
    // -------------------------------------------------------------------------
    private void crearShaders() {
        String codigoVertexShader = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2  uOffset;
            uniform vec2  uScale;
            uniform float uRotation;
            void main() {
                vec2 s = aPos.xy * uScale;
                float c = cos(uRotation), ss = sin(uRotation);
                vec2 r = vec2(s.x*c - s.y*ss, s.x*ss + s.y*c);
                gl_Position = vec4(r + uOffset, aPos.z, 1.0);
            }
            """;
        String codigoFragmentShader = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() { fragColor = vec4(uColor, 1.0); }
            """;
        int idVertexShader   = compilarShader(codigoVertexShader,   GL20.GL_VERTEX_SHADER,   "VS");
        int idFragmentShader = compilarShader(codigoFragmentShader, GL20.GL_FRAGMENT_SHADER, "FS");
        programaShader = GL20.glCreateProgram();
        GL20.glAttachShader(programaShader, idVertexShader); GL20.glAttachShader(programaShader, idFragmentShader);
        GL20.glLinkProgram(programaShader);
        if (GL20.glGetProgrami(programaShader, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Link: " + GL20.glGetProgramInfoLog(programaShader));
        uOffsetLoc = GL20.glGetUniformLocation(programaShader, "uOffset");
        uScaleLoc  = GL20.glGetUniformLocation(programaShader, "uScale");
        uColorLoc  = GL20.glGetUniformLocation(programaShader, "uColor");
        uRotLoc    = GL20.glGetUniformLocation(programaShader, "uRotation");
        GL20.glDeleteShader(idVertexShader); GL20.glDeleteShader(idFragmentShader);
    }

    private int compilarShader(String codigoFuente, int tipo, String nombre) {
        int idShader = GL20.glCreateShader(tipo);
        GL20.glShaderSource(idShader, codigoFuente); GL20.glCompileShader(idShader);
        if (GL20.glGetShaderi(idShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(nombre + ": " + GL20.glGetShaderInfoLog(idShader));
        return idShader;
    }

    // -------------------------------------------------------------------------
    // Geometria base.
    // -------------------------------------------------------------------------
    private int[] crearVAO(float[] verticesFloat) {
        int vao = GL30.glGenVertexArrays(); GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer bufferVertices = BufferUtils.createFloatBuffer(verticesFloat.length);
        bufferVertices.put(verticesFloat).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferVertices, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); GL30.glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    private void crearGeometriaRectangulo() {
        int[] resultadoVAO = crearVAO(new float[]{
            -0.5f,-0.5f,0f, 0.5f,-0.5f,0f, 0.5f,0.5f,0f,
            -0.5f,-0.5f,0f, 0.5f, 0.5f,0f,-0.5f,0.5f,0f });
        vaoQuad = resultadoVAO[0]; vboQuad = resultadoVAO[1];
    }

    private void crearGeometriaTriangulo() {
        int[] resultadoVAO = crearVAO(new float[]{ -0.5f,-0.5f,0f, 0.5f,0f,0f, -0.5f,0.5f,0f });
        vaoTri = resultadoVAO[0]; vboTri = resultadoVAO[1];
    }

    // -------------------------------------------------------------------------
    // Primitivas de dibujo.
    // -------------------------------------------------------------------------
    private void dibujarRectangulo(float x, float y, float ancho, float alto, float rotacion,
                                   float rojo, float verde, float azul) {
        GL30.glBindVertexArray(vaoQuad);
        GL20.glUniform2f(uOffsetLoc, x, y);         GL20.glUniform2f(uScaleLoc, ancho, alto);
        GL20.glUniform1f(uRotLoc, rotacion);         GL20.glUniform3f(uColorLoc, rojo, verde, azul);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }
    private void dibujarRectangulo(float x, float y, float ancho, float alto,
                                   float rojo, float verde, float azul) {
        dibujarRectangulo(x, y, ancho, alto, 0f, rojo, verde, azul);
    }

    private void dibujarTriangulo(float x, float y, float ancho, float alto, float rotacion,
                                  float rojo, float verde, float azul) {
        GL30.glBindVertexArray(vaoTri);
        GL20.glUniform2f(uOffsetLoc, x, y);         GL20.glUniform2f(uScaleLoc, ancho, alto);
        GL20.glUniform1f(uRotLoc, rotacion);         GL20.glUniform3f(uColorLoc, rojo, verde, azul);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    // Rota un offset local (lx,ly) segun el angulo de inclinacion del pajaro.
    private float[] rotarOffset(float lx, float ly, float angulo) {
        float coseno = (float)Math.cos(angulo), seno = (float)Math.sin(angulo);
        return new float[]{ lx*coseno - ly*seno, lx*seno + ly*coseno };
    }

    // -------------------------------------------------------------------------
    // Dibujo del pajaro compuesto.
    // -------------------------------------------------------------------------
    private void dibujarPajaro(float x, float y, float velocidadY, Bird bird) {
        float factorBrillo    = bird.alive ? 1.0f : 0.35f;   // muerto se ve mas oscuro
        boolean estaPardeando = bird.tiempoFlash > 0f && ((int)(bird.tiempoFlash * 12f) % 2) == 0;

        float colorCuerpoR = estaPardeando ? 1f : bird.cr * factorBrillo;
        float colorCuerpoG = estaPardeando ? 1f : bird.cg * factorBrillo;
        float colorCuerpoB = estaPardeando ? 1f : bird.cb * factorBrillo;
        float colorPicoR   = estaPardeando ? 1f : 1f    * factorBrillo;
        float colorPicoG   = estaPardeando ? 1f : 0.50f * factorBrillo;
        float colorPicoB   = estaPardeando ? 1f : 0.08f * factorBrillo;
        float brilloOjo    = estaPardeando ? 1f : factorBrillo;

        float anguloInclinacion = bird.alive ? clamp(velocidadY * 0.28f, -0.55f, 0.45f) : -0.60f;
        float anguloAlaActual   = bird.anguloAla;
        float[] posicionRotada;

        dibujarRectangulo(x, y, BIRD_ANCHO, BIRD_ALTO, anguloInclinacion,
                          colorCuerpoR, colorCuerpoG, colorCuerpoB);                        // cuerpo

        posicionRotada = rotarOffset(-0.057f, 0f, anguloInclinacion);                       // cola
        dibujarTriangulo(x+posicionRotada[0], y+posicionRotada[1], 0.045f, 0.030f,
                         anguloInclinacion+(float)Math.PI,
                         colorCuerpoR*.85f, colorCuerpoG*.85f, colorCuerpoB*.85f);

        float desplazamientoAlaY = bird.alive ? (float)Math.sin(anguloAlaActual)*0.018f : 0f; // ala
        float rotacionAla        = bird.alive ? anguloInclinacion+(float)Math.sin(anguloAlaActual)*0.35f : anguloInclinacion;
        posicionRotada = rotarOffset(-0.005f, -0.005f+desplazamientoAlaY, anguloInclinacion);
        dibujarRectangulo(x+posicionRotada[0], y+posicionRotada[1], 0.065f, 0.028f,
                          rotacionAla, colorCuerpoR*.78f, colorCuerpoG*.78f, colorCuerpoB*.78f);

        posicionRotada = rotarOffset(0.058f, -0.004f, anguloInclinacion);                   // pico
        dibujarTriangulo(x+posicionRotada[0], y+posicionRotada[1], 0.038f, 0.024f,
                         anguloInclinacion, colorPicoR, colorPicoG, colorPicoB);

        posicionRotada = rotarOffset(0.022f, 0.018f, anguloInclinacion);                    // ojo
        dibujarRectangulo(x+posicionRotada[0], y+posicionRotada[1], 0.028f, 0.028f,
                          anguloInclinacion, brilloOjo, brilloOjo, brilloOjo);
        posicionRotada = rotarOffset(0.028f, 0.016f, anguloInclinacion);                    // pupila
        dibujarRectangulo(x+posicionRotada[0], y+posicionRotada[1], 0.013f, 0.013f,
                          anguloInclinacion, 0.08f, 0.08f, 0.08f);
    }

    // PI/2 en radianes: hace que el triangulo base (punta derecha) apunte hacia arriba.
    private static final float UP = (float)(Math.PI / 2.0);

    // -------------------------------------------------------------------------
    // Render del fondo: cielo degradado, sol, montanas, suelo, nubes.
    // -------------------------------------------------------------------------
    private void dibujarFondo() {
        // Cielo: cuatro franjas de degradado azul de oscuro (abajo) a claro (arriba).
        dibujarRectangulo(0f, -1.0f, 2f, 1.0f, 0.38f, 0.65f, 0.82f);
        dibujarRectangulo(0f, -0.2f, 2f, 1.0f, 0.46f, 0.74f, 0.90f);
        dibujarRectangulo(0f,  0.5f, 2f, 1.0f, 0.54f, 0.82f, 0.97f);
        dibujarRectangulo(0f,  0.9f, 2f, 0.4f, 0.62f, 0.89f, 1.00f);

        // Sol: circulo aproximado con quads solapados.
        dibujarRectangulo( 0.72f, 0.72f, 0.14f, 0.14f, 1.00f, 0.96f, 0.50f);
        dibujarRectangulo( 0.72f, 0.72f, 0.10f, 0.18f, 1.00f, 0.96f, 0.50f);
        dibujarRectangulo( 0.72f, 0.72f, 0.18f, 0.10f, 1.00f, 0.96f, 0.50f);
        // Rayos del sol (triangulos).
        dibujarTriangulo( 0.72f, 0.82f, 0.04f, 0.06f,  UP,          1.00f, 0.96f, 0.50f);
        dibujarTriangulo( 0.72f, 0.62f, 0.04f, 0.06f,  UP+(float)Math.PI, 1.00f, 0.96f, 0.50f);
        dibujarTriangulo( 0.82f, 0.72f, 0.06f, 0.04f,  UP*2,        1.00f, 0.96f, 0.50f);
        dibujarTriangulo( 0.62f, 0.72f, 0.06f, 0.04f,  0f,          1.00f, 0.96f, 0.50f);

        // Montanas lejanas: capa trasera (mas claras, mas ancho).
        dibujarTriangulo(-0.80f, -0.74f, 0.90f, 0.62f, UP, 0.58f, 0.66f, 0.63f);
        dibujarTriangulo(-0.05f, -0.70f, 0.70f, 0.56f, UP, 0.54f, 0.62f, 0.60f);
        dibujarTriangulo( 0.65f, -0.72f, 0.80f, 0.58f, UP, 0.56f, 0.64f, 0.61f);
        dibujarTriangulo(-1.30f, -0.76f, 0.75f, 0.55f, UP, 0.60f, 0.67f, 0.64f);

        // Montanas delanteras: capa frontal (mas oscuras, mas altas).
        dibujarTriangulo(-0.62f, -0.76f, 0.65f, 0.72f, UP, 0.42f, 0.52f, 0.50f);
        dibujarTriangulo( 0.10f, -0.73f, 0.55f, 0.65f, UP, 0.38f, 0.48f, 0.46f);
        dibujarTriangulo( 0.60f, -0.75f, 0.60f, 0.68f, UP, 0.40f, 0.50f, 0.48f);
        dibujarTriangulo(-1.20f, -0.78f, 0.58f, 0.60f, UP, 0.44f, 0.54f, 0.52f);

        // Nevado en los picos (triangulo blanco pequeno sobre cada montana delantera).
        dibujarTriangulo(-0.62f, -0.40f, 0.18f, 0.20f, UP, 0.92f, 0.95f, 0.97f);
        dibujarTriangulo( 0.10f, -0.43f, 0.15f, 0.17f, UP, 0.92f, 0.95f, 0.97f);
        dibujarTriangulo( 0.60f, -0.41f, 0.16f, 0.18f, UP, 0.92f, 0.95f, 0.97f);

        // Suelo: franja verde con linea de horizonte y detalle de hierba.
        dibujarRectangulo(0f, SUELO_Y,                             2f, SUELO_ALTO, 0.24f, 0.62f, 0.18f);
        dibujarRectangulo(0f, SUELO_Y + SUELO_ALTO * 0.5f - 0.005f, 2f, 0.025f,   0.18f, 0.50f, 0.14f);
        dibujarRectangulo(0f, SUELO_Y + SUELO_ALTO * 0.5f - 0.018f, 2f, 0.012f,   0.32f, 0.72f, 0.24f);

        // Nubes: 4 quads solapados por nube (mas voluminosas).
        for (float[] n : nubes) dibujarNube(n[0], n[1], n[2]);
    }

    private void dibujarNube(float x, float y, float escala) {
        // Sombra tenue debajo.
        dibujarRectangulo(x + 0.02f*escala, y - 0.05f*escala, 0.22f*escala, 0.07f*escala, 0.75f, 0.82f, 0.90f);
        // Cuerpo de la nube (4 quads blancos solapados).
        dibujarRectangulo(x,                y,                 0.22f*escala, 0.11f*escala, 0.96f, 0.98f, 1.00f);
        dibujarRectangulo(x - 0.09f*escala, y - 0.02f,         0.15f*escala, 0.10f*escala, 0.96f, 0.98f, 1.00f);
        dibujarRectangulo(x + 0.09f*escala, y - 0.02f,         0.15f*escala, 0.10f*escala, 0.96f, 0.98f, 1.00f);
        dibujarRectangulo(x - 0.04f*escala, y + 0.04f,         0.12f*escala, 0.09f*escala, 0.99f, 1.00f, 1.00f);
    }

    // -------------------------------------------------------------------------
    // Display de 7 segmentos para mostrar numeros reales.
    // -------------------------------------------------------------------------
    private void dibujarDigito(int digito, float x, float y, float tamano,
                               float rojo, float verde, float azul) {
        if (digito < 0 || digito > 9) return;
        float altoDigito  = tamano, anchoDigito = tamano * 0.55f, grosorSegmento = tamano * 0.13f;
        float mitadAlto   = altoDigito / 2f, mitadAncho = anchoDigito / 2f;
        float largoVertical   = mitadAlto  - grosorSegmento;
        float largoHorizontal = anchoDigito - grosorSegmento;
        // Segmentos: [arriba, derArriba, derAbajo, abajo, izqAbajo, izqArriba, medio]
        boolean[][] tablaSegmentos = {
            {true,  true,  true,  true,  true,  true,  false}, // 0
            {false, true,  true,  false, false, false, false},  // 1
            {true,  true,  false, true,  true,  false, true},   // 2
            {true,  true,  true,  true,  false, false, true},   // 3
            {false, true,  true,  false, false, true,  true},   // 4
            {true,  false, true,  true,  false, true,  true},   // 5
            {true,  false, true,  true,  true,  true,  true},   // 6
            {true,  true,  true,  false, false, false, false},  // 7
            {true,  true,  true,  true,  true,  true,  true},   // 8
            {true,  true,  true,  true,  false, true,  true},   // 9
        };
        boolean[] segmentosActivos = tablaSegmentos[digito];
        if (segmentosActivos[0]) dibujarRectangulo(x,                    y + mitadAlto  - grosorSegmento/2f, largoHorizontal, grosorSegmento, rojo, verde, azul);
        if (segmentosActivos[1]) dibujarRectangulo(x + mitadAncho - grosorSegmento/2f, y + altoDigito / 4f, grosorSegmento, largoVertical,   rojo, verde, azul);
        if (segmentosActivos[2]) dibujarRectangulo(x + mitadAncho - grosorSegmento/2f, y - altoDigito / 4f, grosorSegmento, largoVertical,   rojo, verde, azul);
        if (segmentosActivos[3]) dibujarRectangulo(x,                    y - mitadAlto  + grosorSegmento/2f, largoHorizontal, grosorSegmento, rojo, verde, azul);
        if (segmentosActivos[4]) dibujarRectangulo(x - mitadAncho + grosorSegmento/2f, y - altoDigito / 4f, grosorSegmento, largoVertical,   rojo, verde, azul);
        if (segmentosActivos[5]) dibujarRectangulo(x - mitadAncho + grosorSegmento/2f, y + altoDigito / 4f, grosorSegmento, largoVertical,   rojo, verde, azul);
        if (segmentosActivos[6]) dibujarRectangulo(x,                    y,                                  largoHorizontal, grosorSegmento, rojo, verde, azul);
    }

    private void dibujarNumero(int numero, float centroPosX, float centroPosY,
                               float tamano, float rojo, float verde, float azul) {
        String textoNumero          = String.valueOf(Math.max(0, numero));
        float  espaciadoEntreDigitos = tamano * 0.72f;
        float  anchoTotalNumero     = (textoNumero.length() - 1) * espaciadoEntreDigitos;
        for (int i = 0; i < textoNumero.length(); i++)
            dibujarDigito(textoNumero.charAt(i) - '0',
                          centroPosX - anchoTotalNumero / 2f + i * espaciadoEntreDigitos,
                          centroPosY, tamano, rojo, verde, azul);
    }

    // -------------------------------------------------------------------------
    // Countdown 3-2-1 antes de iniciar.
    // -------------------------------------------------------------------------
    private void dibujarCuentaRegresiva() {
        int   numeroActual       = Math.max(1, (int) Math.ceil(tiempoCuentaRegresiva));
        float fraccionEnSegundo  = tiempoCuentaRegresiva - (float)(numeroActual - 1); // 0..1 dentro del segundo
        float tamanoActual       = 0.15f + fraccionEnSegundo * 0.38f;  // pulso: grande al inicio, pequeño al final
        float rojo, verde, azul;
        if      (numeroActual == 3) { rojo = 0.20f; verde = 0.88f; azul = 0.35f; } // verde
        else if (numeroActual == 2) { rojo = 1.00f; verde = 0.82f; azul = 0.10f; } // amarillo
        else                        { rojo = 1.00f; verde = 0.22f; azul = 0.15f; } // rojo
        // Sombra del digito.
        dibujarRectangulo(0.010f, -0.010f, tamanoActual * 0.65f, tamanoActual * 1.05f, 0.04f, 0.04f, 0.05f);
        dibujarNumero(numeroActual, 0f, 0f, tamanoActual, rojo, verde, azul);
    }

    // -------------------------------------------------------------------------
    // HUD: barra superior oscura + bloques de puntaje + barra de nivel.
    // -------------------------------------------------------------------------
    private void dibujarHUD() {
        if (!started) return;

        // Fondo del HUD con degradado (dos capas).
        dibujarRectangulo(0f, 0.935f, 2f, 0.130f, 0.08f, 0.10f, 0.13f);
        dibujarRectangulo(0f, 0.865f, 2f, 0.010f, 0.18f, 0.22f, 0.28f);  // linea separadora

        // Icono pajaro J1 (pequeno, a la izquierda de sus bloques).
        dibujarPajaro(-0.88f, 0.938f, 0f, birds[0]);

        // Bloques de puntaje J1 — maximo 10 visibles para no solapar los numeros.
        int bloquesVisiblesJ1 = Math.min(birds[0].score, 10);
        int bloquesVisiblesJ2 = Math.min(birds[1].score, 10);

        for (int i = 0; i < bloquesVisiblesJ1; i++) {
            float posXBloque = -0.76f + i * 0.033f;
            dibujarRectangulo(posXBloque, 0.938f, 0.028f, 0.040f, birds[0].cr*0.6f, birds[0].cg*0.6f, birds[0].cb*0.6f);
            dibujarRectangulo(posXBloque, 0.940f, 0.022f, 0.032f, birds[0].cr, birds[0].cg, birds[0].cb);
        }

        // Icono pajaro J2 (pequeno, a la derecha de sus bloques).
        dibujarPajaro( 0.88f, 0.938f, 0f, birds[1]);

        // Bloques de puntaje J2.
        for (int i = 0; i < bloquesVisiblesJ2; i++) {
            float posXBloque = 0.76f - i * 0.033f;
            dibujarRectangulo(posXBloque, 0.938f, 0.028f, 0.040f, birds[1].cr*0.6f, birds[1].cg*0.6f, birds[1].cb*0.6f);
            dibujarRectangulo(posXBloque, 0.940f, 0.022f, 0.032f, birds[1].cr, birds[1].cg, birds[1].cb);
        }

        // Numeros de puntaje en el HUD.
        dibujarNumero(birds[0].score, -0.28f, 0.935f, 0.078f,
                      birds[0].cr, birds[0].cg, birds[0].cb);
        dibujarNumero(birds[1].score,  0.28f, 0.935f, 0.078f,
                      birds[1].cr, birds[1].cg, birds[1].cb);

        // Indicador de nivel en el centro del HUD con su color de nivel.
        float[] colorIndicadorNivel = nivelColor(nivelActual);
        dibujarNumero(nivelActual, 0f, 0.940f, 0.058f,
                      colorIndicadorNivel[0], colorIndicadorNivel[1], colorIndicadorNivel[2]);

        // --- Barra de progreso al siguiente nivel ---
        float anchoBarraNivel = 0.50f, posYBarra = 0.878f, altoBarra = 0.016f;
        // Fondo gris con borde.
        dibujarRectangulo(0f, posYBarra, anchoBarraNivel + 0.01f, altoBarra + 0.008f, 0.18f, 0.20f, 0.24f);
        dibujarRectangulo(0f, posYBarra, anchoBarraNivel,          altoBarra,           0.22f, 0.24f, 0.28f);

        int puntosEnNivelActual = birds[0].score > birds[1].score ? birds[0].score : birds[1].score;
        puntosEnNivelActual = puntosEnNivelActual % PUNTOS_POR_NIVEL;
        float progresoHaciaProximoNivel = (nivelActual >= NIVEL_MAX) ? 1f
                                        : puntosEnNivelActual / (float) PUNTOS_POR_NIVEL;
        float[] colorNivelActual = nivelColor(nivelActual);
        if (progresoHaciaProximoNivel > 0f)
            dibujarRectangulo(-anchoBarraNivel*0.5f + anchoBarraNivel*progresoHaciaProximoNivel*0.5f,
                               posYBarra, anchoBarraNivel*progresoHaciaProximoNivel, altoBarra,
                               colorNivelActual[0], colorNivelActual[1], colorNivelActual[2]);

        // Icono de nivel: pequeno rayo/triangulo coloreado.
        dibujarTriangulo(anchoBarraNivel*0.5f + 0.022f, posYBarra, 0.018f, 0.028f, 0f,
                         colorNivelActual[0], colorNivelActual[1], colorNivelActual[2]);
        // Marcas de division del nivel (cada 20% de la barra).
        for (int indiceDivision = 1; indiceDivision < PUNTOS_POR_NIVEL; indiceDivision++) {
            float posXMarca = -anchoBarraNivel*0.5f + anchoBarraNivel * (indiceDivision / (float)PUNTOS_POR_NIVEL);
            dibujarRectangulo(posXMarca, posYBarra, 0.004f, altoBarra, 0.10f, 0.11f, 0.14f);
        }
    }

    // Verde → amarillo → naranja → rojo → purpura, uno por nivel.
    private static final float[][] NIVEL_COLORES = {
        {0.20f, 0.85f, 0.35f},  // nivel 1 - verde
        {0.75f, 0.90f, 0.15f},  // nivel 2 - amarillo
        {1.00f, 0.65f, 0.10f},  // nivel 3 - naranja
        {1.00f, 0.25f, 0.15f},  // nivel 4 - rojo
        {0.80f, 0.20f, 0.90f},  // nivel 5 - purpura
    };

    private float[] nivelColor(int nivel) {
        return NIVEL_COLORES[Math.min(nivel, NIVEL_MAX) - 1];
    }

    // -------------------------------------------------------------------------
    // Pantalla de inicio.
    // -------------------------------------------------------------------------
    private void dibujarPantallaInicio(float tiempo) {
        // Sombra del panel (desplazada).
        dibujarRectangulo(0.015f, -0.015f, 1.38f, 0.68f, 0.03f, 0.04f, 0.05f);
        // Panel principal.
        dibujarRectangulo(0f, 0.0f, 1.38f, 0.68f, 0.10f, 0.13f, 0.17f);
        // Franja decorativa superior del panel.
        dibujarRectangulo(0f, 0.30f, 1.38f, 0.08f, 0.14f, 0.18f, 0.24f);

        // Borde celeste (4 lados).
        dibujarRectangulo(0f,      0.340f, 1.38f, 0.012f, 0.40f, 0.70f, 0.95f);
        dibujarRectangulo(0f,     -0.340f, 1.38f, 0.012f, 0.40f, 0.70f, 0.95f);
        dibujarRectangulo(-0.690f, 0.0f,   0.012f, 0.68f, 0.40f, 0.70f, 0.95f);
        dibujarRectangulo( 0.690f, 0.0f,   0.012f, 0.68f, 0.40f, 0.70f, 0.95f);
        // Esquinas del borde (cuadraditos).
        dibujarRectangulo(-0.690f, 0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        dibujarRectangulo( 0.690f, 0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        dibujarRectangulo(-0.690f,-0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        dibujarRectangulo( 0.690f,-0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);

        // Titulo: "2 PLAYERS" como bloques de color alternados.
        float[] colorTitulo = {0.40f, 0.70f, 0.95f};
        for (int i = 0; i < 9; i++) {
            float  posXBloque    = -0.20f + i * 0.048f;
            float[] colorBloque  = (i % 2 == 0) ? colorTitulo : new float[]{1f,1f,1f};
            dibujarRectangulo(posXBloque, 0.295f, 0.040f, 0.035f, colorBloque[0], colorBloque[1], colorBloque[2]);
        }

        // Pajaros animados en la pantalla de inicio (flotan).
        float flotacionJ1 = (float)Math.sin(tiempo * 2.2f) * 0.025f;
        float flotacionJ2 = (float)Math.sin(tiempo * 2.2f + 1.2f) * 0.025f;
        dibujarPajaro(-0.32f, 0.06f + flotacionJ1,  flotacionJ1 * 12f, birds[0]);
        dibujarPajaro( 0.32f, 0.06f + flotacionJ2, -flotacionJ2 * 12f, birds[1]);

        // Separador vertical entre los dos pajaros.
        dibujarRectangulo(0f, 0.08f, 0.006f, 0.28f, 0.25f, 0.30f, 0.38f);

        // Tecla J1 (barra amarilla debajo del pajaro).
        dibujarRectangulo(-0.32f, -0.14f, 0.22f, 0.040f, birds[0].cr*0.7f, birds[0].cg*0.7f, birds[0].cb*0.7f);
        dibujarRectangulo(-0.32f, -0.14f, 0.18f, 0.030f, birds[0].cr, birds[0].cg, birds[0].cb);

        // Tecla J2 (barra celeste debajo del pajaro).
        dibujarRectangulo( 0.32f, -0.14f, 0.22f, 0.040f, birds[1].cr*0.7f, birds[1].cg*0.7f, birds[1].cb*0.7f);
        dibujarRectangulo( 0.32f, -0.14f, 0.18f, 0.030f, birds[1].cr, birds[1].cg, birds[1].cb);

        // Boton parpadeante central "PLAY" (triangulo + barras).
        boolean botonVisible = ((int)(tiempo * 1.6f) % 2) == 0;
        if (botonVisible) {
            float factorPulso = 0.85f + (float)Math.sin(tiempo * 6f) * 0.15f;
            dibujarTriangulo(0f, -0.255f, 0.07f * factorPulso, 0.07f * factorPulso, 0f, 0.95f, 0.95f, 0.60f);
            dibujarRectangulo(-0.10f, -0.255f, 0.055f, 0.018f, 0.95f, 0.95f, 0.60f);
            dibujarRectangulo( 0.10f, -0.255f, 0.055f, 0.018f, 0.95f, 0.95f, 0.60f);
        }
    }

    // -------------------------------------------------------------------------
    // Pantalla de game over.
    // -------------------------------------------------------------------------
    private void dibujarPantallaGameOver() {
        float tiempoActual = (float) GLFW.glfwGetTime();

        // Sombra del panel.
        dibujarRectangulo(0.015f, -0.015f, 1.50f, 0.82f, 0.02f, 0.03f, 0.04f);
        // Panel principal.
        dibujarRectangulo(0f, 0.02f, 1.50f, 0.82f, 0.08f, 0.09f, 0.12f);
        // Franja superior coloreada.
        dibujarRectangulo(0f, 0.37f, 1.50f, 0.08f, 0.55f, 0.12f, 0.12f);

        // Borde rojo (4 lados + esquinas).
        dibujarRectangulo(0f,      0.410f, 1.50f, 0.014f, 0.85f, 0.18f, 0.18f);
        dibujarRectangulo(0f,     -0.390f, 1.50f, 0.014f, 0.85f, 0.18f, 0.18f);
        dibujarRectangulo(-0.750f, 0.010f, 0.014f, 0.82f, 0.85f, 0.18f, 0.18f);
        dibujarRectangulo( 0.750f, 0.010f, 0.014f, 0.82f, 0.85f, 0.18f, 0.18f);

        // Decoracion "GAME OVER": bloques rojos y blancos alternados.
        for (int i = 0; i < 9; i++) {
            float   posXBloque   = -0.20f + i * 0.050f;
            float[] colorBloque  = (i % 2 == 0) ? new float[]{0.90f,0.18f,0.18f} : new float[]{1f,1f,1f};
            dibujarRectangulo(posXBloque, 0.368f, 0.042f, 0.036f, colorBloque[0], colorBloque[1], colorBloque[2]);
        }

        // Resultados: pajaro + barra de score por jugador.
        int   puntajeJ1      = birds[0].score, puntajeJ2 = birds[1].score;
        int   puntajeMaximo  = Math.max(Math.max(puntajeJ1, puntajeJ2), 1);
        float anchoMaximoBarra = 0.52f, altoBarra = 0.060f;

        // J1.
        dibujarPajaro(-0.60f, 0.185f, 0f, birds[0]);
        dibujarRectangulo(-anchoMaximoBarra*0.5f*(puntajeJ1/(float)puntajeMaximo) - 0.05f, 0.185f,
             anchoMaximoBarra*(puntajeJ1/(float)puntajeMaximo), altoBarra,
             birds[0].cr*0.55f, birds[0].cg*0.55f, birds[0].cb*0.55f);
        dibujarRectangulo(-anchoMaximoBarra*0.5f*(puntajeJ1/(float)puntajeMaximo) - 0.05f, 0.188f,
             anchoMaximoBarra*(puntajeJ1/(float)puntajeMaximo), altoBarra*0.60f,
             birds[0].cr, birds[0].cg, birds[0].cb);

        // J2.
        dibujarPajaro(-0.60f, 0.060f, 0f, birds[1]);
        dibujarRectangulo(-anchoMaximoBarra*0.5f*(puntajeJ2/(float)puntajeMaximo) - 0.05f, 0.060f,
             anchoMaximoBarra*(puntajeJ2/(float)puntajeMaximo), altoBarra,
             birds[1].cr*0.55f, birds[1].cg*0.55f, birds[1].cb*0.55f);
        dibujarRectangulo(-anchoMaximoBarra*0.5f*(puntajeJ2/(float)puntajeMaximo) - 0.05f, 0.063f,
             anchoMaximoBarra*(puntajeJ2/(float)puntajeMaximo), altoBarra*0.60f,
             birds[1].cr, birds[1].cg, birds[1].cb);

        // Numeros de puntaje grandes a la derecha de las barras.
        dibujarNumero(puntajeJ1, 0.46f, 0.185f, 0.12f, birds[0].cr, birds[0].cg, birds[0].cb);
        dibujarNumero(puntajeJ2, 0.46f, 0.060f, 0.12f, birds[1].cr, birds[1].cg, birds[1].cb);

        // Gran X roja sobre el separador entre los dos jugadores.
        float anguloRotacionX = (float)(Math.PI * 0.25);
        dibujarRectangulo(0.05f, 0.122f, 0.62f, 0.090f,  anguloRotacionX, 0.88f, 0.12f, 0.12f);
        dibujarRectangulo(0.05f, 0.122f, 0.62f, 0.090f, -anguloRotacionX, 0.88f, 0.12f, 0.12f);

        // Corona/triangulo de ganador.
        if (puntajeJ1 > puntajeJ2) {
            dibujarTriangulo(-0.57f, 0.245f, 0.055f, 0.055f, UP, 1.00f, 0.90f, 0.20f);
            dibujarTriangulo(-0.51f, 0.245f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
            dibujarTriangulo(-0.63f, 0.245f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
        } else if (puntajeJ2 > puntajeJ1) {
            dibujarTriangulo(-0.57f, 0.120f, 0.055f, 0.055f, UP, 1.00f, 0.90f, 0.20f);
            dibujarTriangulo(-0.51f, 0.120f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
            dibujarTriangulo(-0.63f, 0.120f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
        } else {
            // Empate: triangulo para cada uno.
            dibujarTriangulo(-0.57f, 0.245f, 0.040f, 0.040f, UP, 0.90f, 0.90f, 0.90f);
            dibujarTriangulo(-0.57f, 0.120f, 0.040f, 0.040f, UP, 0.90f, 0.90f, 0.90f);
        }

        // Nivel alcanzado: barra coloreada.
        float[] colorNivelAlcanzado = nivelColor(nivelActual);
        dibujarRectangulo(0.05f, -0.100f, 0.80f, 0.040f, 0.15f, 0.17f, 0.20f);
        dibujarRectangulo(0.05f - 0.40f + 0.40f*(nivelActual/(float)NIVEL_MAX),
             -0.100f, 0.80f*(nivelActual/(float)NIVEL_MAX), 0.030f,
             colorNivelAlcanzado[0], colorNivelAlcanzado[1], colorNivelAlcanzado[2]);
        // Marcas de nivel.
        for (int i = 1; i <= NIVEL_MAX; i++) {
            float posXMarcaNivel = 0.05f - 0.40f + 0.80f*(i/(float)NIVEL_MAX);
            dibujarRectangulo(posXMarcaNivel, -0.100f, 0.006f, 0.040f, 0.08f, 0.09f, 0.12f);
        }

        // Boton de reinicio parpadeante.
        boolean botonVisible = ((int)(tiempoActual * 1.4f) % 2) == 0;
        if (botonVisible) {
            dibujarRectangulo(0.05f, -0.270f, 0.75f, 0.050f, 0.16f, 0.18f, 0.22f);
            dibujarRectangulo(0.05f, -0.270f, 0.70f, 0.036f, 0.28f, 0.32f, 0.40f);
            dibujarTriangulo(0.05f, -0.270f, 0.030f, 0.030f, 0f, 0.80f, 0.85f, 0.95f);
        }
    }

    // -------------------------------------------------------------------------
    // Render de tuberias: cuerpo + brillo + sombra + capuchon por par.
    // -------------------------------------------------------------------------
    private void dibujarTuberias() {
        for (Tuberia tuberia : tuberias) {
            float bordeSuperiorHueco = tuberia.centroHuecoY + tuberia.alturaHueco * 0.5f;
            float bordeInferiorHueco = tuberia.centroHuecoY - tuberia.alturaHueco * 0.5f;
            float altoTuberiaArriba  = 1f - bordeSuperiorHueco;  // desde el borde del hueco hasta el techo
            float altoTuberiaAbajo   = bordeInferiorHueco + 1f;  // desde el suelo hasta el borde del hueco
            if (altoTuberiaArriba > 0f) {
                // Sombra lateral derecha.
                dibujarRectangulo(tuberia.x + TUBERIA_ANCHO*0.5f - 0.012f,
                                  bordeSuperiorHueco + altoTuberiaArriba*0.5f,
                                  0.024f, altoTuberiaArriba, 0.08f, 0.40f, 0.10f);
                // Cuerpo.
                dibujarRectangulo(tuberia.x, bordeSuperiorHueco + altoTuberiaArriba*0.5f,
                                  TUBERIA_ANCHO, altoTuberiaArriba, 0.15f, 0.60f, 0.18f);
                // Brillo lateral izquierdo.
                dibujarRectangulo(tuberia.x - TUBERIA_ANCHO*0.5f + 0.010f,
                                  bordeSuperiorHueco + altoTuberiaArriba*0.5f,
                                  0.018f, altoTuberiaArriba, 0.22f, 0.72f, 0.26f);
                // Capuchon (boca de la tuberia).
                dibujarRectangulo(tuberia.x, bordeSuperiorHueco - 0.018f, TUBERIA_ANCHO + 0.035f, 0.036f, 0.10f, 0.45f, 0.13f);
                dibujarRectangulo(tuberia.x, bordeSuperiorHueco - 0.018f, TUBERIA_ANCHO + 0.020f, 0.026f, 0.18f, 0.58f, 0.20f);
            }
            if (altoTuberiaAbajo > 0f) {
                dibujarRectangulo(tuberia.x + TUBERIA_ANCHO*0.5f - 0.012f,
                                  -1f + altoTuberiaAbajo*0.5f,
                                  0.024f, altoTuberiaAbajo, 0.08f, 0.40f, 0.10f);
                dibujarRectangulo(tuberia.x, -1f + altoTuberiaAbajo*0.5f,
                                  TUBERIA_ANCHO, altoTuberiaAbajo, 0.15f, 0.60f, 0.18f);
                dibujarRectangulo(tuberia.x - TUBERIA_ANCHO*0.5f + 0.010f,
                                  -1f + altoTuberiaAbajo*0.5f,
                                  0.018f, altoTuberiaAbajo, 0.22f, 0.72f, 0.26f);
                dibujarRectangulo(tuberia.x, bordeInferiorHueco + 0.018f, TUBERIA_ANCHO + 0.035f, 0.036f, 0.10f, 0.45f, 0.13f);
                dibujarRectangulo(tuberia.x, bordeInferiorHueco + 0.018f, TUBERIA_ANCHO + 0.020f, 0.026f, 0.18f, 0.58f, 0.20f);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Render principal.
    // -------------------------------------------------------------------------
    private void dibujarEscena(float tiempo) {
        GL11.glClearColor(0.52f, 0.80f, 0.95f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programaShader);

        // 1. Fondo (cielo, montanas, suelo, nubes).
        dibujarFondo();

        // 2. Tuberias con borde lateral oscuro y capuchon en la boca.
        dibujarTuberias();

        // 3. Pajaros.
        for (Bird b : birds) {
            if (b.y < -1.4f || b.y > 1.3f) continue;
            dibujarPajaro(BIRD_X, b.y, b.velocidadY, b);
        }

        // 4. Franja lateral de jugador muerto.
        for (int i = 0; i < birds.length; i++) {
            if (!birds[i].alive) {
                float px = (i == 0) ? -0.97f : 0.97f;
                dibujarRectangulo(px, 0f, 0.04f, 2f,
                     birds[i].cr*0.4f, birds[i].cg*0.4f, birds[i].cb*0.4f);
            }
        }

        // 5. HUD (barra superior, bloques de puntaje, barra de nivel).
        dibujarHUD();

        // 6. Pantalla de inicio, conteo, o game over.
        if (!started && !enCuentaRegresiva) dibujarPantallaInicio(tiempo);
        else if (enCuentaRegresiva)         dibujarCuentaRegresiva();
        else if (gameOver)         dibujarPantallaGameOver();
    }

    // -------------------------------------------------------------------------
    // Logica del juego.
    // -------------------------------------------------------------------------
    private void calcularNivel() {
        int puntajeMaximoActual = 0;
        for (Bird pajaro : birds) puntajeMaximoActual = Math.max(puntajeMaximoActual, pajaro.score);
        int nivelNuevo = Math.min(puntajeMaximoActual / PUNTOS_POR_NIVEL + 1, NIVEL_MAX);
        if (nivelNuevo != nivelActual) {
            nivelActual         = nivelNuevo;
            velocidadTuberias   = VEL_BASE   + (nivelActual - 1) * VEL_PASO;
            tiempoEntreTuberias = SPAWN_BASE  - (nivelActual - 1) * SPAWN_PASO;
            alturaHueco         = GAP_ALTO_BASE
                                  - (nivelActual - 1) * (GAP_ALTO_BASE - GAP_ALTO_MIN)
                                  / (float)(NIVEL_MAX - 1);
        }
    }

    private void resetGame() {
        birds[0].reset( 0.12f);
        birds[1].reset(-0.12f);
        timerGeneracion          = 0f;
        started             = false;
        gameOver            = false;
        enCuentaRegresiva            = false;
        tiempoCuentaRegresiva        = 0f;
        nivelActual         = 1;
        velocidadTuberias         = VEL_BASE;
        tiempoEntreTuberias = SPAWN_BASE;
        alturaHueco             = GAP_ALTO_BASE;
        tuberias.clear();
        actualizarTitulo();
    }

    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS)
            GLFW.glfwSetWindowShouldClose(window, true);

        for (Bird pajaro : birds) {
            boolean teclaJumpPresionada = false;
            for (int codigoDeTecla : pajaro.jumpKeys)
                if (GLFW.glfwGetKey(window, codigoDeTecla) == GLFW.GLFW_PRESS) {
                    teclaJumpPresionada = true; break;
                }

            if (teclaJumpPresionada && !pajaro.saltoPrevio) {
                if (gameOver) { resetGame(); break; }
                if (!started && !enCuentaRegresiva) {
                    enCuentaRegresiva     = true;
                    tiempoCuentaRegresiva = 3.0f;
                } else if (started && pajaro.alive) {
                    pajaro.velocidadY = IMPULSO_SALTO;
                }
            }
            pajaro.saltoPrevio = teclaJumpPresionada;
        }

        boolean teclaReinicio = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R)     == GLFW.GLFW_PRESS
                             || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        if (teclaReinicio && !teclaReinicioPulsada && gameOver) resetGame();
        teclaReinicioPulsada = teclaReinicio;
    }

    private void actualizar(float deltaTiempo) {
        actualizarNubes(deltaTiempo);

        // Countdown: avanza el timer y arranca el juego al llegar a 0.
        if (enCuentaRegresiva) {
            tiempoCuentaRegresiva -= deltaTiempo;
            if (tiempoCuentaRegresiva <= 0f) { enCuentaRegresiva = false; started = true; }
            return;
        }

        if (!started || gameOver) return;

        actualizarPajaros(deltaTiempo);

        if (ambosEliminados()) { gameOver = true; actualizarTitulo(); return; }

        actualizarTuberias(deltaTiempo);

        if (ambosEliminados()) { gameOver = true; actualizarTitulo(); }
    }

    // Las nubes se mueven siempre, incluso durante el countdown y game over.
    private void actualizarNubes(float deltaTiempo) {
        for (float[] nube : nubes) {
            nube[0] -= CLOUD_SPEED * deltaTiempo;
            if (nube[0] < -1.5f) nube[0] = 1.5f;
        }
    }

    // Aplica gravedad, alas, techo/suelo y flash de muerte para cada pajaro.
    private void actualizarPajaros(float deltaTiempo) {
        for (Bird pajaro : birds) {
            if (pajaro.tiempoFlash > 0f) pajaro.tiempoFlash = Math.max(0f, pajaro.tiempoFlash - deltaTiempo);
            if (!pajaro.alive) {
                // Pajaro muerto: sigue cayendo con gravedad (sin terminal velocity).
                pajaro.velocidadY += GRAVEDAD * deltaTiempo;
                pajaro.y          += pajaro.velocidadY * deltaTiempo;
                continue;
            }
            pajaro.anguloAla  += deltaTiempo * (Math.abs(pajaro.velocidadY) * 2.5f + 5f);
            pajaro.velocidadY += GRAVEDAD * deltaTiempo;
            if (pajaro.velocidadY < VELOCIDAD_MAX_CAIDA) pajaro.velocidadY = VELOCIDAD_MAX_CAIDA;
            pajaro.y += pajaro.velocidadY * deltaTiempo;

            if (pajaro.y - BIRD_ALTO * 0.5f <= SUELO_Y + SUELO_ALTO * 0.5f) pajaro.matar();
            if (pajaro.y + BIRD_ALTO * 0.5f >= 1f)                           pajaro.matar();
        }
    }

    // Mueve tuberias, da puntos al pasar BIRD_X y detecta colisiones.
    private void actualizarTuberias(float deltaTiempo) {
        timerGeneracion += deltaTiempo;
        if (timerGeneracion >= tiempoEntreTuberias) { timerGeneracion = 0f; generarTuberia(); }

        Iterator<Tuberia> iteradorTuberias = tuberias.iterator();
        while (iteradorTuberias.hasNext()) {
            Tuberia tuberia = iteradorTuberias.next();
            tuberia.x -= velocidadTuberias * deltaTiempo;

            // Punto cuando el borde derecho de la tuberia pasa la posicion X del pajaro.
            if (tuberia.x + TUBERIA_ANCHO * 0.5f < BIRD_X && !tuberia.puntoContabilizado) {
                tuberia.puntoContabilizado = true;
                boolean alguienPuntuo = false;
                for (Bird pajaro : birds) if (pajaro.alive) { pajaro.score++; alguienPuntuo = true; }
                if (alguienPuntuo) { calcularNivel(); actualizarTitulo(); }
            }

            // AABB: colision solo si hay superposicion horizontal Y el pajaro esta fuera del hueco.
            for (Bird pajaro : birds)
                if (pajaro.alive && colisionaConTuberia(tuberia, pajaro.y)) pajaro.matar();

            if (tuberia.x + TUBERIA_ANCHO * 0.5f < -1.3f) iteradorTuberias.remove();
        }
    }

    private void generarTuberia() {
        float centroPosicionHueco = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, centroPosicionHueco, alturaHueco));
    }

    private boolean colisionaConTuberia(Tuberia tuberia, float posYPajaro) {
        float pajaroBordeIzquierdo = BIRD_X  - BIRD_ANCHO*0.5f;
        float pajaroBordeDerecho   = BIRD_X  + BIRD_ANCHO*0.5f;
        float pajaroBordeInferior  = posYPajaro - BIRD_ALTO*0.5f;
        float pajaroBordeSuperior  = posYPajaro + BIRD_ALTO*0.5f;
        float tuberiaIzquierda     = tuberia.x  - TUBERIA_ANCHO*0.5f;
        float tuberiaDerecha       = tuberia.x  + TUBERIA_ANCHO*0.5f;
        // Primero verificar superposicion horizontal (eje X).
        if (!(pajaroBordeDerecho > tuberiaIzquierda && pajaroBordeIzquierdo < tuberiaDerecha)) return false;
        // Luego verificar si el pajaro esta fuera del hueco (eje Y).
        return pajaroBordeSuperior > tuberia.centroHuecoY + tuberia.alturaHueco*0.5f
            || pajaroBordeInferior < tuberia.centroHuecoY - tuberia.alturaHueco*0.5f;
    }

    private void actualizarTitulo() {
        String estadoJugador1     = "P1: " + birds[0].score + (birds[0].alive ? "" : " [X]");
        String estadoJugador2     = "P2: " + birds[1].score + (birds[1].alive ? "" : " [X]");
        String velocidadFormateada = String.format("%.2f", velocidadTuberias);
        String textoNivel          = "Nivel " + nivelActual + "/" + NIVEL_MAX + " | Val: " + velocidadFormateada;
        String textoPrincipal      = "Flappy Bird OpenGL | " + estadoJugador1 + "  |  " + estadoJugador2 + "  ||  " + textoNivel;
        if (!started)      GLFW.glfwSetWindowTitle(window, textoPrincipal + "  |  SPACE / W para iniciar");
        else if (gameOver) GLFW.glfwSetWindowTitle(window, textoPrincipal + "  |  GAME OVER - SPACE/ENTER para reiniciar");
        else               GLFW.glfwSetWindowTitle(window, textoPrincipal);
    }

    private boolean ambosEliminados() { return !birds[0].alive && !birds[1].alive; }

    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    // -------------------------------------------------------------------------
    // Bucle principal.
    // -------------------------------------------------------------------------
    private void bucleDeJuego() {
        float tiempoUltimoFrame = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float tiempoActual = (float) GLFW.glfwGetTime();
            float deltaTiempo = Math.min(tiempoActual - tiempoUltimoFrame, 0.033f);
            tiempoUltimoFrame = tiempoActual;
            procesarInput();
            actualizar(deltaTiempo);
            dibujarEscena(tiempoActual);
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void liberarRecursos() {
        GL30.glDeleteVertexArrays(vaoQuad); GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoTri);  GL15.glDeleteBuffers(vboTri);
        GL20.glDeleteProgram(programaShader);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) { 
        new AppFlappyBird().run(); 
    }
}

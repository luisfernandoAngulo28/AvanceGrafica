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
    private static final float GAP_ALTO       = 0.48f;
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
    // Bird: estado completo de un jugador.
    // -------------------------------------------------------------------------
    private static class Bird {
        float y, velY;
        int   score;
        boolean alive, prevJump;
        float wingAngle;
        final int[]   jumpKeys;
        final float   cr, cg, cb;
        final String  nombre;

        Bird(float startY, float cr, float cg, float cb, String nombre, int... jumpKeys) {
            this.y = startY; this.cr = cr; this.cg = cg; this.cb = cb;
            this.nombre = nombre; this.jumpKeys = jumpKeys;
            reset(startY);
        }
        void reset(float startY) {
            y = startY; velY = 0f; score = 0;
            alive = true; prevJump = false; wingAngle = 0f;
        }
    }

    private static class Tuberia {
        float x, gapCentroY;
        boolean puntuada;
        Tuberia(float x, float gap) { this.x = x; this.gapCentroY = gap; }
    }

    // -------------------------------------------------------------------------
    // OpenGL.
    // -------------------------------------------------------------------------
    private long window;
    private int  programa;
    private int  vaoQuad, vboQuad, vaoTri, vboTri;
    private int  uOffsetLoc, uScaleLoc, uColorLoc, uRotLoc;

    // -------------------------------------------------------------------------
    // Estado del juego.
    // -------------------------------------------------------------------------
    private final Bird[] birds = {
        new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE),
        new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP)
    };

    private float   timerSpawn;
    private boolean started, gameOver, prevR;
    private int     nivelActual         = 1;
    private float   velTuberias         = VEL_BASE;
    private float   tiempoEntreTuberias = SPAWN_BASE;

    private final List<Tuberia> tuberias = new ArrayList<>();
    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Ciclo principal.
    // -------------------------------------------------------------------------
    public void run() { init(); resetGame(); loop(); cleanup(); }

    private void init() {
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
        crearQuadBase();
        crearTrianguloBase();
    }

    // -------------------------------------------------------------------------
    // Shaders con uRotation para inclinar el pajaro.
    // -------------------------------------------------------------------------
    private void crearShaders() {
        String vs = """
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
        String fs = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() { fragColor = vec4(uColor, 1.0); }
            """;
        int v = compile(vs, GL20.GL_VERTEX_SHADER,   "VS");
        int f = compile(fs, GL20.GL_FRAGMENT_SHADER, "FS");
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, v); GL20.glAttachShader(programa, f);
        GL20.glLinkProgram(programa);
        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Link: " + GL20.glGetProgramInfoLog(programa));
        uOffsetLoc = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLoc  = GL20.glGetUniformLocation(programa, "uScale");
        uColorLoc  = GL20.glGetUniformLocation(programa, "uColor");
        uRotLoc    = GL20.glGetUniformLocation(programa, "uRotation");
        GL20.glDeleteShader(v); GL20.glDeleteShader(f);
    }

    private int compile(String src, int type, String name) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src); GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(name + ": " + GL20.glGetShaderInfoLog(id));
        return id;
    }

    // -------------------------------------------------------------------------
    // Geometria base.
    // -------------------------------------------------------------------------
    private int lastVbo;
    private int buildVao(float[] v) {
        int vao = GL30.glGenVertexArrays(); GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers(); lastVbo = vbo;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); GL30.glBindVertexArray(0);
        return vao;
    }

    private void crearQuadBase() {
        vaoQuad = buildVao(new float[]{
            -0.5f,-0.5f,0f, 0.5f,-0.5f,0f, 0.5f,0.5f,0f,
            -0.5f,-0.5f,0f, 0.5f, 0.5f,0f,-0.5f,0.5f,0f });
        vboQuad = lastVbo;
    }

    private void crearTrianguloBase() {
        // Triangulo apuntando a la derecha; uRotation lo reorienta.
        vaoTri = buildVao(new float[]{ -0.5f,-0.5f,0f, 0.5f,0f,0f, -0.5f,0.5f,0f });
        vboTri = lastVbo;
    }

    // -------------------------------------------------------------------------
    // Primitivas de dibujo.
    // -------------------------------------------------------------------------
    private void rect(float x, float y, float w, float h, float rot,
                      float r, float g, float b) {
        GL30.glBindVertexArray(vaoQuad);
        GL20.glUniform2f(uOffsetLoc, x, y); GL20.glUniform2f(uScaleLoc, w, h);
        GL20.glUniform1f(uRotLoc, rot);     GL20.glUniform3f(uColorLoc, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }
    private void rect(float x, float y, float w, float h,
                      float r, float g, float b) { rect(x, y, w, h, 0f, r, g, b); }

    private void tri(float x, float y, float w, float h, float rot,
                     float r, float g, float b) {
        GL30.glBindVertexArray(vaoTri);
        GL20.glUniform2f(uOffsetLoc, x, y); GL20.glUniform2f(uScaleLoc, w, h);
        GL20.glUniform1f(uRotLoc, rot);     GL20.glUniform3f(uColorLoc, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    // Rota offset local para inclinar partes del pajaro juntas.
    private float[] ro(float lx, float ly, float a) {
        float c=(float)Math.cos(a), s=(float)Math.sin(a);
        return new float[]{ lx*c-ly*s, lx*s+ly*c };
    }

    // -------------------------------------------------------------------------
    // Dibujo del pajaro compuesto.
    // -------------------------------------------------------------------------
    private void dibujarPajaro(float x, float y, float velY, Bird bird) {
        float dim  = bird.alive ? 1.0f : 0.35f;
        float cr   = bird.cr * dim, cg = bird.cg * dim, cb = bird.cb * dim;
        float tilt = bird.alive ? clamp(velY * 0.28f, -0.55f, 0.45f) : -0.60f;
        float wa   = bird.wingAngle;
        float[] p;

        rect(x, y, BIRD_ANCHO, BIRD_ALTO, tilt, cr, cg, cb);                        // cuerpo

        p = ro(-0.057f, 0f, tilt);                                                   // cola
        tri(x+p[0], y+p[1], 0.045f, 0.030f, tilt+(float)Math.PI, cr*.85f,cg*.85f,cb*.85f);

        float woy = bird.alive ? (float)Math.sin(wa)*0.018f : 0f;                   // ala
        float wr  = bird.alive ? tilt+(float)Math.sin(wa)*0.35f : tilt;
        p = ro(-0.005f, -0.005f+woy, tilt);
        rect(x+p[0], y+p[1], 0.065f, 0.028f, wr, cr*.78f, cg*.78f, cb*.78f);

        p = ro(0.058f, -0.004f, tilt);                                               // pico
        tri(x+p[0], y+p[1], 0.038f, 0.024f, tilt, 1f*dim, 0.50f*dim, 0.08f*dim);

        p = ro(0.022f, 0.018f, tilt);                                                // ojo
        rect(x+p[0], y+p[1], 0.028f, 0.028f, tilt, dim, dim, dim);
        p = ro(0.028f, 0.016f, tilt);                                                // pupila
        rect(x+p[0], y+p[1], 0.013f, 0.013f, tilt, 0.08f, 0.08f, 0.08f);
    }

    // PI/2 en radianes: hace que el triangulo base (punta derecha) apunte hacia arriba.
    private static final float UP = (float)(Math.PI / 2.0);

    // -------------------------------------------------------------------------
    // Render del fondo: cielo degradado, sol, montanas, suelo, nubes.
    // -------------------------------------------------------------------------
    private void renderFondo() {
        // Cielo: cuatro franjas de degradado azul de oscuro (abajo) a claro (arriba).
        rect(0f, -1.0f, 2f, 1.0f, 0.38f, 0.65f, 0.82f);
        rect(0f, -0.2f, 2f, 1.0f, 0.46f, 0.74f, 0.90f);
        rect(0f,  0.5f, 2f, 1.0f, 0.54f, 0.82f, 0.97f);
        rect(0f,  0.9f, 2f, 0.4f, 0.62f, 0.89f, 1.00f);

        // Sol: circulo aproximado con quads solapados.
        rect( 0.72f, 0.72f, 0.14f, 0.14f, 1.00f, 0.96f, 0.50f);
        rect( 0.72f, 0.72f, 0.10f, 0.18f, 1.00f, 0.96f, 0.50f);
        rect( 0.72f, 0.72f, 0.18f, 0.10f, 1.00f, 0.96f, 0.50f);
        // Rayos del sol (triangulos).
        tri( 0.72f, 0.82f, 0.04f, 0.06f,  UP,          1.00f, 0.96f, 0.50f);
        tri( 0.72f, 0.62f, 0.04f, 0.06f,  UP+(float)Math.PI, 1.00f, 0.96f, 0.50f);
        tri( 0.82f, 0.72f, 0.06f, 0.04f,  UP*2,        1.00f, 0.96f, 0.50f);
        tri( 0.62f, 0.72f, 0.06f, 0.04f,  0f,          1.00f, 0.96f, 0.50f);

        // Montanas lejanas: capa trasera (mas claras, mas ancho).
        tri(-0.80f, -0.74f, 0.90f, 0.62f, UP, 0.58f, 0.66f, 0.63f);
        tri(-0.05f, -0.70f, 0.70f, 0.56f, UP, 0.54f, 0.62f, 0.60f);
        tri( 0.65f, -0.72f, 0.80f, 0.58f, UP, 0.56f, 0.64f, 0.61f);
        tri(-1.30f, -0.76f, 0.75f, 0.55f, UP, 0.60f, 0.67f, 0.64f);

        // Montanas delanteras: capa frontal (mas oscuras, mas altas).
        tri(-0.62f, -0.76f, 0.65f, 0.72f, UP, 0.42f, 0.52f, 0.50f);
        tri( 0.10f, -0.73f, 0.55f, 0.65f, UP, 0.38f, 0.48f, 0.46f);
        tri( 0.60f, -0.75f, 0.60f, 0.68f, UP, 0.40f, 0.50f, 0.48f);
        tri(-1.20f, -0.78f, 0.58f, 0.60f, UP, 0.44f, 0.54f, 0.52f);

        // Nevado en los picos (triangulo blanco pequeno sobre cada montana delantera).
        tri(-0.62f, -0.40f, 0.18f, 0.20f, UP, 0.92f, 0.95f, 0.97f);
        tri( 0.10f, -0.43f, 0.15f, 0.17f, UP, 0.92f, 0.95f, 0.97f);
        tri( 0.60f, -0.41f, 0.16f, 0.18f, UP, 0.92f, 0.95f, 0.97f);

        // Suelo: franja verde con linea de horizonte y detalle de hierba.
        rect(0f, SUELO_Y,                             2f, SUELO_ALTO, 0.24f, 0.62f, 0.18f);
        rect(0f, SUELO_Y + SUELO_ALTO * 0.5f - 0.005f, 2f, 0.025f,   0.18f, 0.50f, 0.14f);
        rect(0f, SUELO_Y + SUELO_ALTO * 0.5f - 0.018f, 2f, 0.012f,   0.32f, 0.72f, 0.24f);

        // Nubes: 4 quads solapados por nube (mas voluminosas).
        for (float[] n : nubes) dibujarNube(n[0], n[1], n[2]);
    }

    private void dibujarNube(float x, float y, float s) {
        // Sombra tenue debajo.
        rect(x + 0.02f*s, y - 0.05f*s, 0.22f*s, 0.07f*s, 0.75f, 0.82f, 0.90f);
        // Cuerpo de la nube (4 quads blancos solapados).
        rect(x,            y,           0.22f*s, 0.11f*s, 0.96f, 0.98f, 1.00f);
        rect(x - 0.09f*s,  y - 0.02f,  0.15f*s, 0.10f*s, 0.96f, 0.98f, 1.00f);
        rect(x + 0.09f*s,  y - 0.02f,  0.15f*s, 0.10f*s, 0.96f, 0.98f, 1.00f);
        rect(x - 0.04f*s,  y + 0.04f,  0.12f*s, 0.09f*s, 0.99f, 1.00f, 1.00f);
    }

    // -------------------------------------------------------------------------
    // HUD: barra superior oscura + bloques de puntaje + barra de nivel.
    // -------------------------------------------------------------------------
    private void renderHUD() {
        if (!started) return;

        // Fondo del HUD con degradado (dos capas).
        rect(0f, 0.935f, 2f, 0.130f, 0.08f, 0.10f, 0.13f);
        rect(0f, 0.865f, 2f, 0.010f, 0.18f, 0.22f, 0.28f);  // linea separadora

        // Icono pajaro J1 (pequeno, a la izquierda de sus bloques).
        dibujarPajaro(-0.88f, 0.938f, 0f, birds[0]);

        // Bloques de puntaje J1 (desde el icono hacia el centro).
        int max = PUNTOS_POR_NIVEL * NIVEL_MAX;
        int s1  = Math.min(birds[0].score, max);
        int s2  = Math.min(birds[1].score, max);

        for (int i = 0; i < s1; i++) {
            float bx = -0.76f + i * 0.040f;
            // Bloque con borde mas oscuro (dos rects solapados).
            rect(bx, 0.938f, 0.036f, 0.044f, birds[0].cr*0.6f, birds[0].cg*0.6f, birds[0].cb*0.6f);
            rect(bx, 0.940f, 0.030f, 0.036f, birds[0].cr, birds[0].cg, birds[0].cb);
        }

        // Icono pajaro J2 (pequeno, a la derecha de sus bloques).
        dibujarPajaro( 0.88f, 0.938f, 0f, birds[1]);

        // Bloques de puntaje J2 (desde el icono hacia el centro).
        for (int i = 0; i < s2; i++) {
            float bx = 0.76f - i * 0.040f;
            rect(bx, 0.938f, 0.036f, 0.044f, birds[1].cr*0.6f, birds[1].cg*0.6f, birds[1].cb*0.6f);
            rect(bx, 0.940f, 0.030f, 0.036f, birds[1].cr, birds[1].cg, birds[1].cb);
        }

        // --- Barra de progreso al siguiente nivel ---
        float barW = 0.50f, barY = 0.878f, barH = 0.016f;
        // Fondo gris con borde.
        rect(0f, barY, barW + 0.01f, barH + 0.008f, 0.18f, 0.20f, 0.24f);
        rect(0f, barY, barW,         barH,           0.22f, 0.24f, 0.28f);

        int ptsEnNivel = birds[0].score > birds[1].score ? birds[0].score : birds[1].score;
        ptsEnNivel = ptsEnNivel % PUNTOS_POR_NIVEL;
        float progreso = (nivelActual >= NIVEL_MAX) ? 1f : ptsEnNivel / (float) PUNTOS_POR_NIVEL;
        float[] nc = nivelColor(nivelActual);
        if (progreso > 0f)
            rect(-barW*0.5f + barW*progreso*0.5f, barY, barW*progreso, barH, nc[0], nc[1], nc[2]);

        // Icono de nivel: pequeno rayo/triangulo coloreado.
        tri(barW*0.5f + 0.022f, barY, 0.018f, 0.028f, 0f, nc[0], nc[1], nc[2]);
        // Marcas de division del nivel (cada 20% de la barra).
        for (int d = 1; d < PUNTOS_POR_NIVEL; d++) {
            float mx = -barW*0.5f + barW * (d / (float)PUNTOS_POR_NIVEL);
            rect(mx, barY, 0.004f, barH, 0.10f, 0.11f, 0.14f);
        }
    }

    // Color que representa cada nivel (verde→amarillo→naranja→rojo→purpura).
    private float[] nivelColor(int nivel) {
        switch (nivel) {
            case 1: return new float[]{0.20f, 0.85f, 0.35f};
            case 2: return new float[]{0.75f, 0.90f, 0.15f};
            case 3: return new float[]{1.00f, 0.65f, 0.10f};
            case 4: return new float[]{1.00f, 0.25f, 0.15f};
            default:return new float[]{0.80f, 0.20f, 0.90f};
        }
    }

    // -------------------------------------------------------------------------
    // Pantalla de inicio.
    // -------------------------------------------------------------------------
    private void renderPantallaInicio(float tiempo) {
        // Sombra del panel (desplazada).
        rect(0.015f, -0.015f, 1.38f, 0.68f, 0.03f, 0.04f, 0.05f);
        // Panel principal.
        rect(0f, 0.0f, 1.38f, 0.68f, 0.10f, 0.13f, 0.17f);
        // Franja decorativa superior del panel.
        rect(0f, 0.30f, 1.38f, 0.08f, 0.14f, 0.18f, 0.24f);

        // Borde celeste (4 lados).
        rect(0f,      0.340f, 1.38f, 0.012f, 0.40f, 0.70f, 0.95f);
        rect(0f,     -0.340f, 1.38f, 0.012f, 0.40f, 0.70f, 0.95f);
        rect(-0.690f, 0.0f,   0.012f, 0.68f, 0.40f, 0.70f, 0.95f);
        rect( 0.690f, 0.0f,   0.012f, 0.68f, 0.40f, 0.70f, 0.95f);
        // Esquinas del borde (cuadraditos).
        rect(-0.690f, 0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        rect( 0.690f, 0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        rect(-0.690f,-0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        rect( 0.690f,-0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);

        // Titulo: "2 PLAYERS" como bloques de color alternados.
        float[] tc = {0.40f, 0.70f, 0.95f};
        for (int i = 0; i < 9; i++) {
            float bx = -0.20f + i * 0.048f;
            float[] c = (i % 2 == 0) ? tc : new float[]{1f,1f,1f};
            rect(bx, 0.295f, 0.040f, 0.035f, c[0], c[1], c[2]);
        }

        // Pajaros animados en la pantalla de inicio (flotan).
        float bob1 = (float)Math.sin(tiempo * 2.2f) * 0.025f;
        float bob2 = (float)Math.sin(tiempo * 2.2f + 1.2f) * 0.025f;
        dibujarPajaro(-0.32f, 0.06f + bob1,  bob1 * 12f, birds[0]);
        dibujarPajaro( 0.32f, 0.06f + bob2, -bob2 * 12f, birds[1]);

        // Separador vertical entre los dos pajaros.
        rect(0f, 0.08f, 0.006f, 0.28f, 0.25f, 0.30f, 0.38f);

        // Tecla J1 (barra amarilla debajo del pajaro).
        rect(-0.32f, -0.14f, 0.22f, 0.040f, birds[0].cr*0.7f, birds[0].cg*0.7f, birds[0].cb*0.7f);
        rect(-0.32f, -0.14f, 0.18f, 0.030f, birds[0].cr, birds[0].cg, birds[0].cb);

        // Tecla J2 (barra celeste debajo del pajaro).
        rect( 0.32f, -0.14f, 0.22f, 0.040f, birds[1].cr*0.7f, birds[1].cg*0.7f, birds[1].cb*0.7f);
        rect( 0.32f, -0.14f, 0.18f, 0.030f, birds[1].cr, birds[1].cg, birds[1].cb);

        // Boton parpadeante central "PLAY" (triangulo + barras).
        boolean on = ((int)(tiempo * 1.6f) % 2) == 0;
        if (on) {
            float pulse = 0.85f + (float)Math.sin(tiempo * 6f) * 0.15f;
            tri(0f, -0.255f, 0.07f * pulse, 0.07f * pulse, 0f, 0.95f, 0.95f, 0.60f);
            rect(-0.10f, -0.255f, 0.055f, 0.018f, 0.95f, 0.95f, 0.60f);
            rect( 0.10f, -0.255f, 0.055f, 0.018f, 0.95f, 0.95f, 0.60f);
        }
    }

    // -------------------------------------------------------------------------
    // Pantalla de game over.
    // -------------------------------------------------------------------------
    private void renderPantallaGameOver() {
        float t = (float) GLFW.glfwGetTime();

        // Sombra del panel.
        rect(0.015f, -0.015f, 1.50f, 0.82f, 0.02f, 0.03f, 0.04f);
        // Panel principal.
        rect(0f, 0.02f, 1.50f, 0.82f, 0.08f, 0.09f, 0.12f);
        // Franja superior coloreada.
        rect(0f, 0.37f, 1.50f, 0.08f, 0.55f, 0.12f, 0.12f);

        // Borde rojo (4 lados + esquinas).
        rect(0f,      0.410f, 1.50f, 0.014f, 0.85f, 0.18f, 0.18f);
        rect(0f,     -0.390f, 1.50f, 0.014f, 0.85f, 0.18f, 0.18f);
        rect(-0.750f, 0.010f, 0.014f, 0.82f, 0.85f, 0.18f, 0.18f);
        rect( 0.750f, 0.010f, 0.014f, 0.82f, 0.85f, 0.18f, 0.18f);

        // Decoracion "GAME OVER": bloques rojos y blancos alternados.
        for (int i = 0; i < 9; i++) {
            float bx = -0.20f + i * 0.050f;
            float[] c = (i % 2 == 0) ? new float[]{0.90f,0.18f,0.18f} : new float[]{1f,1f,1f};
            rect(bx, 0.368f, 0.042f, 0.036f, c[0], c[1], c[2]);
        }

        // Resultados: pajaro + barra de score por jugador.
        int sc1 = birds[0].score, sc2 = birds[1].score;
        int maxSc = Math.max(Math.max(sc1, sc2), 1);
        float barMaxW = 0.52f, barH = 0.060f;

        // J1.
        dibujarPajaro(-0.60f, 0.185f, 0f, birds[0]);
        rect(-barMaxW*0.5f*(sc1/(float)maxSc) - 0.05f, 0.185f,
             barMaxW*(sc1/(float)maxSc), barH,
             birds[0].cr*0.55f, birds[0].cg*0.55f, birds[0].cb*0.55f);
        rect(-barMaxW*0.5f*(sc1/(float)maxSc) - 0.05f, 0.188f,
             barMaxW*(sc1/(float)maxSc), barH*0.60f,
             birds[0].cr, birds[0].cg, birds[0].cb);

        // J2.
        dibujarPajaro(-0.60f, 0.060f, 0f, birds[1]);
        rect(-barMaxW*0.5f*(sc2/(float)maxSc) - 0.05f, 0.060f,
             barMaxW*(sc2/(float)maxSc), barH,
             birds[1].cr*0.55f, birds[1].cg*0.55f, birds[1].cb*0.55f);
        rect(-barMaxW*0.5f*(sc2/(float)maxSc) - 0.05f, 0.063f,
             barMaxW*(sc2/(float)maxSc), barH*0.60f,
             birds[1].cr, birds[1].cg, birds[1].cb);

        // Corona/triangulo de ganador.
        if (sc1 > sc2) {
            tri(-0.57f, 0.245f, 0.055f, 0.055f, UP, 1.00f, 0.90f, 0.20f);
            tri(-0.51f, 0.245f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
            tri(-0.63f, 0.245f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
        } else if (sc2 > sc1) {
            tri(-0.57f, 0.120f, 0.055f, 0.055f, UP, 1.00f, 0.90f, 0.20f);
            tri(-0.51f, 0.120f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
            tri(-0.63f, 0.120f, 0.035f, 0.040f, UP, 1.00f, 0.90f, 0.20f);
        } else {
            // Empate: triangulo para cada uno.
            tri(-0.57f, 0.245f, 0.040f, 0.040f, UP, 0.90f, 0.90f, 0.90f);
            tri(-0.57f, 0.120f, 0.040f, 0.040f, UP, 0.90f, 0.90f, 0.90f);
        }

        // Separador horizontal entre los scores.
        rect(0.05f, 0.122f, 0.90f, 0.005f, 0.22f, 0.24f, 0.30f);

        // Nivel alcanzado: barra coloreada.
        float[] nc = nivelColor(nivelActual);
        rect(0.05f, -0.100f, 0.80f, 0.040f, 0.15f, 0.17f, 0.20f);
        rect(0.05f - 0.40f + 0.40f*(nivelActual/(float)NIVEL_MAX),
             -0.100f, 0.80f*(nivelActual/(float)NIVEL_MAX), 0.030f,
             nc[0], nc[1], nc[2]);
        // Marcas de nivel.
        for (int i = 1; i <= NIVEL_MAX; i++) {
            float mx = 0.05f - 0.40f + 0.80f*(i/(float)NIVEL_MAX);
            rect(mx, -0.100f, 0.006f, 0.040f, 0.08f, 0.09f, 0.12f);
        }

        // Boton de reinicio parpadeante.
        boolean on = ((int)(t * 1.4f) % 2) == 0;
        if (on) {
            rect(0.05f, -0.270f, 0.75f, 0.050f, 0.16f, 0.18f, 0.22f);
            rect(0.05f, -0.270f, 0.70f, 0.036f, 0.28f, 0.32f, 0.40f);
            tri(0.05f, -0.270f, 0.030f, 0.030f, 0f, 0.80f, 0.85f, 0.95f);
        }
    }

    // -------------------------------------------------------------------------
    // Render principal.
    // -------------------------------------------------------------------------
    private void render(float tiempo) {
        GL11.glClearColor(0.52f, 0.80f, 0.95f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programa);

        // 1. Fondo (cielo, montanas, suelo, nubes).
        renderFondo();

        // 2. Tuberias con borde lateral oscuro y capuchon en la boca.
        for (Tuberia t : tuberias) {
            float gT  = t.gapCentroY + GAP_ALTO * 0.5f;
            float gB  = t.gapCentroY - GAP_ALTO * 0.5f;
            float sup = 1f - gT;
            float inf = gB + 1f;
            if (sup > 0f) {
                // Sombra lateral derecha.
                rect(t.x + TUBERIA_ANCHO*0.5f - 0.012f, gT + sup*0.5f,
                     0.024f, sup, 0.08f, 0.40f, 0.10f);
                // Cuerpo.
                rect(t.x, gT + sup*0.5f, TUBERIA_ANCHO, sup, 0.15f, 0.60f, 0.18f);
                // Brillo lateral izquierdo.
                rect(t.x - TUBERIA_ANCHO*0.5f + 0.010f, gT + sup*0.5f,
                     0.018f, sup, 0.22f, 0.72f, 0.26f);
                // Capuchon (boca de la tuberia).
                rect(t.x, gT - 0.018f, TUBERIA_ANCHO + 0.035f, 0.036f, 0.10f, 0.45f, 0.13f);
                rect(t.x, gT - 0.018f, TUBERIA_ANCHO + 0.020f, 0.026f, 0.18f, 0.58f, 0.20f);
            }
            if (inf > 0f) {
                rect(t.x + TUBERIA_ANCHO*0.5f - 0.012f, -1f + inf*0.5f,
                     0.024f, inf, 0.08f, 0.40f, 0.10f);
                rect(t.x, -1f + inf*0.5f, TUBERIA_ANCHO, inf, 0.15f, 0.60f, 0.18f);
                rect(t.x - TUBERIA_ANCHO*0.5f + 0.010f, -1f + inf*0.5f,
                     0.018f, inf, 0.22f, 0.72f, 0.26f);
                rect(t.x, gB + 0.018f, TUBERIA_ANCHO + 0.035f, 0.036f, 0.10f, 0.45f, 0.13f);
                rect(t.x, gB + 0.018f, TUBERIA_ANCHO + 0.020f, 0.026f, 0.18f, 0.58f, 0.20f);
            }
        }

        // 3. Pajaros.
        for (Bird b : birds) {
            if (b.y < -1.4f || b.y > 1.3f) continue;
            dibujarPajaro(BIRD_X, b.y, b.velY, b);
        }

        // 4. Franja lateral de jugador muerto.
        for (int i = 0; i < birds.length; i++) {
            if (!birds[i].alive) {
                float px = (i == 0) ? -0.97f : 0.97f;
                rect(px, 0f, 0.04f, 2f,
                     birds[i].cr*0.4f, birds[i].cg*0.4f, birds[i].cb*0.4f);
            }
        }

        // 5. HUD (barra superior, bloques de puntaje, barra de nivel).
        renderHUD();

        // 6. Pantalla de inicio o game over.
        if (!started)       renderPantallaInicio(tiempo);
        else if (gameOver)  renderPantallaGameOver();
    }

    // -------------------------------------------------------------------------
    // Logica del juego.
    // -------------------------------------------------------------------------
    private void calcularNivel() {
        int maxScore = 0;
        for (Bird b : birds) maxScore = Math.max(maxScore, b.score);
        int nuevo = Math.min(maxScore / PUNTOS_POR_NIVEL + 1, NIVEL_MAX);
        if (nuevo != nivelActual) {
            nivelActual         = nuevo;
            velTuberias         = VEL_BASE   + (nivelActual - 1) * VEL_PASO;
            tiempoEntreTuberias = SPAWN_BASE  - (nivelActual - 1) * SPAWN_PASO;
        }
    }

    private void resetGame() {
        birds[0].reset( 0.12f);
        birds[1].reset(-0.12f);
        timerSpawn          = 0f;
        started             = false;
        gameOver            = false;
        nivelActual         = 1;
        velTuberias         = VEL_BASE;
        tiempoEntreTuberias = SPAWN_BASE;
        tuberias.clear();
        actualizarTitulo();
    }

    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS)
            GLFW.glfwSetWindowShouldClose(window, true);

        for (Bird b : birds) {
            boolean jump = false;
            for (int k : b.jumpKeys)
                if (GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS) { jump = true; break; }

            if (jump && !b.prevJump) {
                if (gameOver) { resetGame(); started = true;
                    for (Bird bb : birds) if (bb.alive) bb.velY = IMPULSO_SALTO;
                    break;
                }
                started = true;
                if (b.alive) b.velY = IMPULSO_SALTO;
            }
            b.prevJump = jump;
        }

        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rAhora && !prevR && gameOver) resetGame();
        prevR = rAhora;
    }

    private void actualizar(float dt) {
        // Las nubes se mueven siempre (independiente del estado del juego).
        for (float[] n : nubes) {
            n[0] -= CLOUD_SPEED * dt;
            if (n[0] < -1.5f) n[0] = 1.5f;
        }

        if (!started || gameOver) return;

        for (Bird b : birds) {
            if (!b.alive) {
                b.velY += GRAVEDAD * dt;
                b.y    += b.velY * dt;
                continue;
            }
            b.wingAngle += dt * (Math.abs(b.velY) * 2.5f + 5f);
            b.velY += GRAVEDAD * dt;
            if (b.velY < VELOCIDAD_MAX_CAIDA) b.velY = VELOCIDAD_MAX_CAIDA;
            b.y += b.velY * dt;

            // Colision suelo.
            if (b.y - BIRD_ALTO * 0.5f <= SUELO_Y + SUELO_ALTO * 0.5f) b.alive = false;
            // Colision techo.
            if (b.y + BIRD_ALTO * 0.5f >= 1f) b.alive = false;
        }

        if (!birds[0].alive && !birds[1].alive) { gameOver = true; actualizarTitulo(); return; }

        timerSpawn += dt;
        if (timerSpawn >= tiempoEntreTuberias) { timerSpawn = 0f; spawnTuberia(); }

        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.x -= velTuberias * dt;

            if (t.x + TUBERIA_ANCHO * 0.5f < BIRD_X && !t.puntuada) {
                t.puntuada = true;
                boolean puntuo = false;
                for (Bird b : birds) if (b.alive) { b.score++; puntuo = true; }
                if (puntuo) { calcularNivel(); actualizarTitulo(); }
            }

            for (Bird b : birds)
                if (b.alive && colisionaConTuberia(t, b.y)) b.alive = false;

            if (t.x + TUBERIA_ANCHO * 0.5f < -1.3f) it.remove();
        }

        if (!birds[0].alive && !birds[1].alive) { gameOver = true; actualizarTitulo(); }
    }

    private void spawnTuberia() {
        float gap = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, gap));
    }

    private boolean colisionaConTuberia(Tuberia t, float by) {
        float bL = BIRD_X - BIRD_ANCHO*0.5f, bR = BIRD_X + BIRD_ANCHO*0.5f;
        float bB = by - BIRD_ALTO*0.5f,      bT = by + BIRD_ALTO*0.5f;
        float pL = t.x - TUBERIA_ANCHO*0.5f, pR = t.x + TUBERIA_ANCHO*0.5f;
        if (!(bR > pL && bL < pR)) return false;
        return bT > t.gapCentroY + GAP_ALTO*0.5f || bB < t.gapCentroY - GAP_ALTO*0.5f;
    }

    private void actualizarTitulo() {
        String j1  = birds[0].nombre + ": " + birds[0].score + (birds[0].alive ? "" : " [X]");
        String j2  = birds[1].nombre + ": " + birds[1].score + (birds[1].alive ? "" : " [X]");
        String vel = String.format("%.2f", velTuberias);
        String niv = "Nivel " + nivelActual + "/" + NIVEL_MAX + " (vel=" + vel + ")";
        String base = "Flappy Bird | " + j1 + "  |  " + j2 + "  ||  " + niv;
        if (!started)      GLFW.glfwSetWindowTitle(window, base + "  |  SPACE / W para empezar");
        else if (gameOver) GLFW.glfwSetWindowTitle(window, base + "  |  GAME OVER - SPACE o R");
        else               GLFW.glfwSetWindowTitle(window, base);
    }

    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    // -------------------------------------------------------------------------
    // Bucle principal.
    // -------------------------------------------------------------------------
    private void loop() {
        float ultimo = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt = Math.min(ahora - ultimo, 0.033f);
            ultimo = ahora;
            procesarInput();
            actualizar(dt);
            render(ahora);
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        GL30.glDeleteVertexArrays(vaoQuad); GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoTri);  GL15.glDeleteBuffers(vboTri);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) { 
        new AppFlappyBird().run(); 
    }
}

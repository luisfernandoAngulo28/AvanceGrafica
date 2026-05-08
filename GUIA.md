# Guía del Proyecto OpenGL con Java

## Índice
1. [¿Qué es este proyecto?](#1-qué-es-este-proyecto)
2. [Conceptos clave antes de empezar](#2-conceptos-clave-antes-de-empezar)
3. [Cómo correr cada programa](#3-cómo-correr-cada-programa)
4. [Estructura común de todos los programas](#4-estructura-común-de-todos-los-programas)
5. [App.java — Triángulo estático](#5-appjava--triángulo-estático)
6. [AppMovimientoTeclado.java — Triángulo con movimiento](#6-appmovimientotecladojava--triángulo-con-movimiento)
7. [AppZoom.java — Triángulo con zoom](#7-appzoomjava--triángulo-con-zoom)
8. [AppMovimientoZoom.java — Movimiento + Zoom combinados](#8-appmovimientozoomjava--movimiento--zoom-combinados)
9. [App3D.java — Cubo 3D](#9-app3djava--cubo-3d)
10. [AppCamara.java — Cámara libre en 3D](#10-appcamarajava--cámara-libre-en-3d)
11. [AppLaberinto.java — Laberinto en primera persona](#11-applaberintojava--laberinto-en-primera-persona)
12. [AppFlappyBird.java — Mini juego 2D](#12-appflappybirdjava--mini-juego-2d)
13. [Resumen de conceptos nuevos por clase](#13-resumen-de-conceptos-nuevos-por-clase)

---

## 1. ¿Qué es este proyecto?

Es una serie de programas que enseñan **programación gráfica con OpenGL** usando Java. Cada clase agrega un concepto nuevo encima de la anterior, empezando por dibujar un triángulo y terminando en un mini-juego completo.

**Las herramientas que usa el proyecto:**

| Herramienta | Para qué sirve |
|---|---|
| **Java 17** | El lenguaje en que está escrito todo |
| **Maven** | Herramienta de compilación y ejecución |
| **LWJGL** | Librería que conecta Java con OpenGL y GLFW |
| **GLFW** | Crea la ventana y recibe teclado/mouse |
| **OpenGL** | API que habla con la GPU para dibujar |

---

## 2. Conceptos clave antes de empezar

### ¿Qué es OpenGL?
OpenGL es un conjunto de funciones que le dicen a la **GPU (tarjeta gráfica)** qué dibujar. OpenGL solo dibuja; no crea ventanas, por eso necesitamos GLFW.

### Sistema de coordenadas de OpenGL
OpenGL usa coordenadas que van de **-1 a 1** en X e Y:

```
        Y=1
         |
(-1,1)   |   (1,1)
         |
---------+--------- X
    -1   |       1
         |
(-1,-1)  |   (1,-1)
        Y=-1
```

El centro de la pantalla es `(0, 0)`. Estas se llaman **coordenadas NDC** (Normalized Device Coordinates).

### ¿Qué son los Shaders?
Los shaders son **pequeños programas que corren en la GPU**, escritos en GLSL (un lenguaje parecido a C). Siempre hay dos:

- **Vertex Shader**: se ejecuta una vez por cada vértice (punto). Decide *dónde* va ese punto en la pantalla.
- **Fragment Shader**: se ejecuta una vez por cada píxel del objeto. Decide *de qué color* es ese píxel.

```
CPU (Java)   →   GPU: Vertex Shader   →   GPU: Fragment Shader   →   Pantalla
(envía datos)    (posiciona puntos)        (pinta píxeles)
```

### ¿Qué son VAO y VBO?
- **VBO (Vertex Buffer Object)**: un bloque de memoria en la GPU donde guardamos las coordenadas de los vértices.
- **VAO (Vertex Array Object)**: guarda la *configuración* de cómo leer el VBO. Con solo activar el VAO, OpenGL sabe cómo dibujar el objeto.

### ¿Qué son los Uniforms?
Son variables que se pasan **desde Java (CPU) al shader (GPU)** en cada frame. Por ejemplo, la posición del triángulo o el valor de zoom.

### ¿Qué es deltaTime?
Es el **tiempo que tardó en dibujarse el frame anterior**. Se usa para que el movimiento sea independiente de los FPS:

```
distancia = velocidad × deltaTime
```

Si el juego corre a 60 FPS, `deltaTime ≈ 0.016s`. Si corre a 30 FPS, `deltaTime ≈ 0.033s`. En ambos casos, la distancia recorrida por segundo es la misma.

---

## 3. Cómo correr cada programa

Desde la terminal en la carpeta del proyecto:

```bash
# Compilar todo (solo necesario la primera vez o cuando cambias código)
mvn compile

# Correr cada programa
mvn exec:exec -DmainClass=com.graphics.App
mvn exec:exec -DmainClass=com.graphics.AppMovimientoTeclado
mvn exec:exec -DmainClass=com.graphics.AppZoom
mvn exec:exec -DmainClass=com.graphics.AppMovimientoZoom
mvn exec:exec -DmainClass=com.graphics.App3D
mvn exec:exec -DmainClass=com.graphics.AppCamara
mvn exec:exec -DmainClass=com.graphics.AppLaberinto
mvn exec:exec -DmainClass=com.graphics.AppFlappyBird
```

---

## 4. Estructura común de todos los programas

Todos los programas siguen exactamente el mismo esquema de tres pasos:

```
run()
 ├── init()      → Crear ventana, cargar OpenGL, crear shaders y geometría
 ├── loop()      → Repetir hasta cerrar: leer input → dibujar → mostrar
 └── cleanup()   → Liberar memoria y cerrar GLFW
```

### El bucle principal (`loop`)
Este es el corazón de cualquier programa gráfico:

```java
while (!GLFW.glfwWindowShouldClose(window)) {
    // 1. Calcular deltaTime
    // 2. Leer teclado/mouse
    // 3. Limpiar pantalla
    // 4. Enviar datos al shader (uniforms)
    // 5. Dibujar
    GLFW.glfwSwapBuffers(window);  // Mostrar lo dibujado
    GLFW.glfwPollEvents();         // Procesar eventos del OS
}
```

### Proceso de crear shaders (igual en todas las clases)
```java
// 1. Crear shader en GPU y darle el código GLSL
int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
GL20.glShaderSource(shader, codigoGLSL);
GL20.glCompileShader(shader);

// 2. Crear programa y unir vertex + fragment shader
programa = GL20.glCreateProgram();
GL20.glAttachShader(programa, vertexShader);
GL20.glAttachShader(programa, fragmentShader);
GL20.glLinkProgram(programa);
```

---

## 5. App.java — Triángulo estático

**Lo que hace:** Dibuja un triángulo verde en el centro. Nada más.

**Controles:** Ninguno (solo cerrar la ventana).

### Cómo define el triángulo
```java
float[] vertices = {
     0.0f,  0.5f, 0.0f,   // vértice superior (centro arriba)
    -0.5f, -0.5f, 0.0f,   // vértice inferior izquierdo
     0.5f, -0.5f, 0.0f    // vértice inferior derecho
};
```

Cada vértice tiene 3 valores: `x, y, z`. Como es 2D, z siempre es 0.

### Vertex Shader
```glsl
layout (location = 0) in vec3 aPos;  // recibe la posición del VBO
void main() {
    gl_Position = vec4(aPos, 1.0);   // la pone directamente en pantalla
}
```

### Fragment Shader
```glsl
out vec4 fragColor;
void main() {
    fragColor = vec4(0.2, 0.8, 0.4, 1.0);  // verde fijo (R, G, B, A)
}
```

**Concepto nuevo aquí:** VAO, VBO, Vertex Shader, Fragment Shader.

---

## 6. AppMovimientoTeclado.java — Triángulo con movimiento

**Lo que hace:** El mismo triángulo, pero se mueve con el teclado.

**Controles:**
| Tecla | Acción |
|---|---|
| `W` o `↑` | Mover arriba |
| `S` o `↓` | Mover abajo |
| `A` o `←` | Mover izquierda |
| `D` o `→` | Mover derecha |
| `ESC` | Cerrar |

### Cómo funciona el movimiento

En lugar de cambiar los vértices del triángulo (eso sería lento), se usa un **uniform** que desplaza todos los vértices en el shader:

**En Java** (CPU): se detecta la tecla y se actualiza un valor:
```java
float offsetX = 0.0f;
float offsetY = 0.0f;

// Si presionan D, mover a la derecha
if (teclaD presionada) {
    offsetX += VELOCIDAD * deltaTime;
}

// Enviar el offset al shader
GL20.glUniform2f(uOffsetLocation, offsetX, offsetY);
```

**En el Vertex Shader** (GPU): suma el offset a la posición original:
```glsl
uniform vec2 uOffset;           // recibe offsetX y offsetY desde Java
void main() {
    vec3 pos = vec3(aPos.xy + uOffset, aPos.z);  // suma el desplazamiento
    gl_Position = vec4(pos, 1.0);
}
```

**Concepto nuevo aquí:** Uniforms, deltaTime, procesarInput().

---

## 7. AppZoom.java — Triángulo con zoom

**Lo que hace:** El triángulo se puede agrandar o achicar.

**Controles:**
| Tecla / Acción | Efecto |
|---|---|
| `W` o `+` | Acercar (agrandar) |
| `Q` o `-` | Alejar (achicar) |
| Rueda del mouse | Zoom |
| `ESC` | Cerrar |

### Cómo funciona el zoom

El zoom se implementa **multiplicando** la posición del vértice por un factor:

```glsl
uniform float uZoom;
void main() {
    vec3 pos = vec3(aPos.xy * uZoom, aPos.z);  // multiplica XY por el zoom
    gl_Position = vec4(pos, 1.0);
}
```

- `uZoom = 1.0` → tamaño original
- `uZoom = 2.0` → doble de grande
- `uZoom = 0.5` → mitad de grande

### Zoom con el mouse (ScrollCallback)
```java
GLFW.glfwSetScrollCallback(window, (w, xoffset, yoffset) -> {
    zoom += (float) yoffset * 0.10f;  // yoffset: +1 arriba, -1 abajo
    zoom = clamp(zoom, ZOOM_MIN, ZOOM_MAX);
});
```

**Concepto nuevo aquí:** Zoom con multiplicación, ScrollCallback, función clamp.

---

## 8. AppMovimientoZoom.java — Movimiento + Zoom combinados

**Lo que hace:** Combina las dos clases anteriores: movimiento con WASD y zoom con Q/E y el mouse.

**Controles:**
| Tecla | Acción |
|---|---|
| `W/A/S/D` o flechas | Mover |
| `E` o `+` | Acercar |
| `Q` o `-` | Alejar |
| Rueda del mouse | Zoom |

### Cómo se combina en el shader

El shader recibe **dos uniforms** y los aplica en orden: primero zoom, luego offset:

```glsl
uniform vec2 uOffset;
uniform float uZoom;
void main() {
    // Primero se escala, luego se traslada
    vec3 pos = vec3((aPos.xy * uZoom) + uOffset, aPos.z);
    gl_Position = vec4(pos, 1.0);
}
```

**¿Por qué primero zoom y luego offset?**
Si hicieras al revés (primero offset, luego zoom), el zoom también escalaría el desplazamiento, lo que daría un comportamiento raro.

**Concepto nuevo aquí:** Combinar múltiples transformaciones en el shader.

---

## 9. App3D.java — Cubo 3D

**Lo que hace:** Muestra un cubo 3D con cada cara de un color diferente. Se puede rotar y mover.

**Controles:**
| Tecla | Acción |
|---|---|
| `W/A/S/D` | Mover el cubo en X/Y |
| `+` / `-` | Zoom (acercar/alejar en Z) |
| `←` / `→` | Rotar en Y (yaw) |
| `↑` / `↓` | Rotar en X (pitch) |
| Rueda del mouse | Zoom |

### Cómo se define el cubo

Un cubo tiene 6 caras. Cada cara se dibuja con **2 triángulos** (= 6 vértices por cara × 6 caras = **36 vértices** en total).

Cada vértice ahora tiene **6 valores**: posición (x,y,z) + color (r,g,b):
```java
float[] vertices = {
    // Cara frontal (roja): posX, posY, posZ, R, G, B
    -0.5f, -0.5f,  0.5f,   1.0f, 0.0f, 0.0f,
     0.5f, -0.5f,  0.5f,   1.0f, 0.0f, 0.0f,
    // ...
};
```

### Cómo OpenGL sabe separar posición de color

Se configura con dos "punteros" (atributos):
```java
// Atributo 0: posición — 3 floats, desde el byte 0
GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);

// Atributo 1: color — 3 floats, desde el byte 12 (3 floats × 4 bytes)
GL20.glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
```

El `stride` (6 × 4 = 24 bytes) le dice a OpenGL cuántos bytes saltar para llegar al siguiente vértice.

### Cómo funciona la rotación

La rotación se hace con **matrices de rotación** en el vertex shader. Una matriz de rotación en Y (yaw) se ve así:

```glsl
mat3 rotY = mat3(
     cos(angulo), 0.0, sin(angulo),
     0.0,         1.0, 0.0,
    -sin(angulo), 0.0, cos(angulo)
);
vec3 posRotada = rotY * aPos;
```

### Depth Test (prueba de profundidad)

Para que las caras más cercanas tapen a las más lejanas, se activa el z-buffer:
```java
GL11.glEnable(GL11.GL_DEPTH_TEST);
// Y en cada frame hay que limpiar también el buffer de profundidad:
GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
```

### Proyección en perspectiva

Para que los objetos se vean más chicos cuando están lejos, el shader calcula la proyección manualmente:
```glsl
float fov = radians(60.0);    // campo de visión: 60 grados
float f = 1.0 / tan(fov * 0.5);
// Fórmula estándar de perspectiva:
clip.x = pos.x * (f / aspectRatio);
clip.y = pos.y * f;
clip.z = ...  // cálculo de profundidad
clip.w = -pos.z;  // perspectiva: objetos más lejos = más chicos
```

**Conceptos nuevos aquí:** 3D, atributos múltiples (posición + color), matrices de rotación, depth test, proyección en perspectiva.

---

## 10. AppCamara.java — Cámara libre en 3D

**Lo que hace:** Muestra 5 cubos en la escena y te permite caminar entre ellos como en un videojuego en primera persona.

**Controles:**
| Tecla | Acción |
|---|---|
| `W/S` | Avanzar/retroceder |
| `A/D` | Moverse a los lados (strafe) |
| `Q/E` | Bajar/subir |
| `←` / `→` | Girar la cámara horizontalmente |
| `↑` / `↓` | Girar la cámara verticalmente |
| `Shift` | Moverse más rápido |

### El concepto clave: la cámara no existe en OpenGL

En OpenGL no hay una "cámara real". Para simularla, se **mueve todo el mundo en la dirección opuesta** a donde debería estar la cámara. Si la cámara va a la derecha, el mundo se mueve a la izquierda.

```glsl
// Vista: restar la posición de la cámara al mundo
vec3 viewPos = worldPos - uCamPos;

// Rotar el mundo en dirección opuesta a la cámara
vec3 camSpace = rotPitch * (rotYaw * viewPos);
```

### Movimiento con dirección (forward vector)

El jugador puede mirar en cualquier dirección, entonces "avanzar" no siempre significa moverse en Z. Se calcula un vector de dirección usando el ángulo de rotación (yaw):

```java
float forwardX = (float) Math.sin(yaw);   // componente X de "adelante"
float forwardZ = (float) -Math.cos(yaw);  // componente Z de "adelante"

// Mover en la dirección que se está mirando
camX += forwardX * velocidad;
camZ += forwardZ * velocidad;
```

### Dibujar múltiples cubos con la misma malla

En vez de crear un VBO por cubo, se dibuja **el mismo cubo varias veces** cambiando solo el uniform de posición:

```java
for (float[] cubo : CUBOS) {
    GL20.glUniform3f(uModelOffsetLocation, cubo[0], cubo[1], cubo[2]);
    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36);  // dibuja con posición diferente
}
```

**Conceptos nuevos aquí:** Cámara virtual, view matrix manual, forward vector, reusar mallas.

---

## 11. AppLaberinto.java — Laberinto en primera persona

**Lo que hace:** Un laberinto 3D en primera persona. Tenés que llegar al cubo dorado sin atravesar los muros.

**Controles:**
| Tecla | Acción |
|---|---|
| `W/S` | Avanzar/retroceder |
| `A/D` | Moverse a los lados |
| `←` / `→` | Girar la vista |
| `↑` / `↓` | Mirar arriba/abajo |
| `Shift` | Correr |
| `ESC` | Salir |

### El mapa como matriz

El laberinto se define con una matriz de enteros:
```java
private static final int[][] LABERINTO = {
    {1,1,1,1,1,1,1,1,1,1,1},
    {1,3,0,0,0,1,0,0,0,0,1},  // 3 = inicio del jugador
    // ...
    {1,0,0,0,1,0,0,0,1,2,1},  // 2 = meta (cubo dorado)
    {1,1,1,1,1,1,1,1,1,1,1}
};
// 0 = camino libre, 1 = muro, 2 = meta, 3 = inicio
```

Cada celda de la matriz se convierte a coordenadas 3D del mundo:
```java
float x = worldMinX() + col * TILE + TILE * 0.5f;
float z = worldMinZ() + fila * TILE + TILE * 0.5f;
```

### Colisión contra los muros

Antes de mover al jugador, se verifica si la nueva posición colisiona con algún muro usando **AABB** (caja delimitadora alineada con los ejes):

```java
// Se calcula posición tentativa
float nextX = camX + movimientoX;
float nextZ = camZ + movimientoZ;

// Se valida por cada eje por separado (permite "deslizar" en esquinas)
if (puedeMover(nextX, camZ)) camX = nextX;  // si X es válida, moverse en X
if (puedeMover(camX, nextZ)) camZ = nextZ;  // si Z es válida, moverse en Z
```

La función `puedeMover` revisa solo las celdas cercanas al jugador (no toda la matriz), lo que es más eficiente.

### Escalado de objetos (uModelScale)

Los muros, el piso y la meta son el mismo cubo, pero con **distinto tamaño y posición** controlados por uniforms:

```java
// Piso: ancho y largo del tile, pero muy delgado
GL20.glUniform3f(uModelScaleLocation, TILE, 0.1f, TILE);

// Muro: mismo ancho pero alto completo
GL20.glUniform3f(uModelScaleLocation, TILE, 2.0f, TILE);

// Meta: cubo pequeño y dorado
GL20.glUniform3f(uModelScaleLocation, 0.7f, 0.7f, 0.7f);
GL20.glUniform3f(uTintLocation, 1.0f, 0.85f, 0.2f);  // color dorado
```

**Conceptos nuevos aquí:** Mapa como matriz, colisión AABB, escalado de objetos, tinting de color.

---

## 12. AppFlappyBird.java — Mini juego 2D

**Lo que hace:** Un clon de Flappy Bird. El pájaro cae por gravedad y hay que saltar para esquivar las tuberías.

**Controles:**
| Tecla | Acción |
|---|---|
| `SPACE` | Empezar / Saltar |
| `R` | Reiniciar (solo cuando game over) |
| `ESC` | Salir |

### Un solo quad para todo

En vez de crear geometría diferente para cada objeto, se crea **un solo rectángulo unitario** (de -0.5 a +0.5) y se usa para dibujar todo con distintas escalas y posiciones:

```java
// El mismo quad, con diferentes parámetros:
dibujarRect(BIRD_X, birdY, BIRD_ANCHO, BIRD_ALTO, 0.98f, 0.85f, 0.20f);  // pájaro (amarillo)
dibujarRect(t.x, yCentro, TUBERIA_ANCHO, alto, 0.18f, 0.70f, 0.25f);      // tubería (verde)
```

```java
private void dibujarRect(float x, float y, float ancho, float alto, float r, float g, float b) {
    GL20.glUniform2f(uOffsetLocation, x, y);       // posición
    GL20.glUniform2f(uScaleLocation, ancho, alto); // tamaño
    GL20.glUniform3f(uColorLocation, r, g, b);     // color
    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
}
```

### Física simple

```java
// Cada frame:
birdVelY += GRAVEDAD * dt;         // la gravedad baja la velocidad vertical
birdY    += birdVelY * dt;         // la velocidad mueve al pájaro

// Al saltar (SPACE):
birdVelY = IMPULSO_SALTO;          // se le da un impulso hacia arriba
```

### Detección de flanco (evitar acción repetida)

Si usaras `glfwGetKey` directamente para saltar, mientras el jugador mantiene `SPACE` presionado, saltaría cada frame. Se evita con un "flanco":

```java
boolean spaceAhora = glfwGetKey(window, SPACE) == PRESS;
if (spaceAhora && !prevSpace) {   // ← solo si cambió de suelto a presionado
    birdVelY = IMPULSO_SALTO;
}
prevSpace = spaceAhora;           // guardar estado del frame anterior
```

### Colisión con las tuberías (AABB)

```java
// ¿Hay overlap horizontal?
boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;

// Si hay overlap X, ¿el pájaro está fuera del hueco?
boolean colision = birdTop > gapTop || birdBottom < gapBottom;
```

**Conceptos nuevos aquí:** Quad reutilizable, física de gravedad, detección de flanco, spawn de objetos, game loop con estados (started/gameOver/reset).

---

## 13. Resumen de conceptos nuevos por clase

| Clase | Concepto nuevo |
|---|---|
| `App` | VAO, VBO, Vertex Shader, Fragment Shader, bucle de render |
| `AppMovimientoTeclado` | Uniforms, deltaTime, lectura de teclado |
| `AppZoom` | Zoom por multiplicación, ScrollCallback |
| `AppMovimientoZoom` | Combinar múltiples transformaciones |
| `App3D` | Atributos múltiples, matrices de rotación, depth test, perspectiva |
| `AppCamara` | Cámara virtual, view matrix, forward vector |
| `AppLaberinto` | Mapa en matriz, colisión AABB, escalado de objetos |
| `AppFlappyBird` | Quad reutilizable, física, detección de flanco, game states |

---

> **Tip:** Si algo no queda claro en el código, buscá el concepto en esta tabla y leé la sección correspondiente. Cada sección explica el "por qué" antes del "cómo".

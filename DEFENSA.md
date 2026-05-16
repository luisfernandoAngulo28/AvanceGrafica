# Guía de Defensa — Primer Parcial Programación Gráfica
**Fecha del examen: 16 de mayo de 2026**

---

## Índice
1. [Mapa de constantes — cambios instantáneos](#1-mapa-de-constantes--cambios-instantáneos)
2. [Mapa de funcionalidad — dónde vive cada cosa](#2-mapa-de-funcionalidad--dónde-vive-cada-cosa)
3. [Pedidos probables del docente](#3-pedidos-probables-del-docente)
4. [Estructura general del proyecto](#4-estructura-general-del-proyecto)
5. [Cómo explicar el bucle principal](#5-cómo-explicar-el-bucle-principal)
6. [Requerimiento 2.1 — Pájaro compuesto](#6-requerimiento-21--pájaro-compuesto)
7. [Requerimiento 2.2 — Dos jugadores](#7-requerimiento-22--dos-jugadores)
8. [Requerimiento 2.3 — Velocidad progresiva](#8-requerimiento-23--velocidad-progresiva)
9. [Requerimiento 2.4 — Interfaz mejorada](#9-requerimiento-24--interfaz-mejorada)
10. [Preguntas técnicas sobre OpenGL](#10-preguntas-técnicas-sobre-opengl)
11. [Modificaciones en vivo practicadas](#11-modificaciones-en-vivo-practicadas)
12. [Consejo final para la defensa](#12-consejo-final-para-la-defensa)

---

## 1. Mapa de constantes — cambios instantáneos

> Estas son las líneas donde se concentran los números del juego. Si el docente pide cambiar algo, aquí empieza la búsqueda.

| Qué cambiar | Línea | Variable / valor actual |
|---|---|---|
| Tamaño de la ventana | 19–20 | `ANCHO = 1100`, `ALTO = 720` |
| Posición X de los pájaros | 22 | `BIRD_X = -0.45f` |
| Ancho del pájaro (hitbox) | 23 | `BIRD_ANCHO = 0.10f` |
| Alto del pájaro (hitbox) | 24 | `BIRD_ALTO = 0.09f` |
| Gravedad | 26 | `GRAVEDAD = -1.9f` |
| Fuerza del salto | 27 | `IMPULSO_SALTO = 0.85f` |
| Velocidad máxima de caída | 28 | `VELOCIDAD_MAX_CAIDA = -1.8f` |
| Ancho de tuberías | 30 | `TUBERIA_ANCHO = 0.18f` |
| Hueco inicial entre tuberías | 31 | `GAP_ALTO_BASE = 0.48f` |
| Hueco mínimo en nivel 5 | 32 | `GAP_ALTO_MIN = 0.28f` |
| Variación vertical del hueco | 33–34 | `GAP_MIN_CENTRO = -0.38f`, `GAP_MAX_CENTRO = 0.38f` |
| Velocidad inicial de tuberías | 37 | `VEL_BASE = 0.62f` |
| Tiempo inicial entre tuberías | 38 | `SPAWN_BASE = 1.50f` |
| Incremento de velocidad por nivel | 39 | `VEL_PASO = 0.15f` |
| Reducción de spawn por nivel | 40 | `SPAWN_PASO = 0.13f` |
| Cantidad de niveles | 41 | `NIVEL_MAX = 5` |
| Puntos para subir de nivel | 42 | `PUNTOS_POR_NIVEL = 5` |
| Posición Y del suelo | 45 | `SUELO_Y = -0.88f` |
| Velocidad de las nubes | 48 | `CLOUD_SPEED = 0.07f` |
| Color J1 (amarillo) | 101 | `0.97f, 0.82f, 0.15f` (R, G, B) |
| Color J2 (azul) | 102 | `0.20f, 0.80f, 0.97f` (R, G, B) |
| Tecla salto J1 | 101 | `GLFW_KEY_SPACE` |
| Teclas salto J2 | 102 | `GLFW_KEY_W, GLFW_KEY_UP` |
| Duración del countdown | 738 | `tiempoConteo = 3.0f` |
| Duración del flash al morir | 781, 784, 807 | `b.flashTimer = 0.5f` |

---

## 2. Mapa de funcionalidad — dónde vive cada cosa

```
AppFlappyBird.java
│
├── Línea 19–55   CONSTANTES DEL JUEGO
│                 ← todo lo que controla física, velocidad, tamaño
│
├── Línea 61–79   CLASE Bird (estado por jugador)
│                 ← y, velY, score, alive, prevJump, wingAngle,
│                    flashTimer, jumpKeys[], color (cr,cg,cb), nombre
│
├── Línea 81–87   CLASE Tuberia
│                 ← x, gapCentroY, gapAlto (propio), puntuada
│
├── Línea 100–103 CREACIÓN DE PÁJAROS
│                 ← color y teclas de cada jugador
│
├── Línea 105–115 VARIABLES DE ESTADO
│                 ← started, gameOver, enConteo, nivelActual, velTuberias...
│
├── Línea 127–147 init()
│                 ← GLFW + ventana + OpenGL + shaders + VAOs
│
├── Línea 152–184 crearShaders()
│                 ← vertex shader (escala, rotación, offset)
│                 ← fragment shader (color sólido)
│
├── Línea 197–220 buildVao() + crearQuadBase() + crearTrianguloBase()
│                 ← geometría base subida a GPU (solo se hace 1 vez)
│
├── Línea 225–247 rect() + tri() + ro()
│                 ← PRIMITIVAS DE DIBUJO (toda figura pasa por aquí)
│
├── Línea 252–283 dibujarPajaro()
│                 ← cuerpo, cola, ala animada, pico, ojo, pupila
│                 ← flash blanco al morir (flashTimer)
│                 ← inclinación con tilt = velY * 0.28
│
├── Línea 291–342 renderFondo()
│                 ← cielo (4 franjas), sol, rayos, montañas, suelo, nubes
│
├── Línea 344–381 dibujarNube() + dibujarDigito() + dibujarNumero()
│                 ← nube = 4 rects blancos solapados
│                 ← dígitos = display 7 segmentos con rects
│
├── Línea 386–397 renderConteo()
│                 ← countdown 3-2-1 pulsante (verde/amarillo/rojo)
│
├── Línea 402–462 renderHUD()
│                 ← barra superior, bloques de puntaje, números de score,
│                    indicador de nivel, barra de progreso
│
├── Línea 478–530 renderPantallaInicio()
│                 ← panel con pájaros animados (bob), botón PLAY parpadeante
│
├── Línea 535–624 renderPantallaGameOver()
│                 ← barras de score, números grandes, X roja, corona al ganador
│
├── Línea 629–690 render()
│                 ← ORDEN DE DIBUJO: fondo → tuberías → pájaros → HUD → overlay
│
├── Línea 695–707 calcularNivel()
│                 ← sube nivel, actualiza velTuberias, tiempoEntreTuberias, gapAlto
│
├── Línea 709–723 resetGame()
│                 ← reinicia todo a valores iniciales
│
├── Línea 725–750 procesarInput()
│                 ← detección de flanco (prevJump), inicio countdown, salto, reinicio
│
├── Línea 752–814 actualizar(dt)
│                 ← countdown, física de pájaros, colisión suelo/techo,
│                    spawn de tuberías, puntaje, colisión con tuberías
│
├── Línea 816–827 spawnTuberia() + colisionaConTuberia()
│                 ← spawn pasa gapAlto actual a la nueva tubería
│                 ← colisión AABB: overlap X AND fuera del gap en Y
│
└── Línea 845–857 loop()
                  ← while principal: dt → input → actualizar → render → swap
```

---

## 3. Pedidos probables del docente

| Pedido | Línea exacta | Qué cambiar |
|---|---|---|
| Cambia tecla de J2 a flecha derecha | 102 | `GLFW_KEY_W` → `GLFW_KEY_RIGHT` |
| Cambia tecla de J2 a `E` | 102 | `GLFW_KEY_W` → `GLFW_KEY_E` |
| Pájaro salta más alto | 27 | `IMPULSO_SALTO = 0.85f` → `1.2f` |
| Pájaro cae más rápido | 26 | `GRAVEDAD = -1.9f` → `-2.8f` |
| Cambia color de J1 a rojo | 101 | `0.97f, 0.82f, 0.15f` → `0.95f, 0.15f, 0.15f` |
| Cambia color de J2 a verde | 102 | `0.20f, 0.80f, 0.97f` → `0.20f, 0.90f, 0.20f` |
| Agrega figura al pájaro | 252–283 | Dentro de `dibujarPajaro()` con `ro()` + `rect()`/`tri()` |
| Sube de nivel cada 3 puntos | 42 | `PUNTOS_POR_NIVEL = 5` → `3` |
| Más niveles | 41 | `NIVEL_MAX = 5` → `7` |
| Tuberías más rápidas desde el inicio | 37 | `VEL_BASE = 0.62f` → `0.90f` |
| Hueco de tuberías más chico | 31 | `GAP_ALTO_BASE = 0.48f` → `0.35f` |
| Tuberías más anchas | 30 | `TUBERIA_ANCHO = 0.18f` → `0.25f` |
| Countdown de 5 segundos | 738 | `tiempoConteo = 3.0f` → `5.0f` |
| Flash más largo al morir | 781, 784, 807 | `b.flashTimer = 0.5f` → `1.0f` |
| Altera el vertex shader | 153–165 | Modificar el `String vs` en `crearShaders()` |
| Cambia el color del fondo | 630 | `glClearColor(0.52f, 0.80f, 0.95f, 1f)` |
| Pájaros en posición diferente | 710–711 | `birds[0].reset(0.12f)` y `birds[1].reset(-0.12f)` |

---

## 4. Estructura general del proyecto

**Lo que el docente quiere escuchar:**
> "El proyecto tiene una clase principal `AppFlappyBird` con dos clases internas: `Bird` que encapsula el estado completo de cada jugador, y `Tuberia` que representa cada obstáculo. La lógica está dividida en métodos con responsabilidades claras siguiendo el patrón game loop."

**Mapa mental rápido:**
```
init()         → crea ventana + carga OpenGL + compila shaders + sube geometría a GPU
loop()         → while: procesarInput → actualizar → render → swap → poll
procesarInput  → lee teclado, detecta flancos, inicia countdown, aplica salto
actualizar(dt) → física, colisiones, spawn, puntaje, nivel
render()       → fondo → tuberías → pájaros → HUD → overlay (painter's algorithm)
```

---

## 5. Cómo explicar el bucle principal

**Código** (línea 845):
```java
while (!GLFW.glfwWindowShouldClose(window)) {
    float dt = Math.min(ahora - ultimo, 0.033f);  // deltaTime, máx 33ms
    procesarInput();
    actualizar(dt);
    render(ahora);
    GLFW.glfwSwapBuffers(window);  // muestra el frame dibujado
    GLFW.glfwPollEvents();         // procesa eventos del OS
}
```

**Por qué `Math.min(..., 0.033f)`:**
> "Si el juego se congela un momento, sin este límite deltaTime sería enorme y el pájaro caería metros en un solo frame. El límite de 0.033s equivale a simular como mínimo ~30 FPS. La física sigue siendo correcta pero no explota."

**Por qué `glfwSwapBuffers`:**
> "OpenGL usa doble buffer: dibujamos en el buffer trasero y `swapBuffers` lo muestra instantáneamente. Sin esto el usuario vería el frame dibujándose a medias."

**Por qué `glfwPollEvents`:**
> "Sin este llamado el OS marca la ventana como 'no responde'. Procesa eventos del sistema: teclado, mouse, redimensionar ventana, etc."

---

## 6. Requerimiento 2.1 — Pájaro compuesto

### Resumen
El pájaro tiene **5 partes**: cuerpo (rect), cola (tri), ala animada (rect), pico (tri), ojo + pupila (rect × 2).

### Cómo explicar la inclinación
> "El ángulo `tilt = velY * 0.28` inclina el pájaro: hacia arriba cuando sube, hacia abajo cuando cae. Para que todas las partes se inclinen juntas, uso el método `ro(offsetX, offsetY, tilt)` que aplica la fórmula de rotación 2D al offset local de cada parte:
> ```
> x' = ox·cos(tilt) - oy·sin(tilt)
> y' = ox·sin(tilt) + oy·cos(tilt)
> ```
> Así el pico siempre apunta hacia adelante y el ala siempre está del lado correcto."

### Cómo explicar la animación del ala
> "`wingAngle` acumula tiempo: `wingAngle += dt * (|velY| * 2.5 + 5)`. Cuando el pájaro va rápido, el ala aletea más rápido. El offset Y del ala es `sin(wingAngle) * 0.018` para moverla arriba/abajo, y la rotación del ala también varía con seno para que se doble."

### Cómo explicar el flash al morir
> "El campo `flashTimer` empieza en 0.5s cuando el pájaro colisiona. Cada frame se reduce en `dt`. Mientras sea mayor que cero, la expresión `(int)(flashTimer * 12f) % 2 == 0` alterna entre true y false 12 veces por segundo, haciendo parpadear el color entre blanco y normal."

### Pregunta: ¿por qué la rotación está en el shader?
> "Si rotara los vértices en Java tendría que recalcularlos y subirlos a la GPU cada frame (muy costoso). Con un `uniform float uRotation`, mando un solo float y la GPU rota los vértices en paralelo. Es mucho más eficiente."

---

## 7. Requerimiento 2.2 — Dos jugadores

### Resumen
```java
private final Bird[] birds = {
    new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE),
    new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP)
};
```
Toda la lógica itera sobre `birds[]`. Agregar un tercer jugador = añadir una línea.

### Cómo explicar la detección de flanco
> "Uso `prevJump` para guardar si la tecla estaba presionada el frame anterior. El salto solo ocurre cuando `jumpAhora == true && prevJump == false`, es decir, exactamente en el frame en que se aprieta. Si el jugador mantiene la tecla, no salta repetidamente."

### Cómo explicar cuándo termina la partida
> "Solo cuando `!birds[0].alive && !birds[1].alive`. Mientras uno viva, el juego continúa. El pájaro muerto sigue afectado por la gravedad y cae, dando feedback visual de que perdió."

### Cómo explicar el puntaje independiente
> "Cuando la tubería supera la columna X de los pájaros, itero el array y sumo 1 a cada `Bird` que esté vivo (`b.alive`). Un pájaro muerto no suma. La tubería tiene el flag `puntuada` para no contar la misma más de una vez."

---

## 8. Requerimiento 2.3 — Velocidad progresiva

### Tabla para memorizar

| Nivel | Puntaje necesario | Velocidad | Spawn cada | Gap hueco |
|---|---|---|---|---|
| 1 | 0–4   | 0.62 | 1.50 s | 0.48 |
| 2 | 5–9   | 0.77 | 1.37 s | 0.43 |
| 3 | 10–14 | 0.92 | 1.24 s | 0.38 |
| 4 | 15–19 | 1.07 | 1.11 s | 0.33 |
| 5 | 20+   | 1.22 | 0.98 s | 0.28 |

### Código clave (`calcularNivel`, línea 695)
```java
int nuevo = Math.min(maxScore / PUNTOS_POR_NIVEL + 1, NIVEL_MAX);
if (nuevo != nivelActual) {
    nivelActual         = nuevo;
    velTuberias         = VEL_BASE + (nivelActual - 1) * VEL_PASO;
    tiempoEntreTuberias = SPAWN_BASE - (nivelActual - 1) * SPAWN_PASO;
    gapAlto             = GAP_ALTO_BASE
                          - (nivelActual - 1) * (GAP_ALTO_BASE - GAP_ALTO_MIN)
                          / (float)(NIVEL_MAX - 1);
}
```

### Por qué el nivel usa el máximo entre los dos jugadores
> "Usar la suma haría que dos jugadores suban de nivel el doble de rápido que uno solo. Usar el máximo significa que la dificultad refleja al jugador más hábil, que es la referencia más justa para ambos."

### Por qué cada tubería guarda su propio `gapAlto`
> "Si el gap fuera global, cuando sube el nivel todas las tuberías en pantalla cambiarían de tamaño instantáneamente. Cada tubería recibe el gap del momento en que se crea, y lo conserva. Las que ya están en pantalla no cambian."

### Cómo se refleja el nivel en la interfaz
> "De tres formas: título de la ventana muestra `Nivel 3/5 | Val: 0.92`. El HUD tiene un número de nivel coloreado en el centro. Y hay una barra de progreso que muestra qué tan cerca está el jugador del siguiente nivel."

---

## 9. Requerimiento 2.4 — Interfaz mejorada

### Elementos implementados
- **Fondo**: 4 franjas de cielo degradado, sol con rayos, montañas con nieve, suelo texturado, nubes animadas
- **HUD**: barra superior oscura, bloques de puntaje por jugador, números de puntaje en 7 segmentos, indicador de nivel, barra de progreso al siguiente nivel
- **Pantalla de inicio**: panel con pájaros flotantes, botón PLAY parpadeante, colores de cada jugador
- **Pantalla de Game Over**: barras de score comparativas, números grandes, X roja, corona al ganador, nivel alcanzado
- **Countdown 3-2-1**: animación pulsante verde/amarillo/rojo antes de empezar
- **Flash al morir**: el pájaro parpadea en blanco 0.5 segundos cuando colisiona

### Cómo explicar las nubes sin texturas
> "Cada nube son 4 rectángulos blancos solapados: uno central y otros desplazados a los lados y abajo. El solapamiento da la forma redondeada. Se mueven restando `CLOUD_SPEED * dt` a su X cada frame. Cuando salen por la izquierda (`x < -1.5`), se reinician en `x = 1.5`."

### Cómo explicar los números en 7 segmentos
> "El método `dibujarDigito()` tiene una tabla de 10 filas (dígitos 0-9) con 7 booleans cada una indicando qué segmentos están activos. Cada segmento activo dibuja un `rect()` en la posición correcta. `dibujarNumero()` convierte el int a String y llama `dibujarDigito` por cada carácter."

### Cómo explicar el orden de dibujo
> "OpenGL no tiene capas: lo que se dibuja después aparece encima. Sigo el 'painter's algorithm': primero el fondo, luego tuberías, luego pájaros, luego HUD, y al final los overlays (intro o game over). Así los overlays tapan todo lo demás cuando aparecen."

### Cómo explicar el parpadeo
> "`(int)(tiempo * 1.8f) % 2 == 0` — multiplico el tiempo en segundos por la frecuencia de parpadeo. El módulo 2 alterna entre 0 y 1. No necesito ninguna variable de estado extra, se calcula directo del tiempo global."

---

## 10. Preguntas técnicas sobre OpenGL

| Pregunta | Respuesta clave |
|---|---|
| ¿Qué es un VAO? | Objeto que guarda la configuración de cómo leer el VBO (atributos, stride, offset). Un solo `glBindVertexArray(vao)` restaura toda esa configuración. |
| ¿Qué es un VBO? | Buffer en memoria de la GPU con los datos de vértices. Se sube una sola vez con `glBufferData`. |
| ¿Qué es un uniform? | Variable en el shader cuyo valor viene de Java. Es igual para todos los vértices del mismo draw call. Se cambia con `glUniform*`. |
| ¿Por qué GLFW? | OpenGL no crea ventanas. GLFW crea la ventana, el contexto OpenGL, y maneja teclado/mouse. |
| ¿Qué hace `glSwapBuffers`? | Muestra el buffer trasero (donde dibujamos) en pantalla. Evita ver el frame a medio dibujar. |
| ¿Qué es `deltaTime`? | Segundos desde el frame anterior. Se multiplica por velocidades para que el movimiento sea independiente de los FPS. |
| ¿Qué hace el vertex shader? | Transforma cada vértice: aplica escala (`uScale`), rotación 2D (`uRotation`) y traslación (`uOffset`). Produce la posición final en NDC. |
| ¿Qué hace el fragment shader? | Devuelve el color del píxel. Aquí solo devuelve `uColor` (color sólido). |
| ¿Qué es NDC? | Normalized Device Coordinates. Sistema de coordenadas donde toda la pantalla va de -1 a +1 en X e Y. El centro es (0,0). |
| ¿Por qué `GL_CORE_PROFILE`? | Fuerza el uso de OpenGL moderno (sin funciones deprecadas como `glBegin`/`glEnd`). Requiere VAO/VBO y shaders explícitos. |

---

## 11. Modificaciones en vivo practicadas

> Practica cada una hasta hacerla en **menos de 2 minutos**.

---

### MOD 1 — Cambiar tecla de control
**Enunciado probable:** "Cambiá el salto del jugador 2 a la tecla `E`"

**Línea 102:**
```java
// ANTES:
new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP)

// DESPUÉS:
new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_E)
```
**Otras teclas útiles:** `GLFW_KEY_RIGHT`, `GLFW_KEY_LEFT`, `GLFW_KEY_UP`, `GLFW_KEY_DOWN`, `GLFW_KEY_ENTER`, `GLFW_KEY_A` a `GLFW_KEY_Z`.

---

### MOD 2 — Cambiar física
**Enunciado probable:** "Que el pájaro caiga más rápido" / "Que el salto sea más suave"

**Líneas 26–27:**
```java
private static final float GRAVEDAD       = -1.9f;   // más negativo = cae más rápido
private static final float IMPULSO_SALTO  = 0.85f;   // más grande = salta más alto
```

---

### MOD 3 — Cambiar color de un pájaro
**Enunciado probable:** "Cambiá el pájaro del jugador 1 a color rojo"

**Línea 101** (el orden es `startY, R, G, B, nombre, teclas`):
```java
// ANTES (amarillo):
new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE)

// DESPUÉS (rojo):
new Bird( 0.12f, 0.95f, 0.15f, 0.10f, "J1", GLFW.GLFW_KEY_SPACE)

// Verde:  0.15f, 0.90f, 0.20f
// Azul:   0.15f, 0.45f, 0.95f
// Naranja:0.98f, 0.55f, 0.10f
// Blanco: 0.95f, 0.95f, 0.95f
```

---

### MOD 4 — Cambiar dificultad
**Enunciado probable:** "Que suba de nivel cada 3 puntos" / "Que haya más niveles"

**Líneas 41–42:**
```java
private static final int NIVEL_MAX        = 5;   // más niveles: 7, 10
private static final int PUNTOS_POR_NIVEL = 5;   // menos puntos para subir: 3
```

---

### MOD 5 — Cambiar tamaño del hueco de tuberías
**Enunciado probable:** "Que el hueco sea más chico desde el inicio"

**Líneas 31–32:**
```java
private static final float GAP_ALTO_BASE = 0.48f;  // reducir a 0.35f
private static final float GAP_ALTO_MIN  = 0.28f;  // reducir a 0.18f
```

---

### MOD 6 — Agregar una figura al pájaro
**Enunciado probable:** "Agregale una cresta/antena/sombrero al pájaro"

**Dentro de `dibujarPajaro()` (línea 252), después del cuerpo (línea 266):**
```java
// Cresta (triángulo apuntando arriba)
p = ro(0.010f, 0.058f, tilt);
tri(x+p[0], y+p[1], 0.030f, 0.040f,
    tilt - (float)Math.PI * 0.5f,   // UP = PI/2
    cr, cg * 0.5f, cb * 0.3f);

// Antena (rect delgado arriba)
p = ro(0.015f, 0.070f, tilt);
rect(x+p[0], y+p[1], 0.008f, 0.035f, tilt, 0.9f, 0.9f, 0.9f);
```

**La clave: `ro(offsetX, offsetY, tilt)`** — el primer valor es horizontal (+ = adelante), el segundo es vertical (+ = arriba).

---

### MOD 7 — Alterar el shader
**Enunciado probable:** "Modificá el shader para que todos los objetos tengan un tinte rojo"

**Dentro de `crearShaders()` (línea 166–171), en el fragment shader:**
```glsl
// ANTES:
void main() { fragColor = vec4(uColor, 1.0); }

// DESPUÉS (tinte rojo):
void main() {
    vec3 tinted = uColor * vec3(1.2, 0.8, 0.8);
    fragColor = vec4(tinted, 1.0);
}
```

---

### MOD 8 — Cambiar la velocidad de las nubes
**Enunciado probable:** "Que las nubes se muevan más rápido"

**Línea 48:**
```java
private static final float CLOUD_SPEED = 0.07f;  // aumentar a 0.20f
```

---

### MOD 9 — Que un jugador tenga más gravedad que el otro
**Enunciado probable:** "Que el jugador 2 caiga más rápido"

**Dentro de `actualizar(dt)` (línea 776–777):**
```java
// ANTES:
b.velY += GRAVEDAD * dt;
if (b.velY < VELOCIDAD_MAX_CAIDA) b.velY = VELOCIDAD_MAX_CAIDA;

// DESPUÉS:
float gravedad = (b == birds[1]) ? GRAVEDAD * 1.5f : GRAVEDAD;
b.velY += gravedad * dt;
float velMax = (b == birds[1]) ? -2.5f : VELOCIDAD_MAX_CAIDA;
if (b.velY < velMax) b.velY = velMax;
```

---

### MOD 10 — Cambiar la duración del countdown
**Enunciado probable:** "Que el juego comience en 5 segundos"

**Línea 738:**
```java
tiempoConteo = 3.0f;  // cambiar a 5.0f
```

---

## 12. Consejo final para la defensa

Cuando el docente señale un fragmento, **no leas el código en voz alta**. En cambio explicá las 3 capas:

1. **QUÉ hace** — el resultado visible en pantalla o en la lógica
2. **POR QUÉ** así — la decisión de diseño, el tradeoff
3. **QUÉ pasaría** si lo quitás o cambias — demuestra que entendés las consecuencias

**Ejemplo de respuesta bien estructurada:**
> "Este bloque calcula `deltaTime` y lo limita a 33ms. **Qué hace**: mide cuánto tiempo pasó desde el frame anterior para que la física sea independiente de los FPS. **Por qué el límite**: si el juego se pausa un segundo y luego reanuda, sin el límite la gravedad acumularía todo ese tiempo y el pájaro saltaría fuera de la pantalla en un frame. **Si lo quitás**: en computadoras lentas o al minimizar la ventana, la física explotaría."

---

### Flujo de estados del juego (para explicar rápido)

```
Inicio (started=false, enConteo=false)
    ↓ SPACE / W
Countdown (enConteo=true, tiempoConteo=3.0)
    ↓ 3 segundos
Jugando (started=true, gameOver=false)
    ↓ ambos pájaros muertos
Game Over (gameOver=true)
    ↓ SPACE / ENTER / R
Inicio (resetGame())
```

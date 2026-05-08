# Preparación para la Defensa — Primer Parcial

## Índice
1. [Cómo explicar la estructura general](#1-cómo-explicar-la-estructura-general)
2. [Cómo explicar el bucle principal](#2-cómo-explicar-el-bucle-principal)
3. [Requerimiento 2.1 — Pájaro compuesto](#3-requerimiento-21--pájaro-compuesto)
4. [Requerimiento 2.2 — Dos jugadores](#4-requerimiento-22--dos-jugadores)
5. [Requerimiento 2.3 — Velocidad progresiva](#5-requerimiento-23--velocidad-progresiva)
6. [Requerimiento 2.4 — Interfaz mejorada](#6-requerimiento-24--interfaz-mejorada)
7. [Preguntas probables del docente](#7-preguntas-probables-del-docente)
8. [Modificaciones en vivo más probables](#8-modificaciones-en-vivo-más-probables)

---

## 1. Cómo explicar la estructura general

**Lo que el docente quiere escuchar:**
> "El proyecto tiene una clase principal `AppFlappyBird` con dos clases internas: `Bird` que encapsula el estado de cada jugador, y `Tuberia` que representa cada obstáculo. La lógica está dividida en métodos con responsabilidades claras."

**Mapa mental de la clase:**

```
AppFlappyBird
├── Bird (clase interna)          ← estado por jugador: posición, velocidad, score, color, teclas
├── Tuberia (clase interna)       ← posición, hueco, si ya fue puntuada
├── init()                        ← crea ventana GLFW + carga OpenGL + compila shaders
├── crearShaders()                ← compila GLSL en la GPU
├── crearQuadBase()               ← crea el rectángulo unitario en GPU (VAO/VBO)
├── crearTrianguloBase()          ← crea el triángulo unitario en GPU (VAO/VBO)
├── loop()                        ← bucle principal: input → actualizar → render → swap
├── procesarInput()               ← lee teclado, detecta salto por flanco
├── actualizar(dt)                ← física, colisiones, tuberías, puntaje, nivel
├── render(tiempo)                ← dibuja todo en orden
│   ├── renderFondo()             ← cielo, montañas, suelo, nubes
│   ├── tuberías
│   ├── dibujarPajaro() x2        ← cuerpo + ojo + pico + ala + cola
│   ├── renderHUD()               ← barra superior, bloques de score, barra de nivel
│   ├── renderPantallaInicio()    ← panel con pájaros animados
│   └── renderPantallaGameOver()  ← panel con comparación de scores
├── calcularNivel()               ← actualiza velocidad y spawn según puntaje máximo
└── cleanup()                     ← libera VAO, VBO, programa, ventana
```

---

## 2. Cómo explicar el bucle principal

**Código clave** (`loop()`, línea ~300):
```java
while (!GLFW.glfwWindowShouldClose(window)) {
    float dt = Math.min(ahora - ultimo, 0.033f);  // deltaTime, máximo 33ms
    procesarInput();   // leer teclado
    actualizar(dt);    // física y lógica
    render(ahora);     // dibujar frame
    GLFW.glfwSwapBuffers(window);  // mostrar lo dibujado
    GLFW.glfwPollEvents();         // procesar eventos del OS
}
```

**Por qué `Math.min(..., 0.033f)`:**
> "Si el juego se congela un momento (por ejemplo al cambiar de ventana), sin este límite deltaTime sería enorme y el pájaro saltaría varios metros en un solo frame. El límite de 0.033s equivale a no bajar de ~30 FPS efectivos en la simulación."

**Por qué `glfwSwapBuffers` y `glfwPollEvents`:**
> "OpenGL usa doble buffer: dibujamos en uno oculto y `swapBuffers` lo muestra en pantalla sin que el usuario vea el dibujo a medias. `pollEvents` le dice al OS que procesamos los eventos; sin eso la ventana parece congelada y el OS la marca como 'no responde'."

---

## 3. Requerimiento 2.1 — Pájaro compuesto

### Qué decir sobre el diseño

> "El pájaro es un conjunto de 5 partes dibujadas con el mismo quad y triángulo base, pero cada una con distinto offset, escala, rotación y color. El truco principal es la inclinación: todas las partes se rotan juntas usando el método `ro()` que transforma el offset local según el ángulo de inclinación."

### Preguntas y respuestas sobre este requerimiento

**¿Por qué usás dos VAO (quad y triángulo)?**
> "Un quad son dos triángulos que forman un rectángulo, útil para el cuerpo, ala y ojo. El triángulo puro (vaoTri) tiene la punta en un extremo, lo que lo hace perfecto para el pico y la cola. No puedo simular un triángulo con el quad sin dejar un área en blanco."

**¿Cómo funciona la inclinación?**
> "El ángulo de inclinación (`tilt`) se calcula como `velY * 0.28`, limitado con clamp. El método `ro(lx, ly, tilt)` aplica la fórmula de rotación 2D:
> ```
> x' = lx·cos(tilt) - ly·sin(tilt)
> y' = lx·sin(tilt) + ly·cos(tilt)
> ```
> Así cada parte se reposiciona alrededor del centro del pájaro antes de dibujarse, y todas inclinan juntas como una unidad."

**¿Cómo funciona la animación del ala?**
> "`wingAngle` acumula tiempo cada frame: `wingAngle += dt * (|velY| * 2.5 + 5)`. Cuando el pájaro va rápido, el ala aletea más rápido. El offset Y del ala es `sin(wingAngle) * 0.018`, y la rotación del ala también usa `sin(wingAngle) * 0.35` para que además se incline mientras sube y baja."

**¿Por qué `uRotation` está en el shader y no en Java?**
> "Si rotara los vértices en Java tendría que recalcularlos y subirlos a la GPU cada frame, que es costoso. Con un uniform, mando un solo float al shader y la GPU rota los 3 o 6 vértices en paralelo. Es mucho más eficiente."

---

## 4. Requerimiento 2.2 — Dos jugadores

### Qué decir sobre el diseño

> "Introduje la clase interna `Bird` para encapsular todo el estado de un jugador: posición, velocidad, score, color, nombre y las teclas de salto. El array `birds[]` tiene dos instancias. Toda la lógica que antes era para un jugador ahora itera sobre ese array, por lo que agregar un tercer jugador solo requeriría añadir una línea al array."

### Preguntas y respuestas

**¿Por qué usás una clase `Bird` en vez de duplicar las variables?**
> "Con variables duplicadas (birdY1, birdY2, velY1, velY2...) el código de colisiones, física y render se duplica también. Con `Bird` como clase, escribo los métodos una sola vez y los aplico a cada instancia. Es más fácil de leer, de depurar y de extender."

**¿Cuándo termina la partida?**
> "Solo cuando ambos pájaros están muertos. Después de procesar cada tubería y cada frame, verifico `!birds[0].alive && !birds[1].alive`. Mientras al menos uno viva, el juego continúa. El pájaro muerto sigue cayendo por gravedad para dar feedback visual."

**¿Cómo evitás que una tecla de salto se cuente varias veces por frame?**
> "Uso detección de flanco. `prevJump` guarda si la tecla estaba presionada en el frame anterior. El salto solo ocurre cuando `jumpAhora == true && prevJump == false`, es decir, en el momento exacto en que se aprieta. Si el jugador la mantiene, no salta repetidamente."

**¿Cómo se puntúan los jugadores independientemente?**
> "Cuando la tubería supera la columna de los pájaros, itero `birds[]` y sumo 1 a cada uno que esté vivo. Un pájaro muerto no suma. La tubería tiene un flag `puntuada` para evitar contarlo más de una vez."

---

## 5. Requerimiento 2.3 — Velocidad progresiva

### Qué decir sobre el diseño

> "El nivel se calcula tomando el puntaje más alto entre los dos jugadores, dividiéndolo por `PUNTOS_POR_NIVEL` (5). Con eso obtengo el nivel, limitado a `NIVEL_MAX` (5). Cada nivel aumenta la velocidad de las tuberías en 0.15 y reduce el tiempo entre tuberías en 0.13 segundos."

### Tabla para memorizar

| Nivel | Puntaje | Velocidad | Spawn cada |
|---|---|---|---|
| 1 | 0–4   | 0.62 | 1.50 s |
| 2 | 5–9   | 0.77 | 1.37 s |
| 3 | 10–14 | 0.92 | 1.24 s |
| 4 | 15–19 | 1.07 | 1.11 s |
| 5 | 20+   | 1.22 | 0.98 s |

### Preguntas y respuestas

**¿Por qué el nivel se basa en el máximo de los dos jugadores y no en la suma?**
> "Usar la suma haría que dos jugadores suban de nivel el doble de rápido que uno solo, lo que sería injusto. Usar el máximo significa que la dificultad refleja qué tan bien le está yendo al jugador más hábil, que es una referencia más justa."

**¿Por qué hay un `NIVEL_MAX`?**
> "Sin límite, a puntajes muy altos la velocidad sería imposible de jugar. El cap en nivel 5 mantiene el juego desafiante pero jugable. Además, la barra de nivel del HUD llega al 100% y deja de crecer, lo que visualmente comunica que llegaron al máximo."

**¿Dónde se llama `calcularNivel()`?**
> "Se llama en `actualizar()` justo después de que una tubería puntúa. Solo se recalcula cuando algo cambia, no cada frame, lo que es eficiente."

**¿Cómo se refleja el nivel en la interfaz?**
> "De dos formas: en el título de la ventana aparece `Nivel 3/5 (vel=0.92)` en tiempo real. Y en el HUD hay una barra de progreso que muestra qué tan cerca está el jugador del siguiente nivel, con un color diferente por nivel (verde, amarillo, naranja, rojo, púrpura)."

---

## 6. Requerimiento 2.4 — Interfaz mejorada

### Qué decir sobre el diseño

> "La interfaz tiene cuatro mejoras principales: fondo animado con primitivas OpenGL, HUD con bloques de puntaje visuales, pantalla de inicio interactiva y pantalla de game over con comparación de scores. Todo está dibujado con el mismo quad y triángulo base, cambiando solo los uniforms de posición, escala, rotación y color."

### Preguntas y respuestas

**¿Cómo dibujás las nubes sin texturas?**
> "Cada nube son 3 rectángulos blancos solapados: uno central y dos a los lados ligeramente más bajos. Visualmente el solapamiento da la forma redondeada de una nube. Las nubes tienen una posición X que se decrementa cada frame con `CLOUD_SPEED * dt`, y cuando salen por la izquierda se reinician a `x = 1.5`."

**¿Por qué el HUD usa bloques en vez de números?**
> "Renderizar texto con OpenGL puro requeriría una textura de fuente (font atlas) o cargar una librería externa. Con bloques de colores el puntaje es igualmente legible y se implementa solo con `rect()`. Cada bloque representa un punto; el color del bloque indica a qué jugador pertenece."

**¿Cómo hiciste el efecto parpadeante en las pantallas?**
> "Uso `(int)(tiempo * 1.8f) % 2 == 0`. El tiempo en segundos multiplicado por 1.8 da la frecuencia de parpadeo. El módulo 2 alterna entre 0 y 1, haciendo que el elemento se muestre o se oculte. No necesito ninguna variable de estado extra."

**¿Por qué el render está ordenado así (fondo → tuberías → pájaros → HUD → overlay)?**
> "OpenGL dibuja por capas: lo que se dibuja después aparece encima. El fondo va primero para que todo lo demás lo tape. El HUD va antes que los overlays para que en game over el panel cubra la barra pero no al revés. Es el concepto de 'painter's algorithm'."

---

## 7. Preguntas probables del docente

### Sobre OpenGL en general

| Pregunta | Respuesta clave |
|---|---|
| ¿Qué es un VAO? | Objeto que guarda la configuración de cómo leer los VBOs (qué atributos, qué stride, qué offset). Con un solo bind del VAO, OpenGL ya sabe cómo interpretar los datos. |
| ¿Qué es un VBO? | Buffer en memoria de la GPU con los datos de los vértices (posiciones, colores, UVs, etc.). |
| ¿Qué es un uniform? | Variable en el shader cuyo valor se manda desde la CPU (Java) antes de cada draw call. Es igual para todos los vértices de ese draw. |
| ¿Por qué GLFW? | OpenGL no crea ventanas. GLFW crea la ventana, inicializa el contexto OpenGL y maneja input de teclado/mouse. |
| ¿Qué hace `glfwSwapBuffers`? | Intercambia el buffer trasero (donde dibujamos) con el delantero (el que ve el usuario). Evita ver el frame a medio dibujar. |
| ¿Qué es `deltaTime`? | Tiempo que pasó desde el frame anterior. Se multiplica por la velocidad para que el movimiento sea independiente de los FPS. |

### Sobre el código específico

| Pregunta | Dónde mirar |
|---|---|
| ¿Cómo funciona el vertex shader? | `crearShaders()` — aplica escala, rotación 2D y offset al vértice |
| ¿Cómo se detecta la colisión? | `colisionaConTuberia()` — AABB: overlap en X AND fuera del gap en Y |
| ¿Cómo sabés que un pájaro tocó el suelo? | `actualizar()` — compara `b.y - BIRD_ALTO*0.5f <= SUELO_Y + SUELO_ALTO*0.5f` |
| ¿Qué pasa cuando reiniciás? | `resetGame()` — resetea los dos Bird, limpia tuberías, vuelve nivel 1 |
| ¿Cómo sabe el shader qué figura dibujar? | No lo sabe. Es el Java quien elige qué VAO bindear (quad o triángulo) antes del draw call |

---

## 8. Modificaciones en vivo más probables

Practicá estas antes del 16 de mayo. Cada una debería llevarte **menos de 3 minutos**.

### Modificación 1 — Cambiar una tecla de control
**Enunciado probable:** "Cambiá el salto del jugador 2 a la tecla `E`"

**Dónde:** En el array `birds[]`, segunda entrada:
```java
// ANTES:
new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP)

// DESPUÉS (solo cambiar los jumpKeys):
new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_E)
```

---

### Modificación 2 — Cambiar la gravedad o el impulso
**Enunciado probable:** "Hacé que el pájaro caiga más rápido" o "Reducí el impulso del salto"

**Dónde:** Constantes al principio de la clase:
```java
private static final float GRAVEDAD       = -1.9f;   // más negativo = cae más rápido
private static final float IMPULSO_SALTO  = 0.85f;   // más pequeño = salta menos alto
```

---

### Modificación 3 — Cambiar el color de un pájaro
**Enunciado probable:** "Cambiá el pájaro del jugador 1 a color rojo"

**Dónde:** En el array `birds[]`, primera entrada (los tres últimos floats antes de "J1"):
```java
// ANTES (amarillo):
new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE)

// DESPUÉS (rojo):
new Bird( 0.12f, 0.95f, 0.15f, 0.10f, "J1", GLFW.GLFW_KEY_SPACE)
//                R      G      B
```

---

### Modificación 4 — Cambiar el nivel de dificultad
**Enunciado probable:** "Hacé que el nivel suba cada 3 puntos en vez de 5"

**Dónde:** Constante en la clase:
```java
private static final int PUNTOS_POR_NIVEL = 5;  // cambiar a 3
```

---

### Modificación 5 — Agregar una figura al pájaro
**Enunciado probable:** "Agregale una cresta al pájaro"

**Dónde:** En `dibujarPajaro()`, agregar después del cuerpo:
```java
// Cresta (triangulo apuntando arriba encima del cuerpo)
p = ro(0.010f, 0.058f, tilt);
tri(x+p[0], y+p[1], 0.030f, 0.040f,
    tilt - (float)Math.PI/2,   // rotar 90° para apuntar arriba
    cr*1.1f, cg*0.5f, cb*0.3f);
```

---

### Modificación 6 — Cambiar el gap de las tuberías
**Enunciado probable:** "Hacé el hueco entre las tuberías más chico"

**Dónde:** Constante al principio:
```java
private static final float GAP_ALTO = 0.48f;  // reducir a 0.35f para más difícil
```

---

### Modificación 7 — Agregar velocidad máxima de caída diferente
**Enunciado probable:** "Que el jugador 2 caiga más rápido que el jugador 1"

**Dónde:** En `actualizar()`, dentro del loop de birds, personalizar por jugador:
```java
// Donde está:
if (b.velY < VELOCIDAD_MAX_CAIDA) b.velY = VELOCIDAD_MAX_CAIDA;

// Cambiar a:
float velMax = (b == birds[1]) ? -2.5f : VELOCIDAD_MAX_CAIDA;
if (b.velY < velMax) b.velY = velMax;
```

---

## Consejo final para la defensa

Cuando el docente te pregunte sobre un fragmento de código, **no leas el código en voz alta**. En cambio explicá:
1. **Qué hace** ese bloque (el resultado visible)
2. **Por qué** lo hiciste así (la decisión de diseño)
3. **Qué pasaría** si no estuviera (consecuencia)

Ejemplo:
> "Este bloque calcula `deltaTime` y lo limita a 33ms. Sin el límite, si el juego se pausa un segundo y luego reanuda, el pájaro caería casi 2 unidades en un solo frame porque la física acumularía todo ese tiempo. El límite hace que la simulación sea estable independientemente de lo que pase con el SO."

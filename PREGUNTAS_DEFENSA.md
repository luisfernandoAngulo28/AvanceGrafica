# Preguntas de Defensa — Flappy Bird OpenGL

> Cada pregunta simula lo que el profesor puede preguntarte en el parcial.
> Lee la **Pregunta**, intenta responderla tú solo, y luego lee la **Solución**.

---

## BLOQUE 1 — Bucle Principal y Ciclo de Vida

---

### P1. ¿Cuál es el flujo de ejecución desde que arranca el juego hasta que se dibuja el primer frame?

<details>
<summary>Ver solución</summary>

```
main()
  └─► run()
        ├─► inicializar()      ← crea ventana, compila shaders, sube geometría a la GPU
        ├─► resetGame()        ← posiciona pájaros, limpia tuberías, valores iniciales
        ├─► bucleDeJuego()     ← repite cada frame:
        │       1. calcular deltaTiempo
        │       2. procesarInput()
        │       3. actualizar(deltaTiempo)
        │       4. dibujarEscena(tiempoActual)
        │       5. glfwSwapBuffers()
        │       6. glfwPollEvents()
        └─► liberarRecursos()  ← borra VAO/VBO de la GPU, destruye ventana
```

**Código clave** → `AppFlappyBird.java` línea 89–94 (`run()`) y línea 871–883 (`bucleDeJuego()`).

</details>

---

### P2. ¿Qué es `deltaTiempo` y por qué lo limitas a `0.033f`?

<details>
<summary>Ver solución</summary>

`deltaTiempo` = segundos transcurridos desde el último frame.

Se usa para que la **física sea independiente del framerate**:
```java
pajaro.y += pajaro.velocidadY * deltaTiempo;
```
Si `deltaTiempo` no existiera, en una PC a 300fps el pájaro caería 10 veces más rápido que en una PC a 30fps.

El límite `0.033f` equivale a **~30fps mínimo**. Si la PC se congela 1 segundo, sin el límite `deltaTiempo = 1.0f` y el pájaro teletransportaría. Con el límite, el juego solo "se ralentiza" visualmente pero la física no explota.

**Código** → línea 875:
```java
float deltaTiempo = Math.min(tiempoActual - tiempoUltimoFrame, 0.033f);
```

</details>

---

### P3. ¿Qué hace `glfwSwapBuffers` y por qué es necesario?

<details>
<summary>Ver solución</summary>

El juego usa **double buffering** (dos buffers de imagen):
- **Back buffer** (trasero): aquí OpenGL dibuja silenciosamente el frame nuevo.
- **Front buffer** (delantero): este es el que se ve en pantalla.

`glfwSwapBuffers(window)` **intercambia** los dos buffers al instante.  
Sin esto el usuario vería el dibujo a medias (tearing / parpadeo).

Está activado con `glfwSwapInterval(1)` en línea 109, que lo sincroniza con el refresco del monitor (VSync).

</details>

---

## BLOQUE 2 — OpenGL (VAO, VBO, Shaders)

---

### P4. ¿Qué son VAO y VBO y para qué los usas en tu juego?

<details>
<summary>Ver solución</summary>

| Término | Qué es | En tu juego |
|---------|--------|------------|
| **VBO** (Vertex Buffer Object) | Memoria en la **GPU** que guarda los vértices | Guarda los 6 vértices del rectángulo y los 3 del triángulo |
| **VAO** (Vertex Array Object) | "Recuerda" cómo leer el VBO | Guarda que el atributo 0 = posición XYZ con stride 3 floats |

Tienes **dos pares** VAO/VBO:
- `vaoQuad / vboQuad` → rectángulo (2 triángulos = 6 vértices)
- `vaoTri / vboTri`   → triángulo (3 vértices)

Con esas 2 formas dibujas TODO: cielo, tuberías, pájaros, HUD, etc. Solo cambias el **uniform** de posición (`uOffset`), escala (`uScale`) y color (`uColor`).

**Código** → línea 166–189 (`crearVAO`, `crearGeometriaRectangulo`, `crearGeometriaTriangulo`).

</details>

---

### P5. ¿Qué uniforms usa tu vertex shader y qué hace cada uno?

<details>
<summary>Ver solución</summary>

```glsl
uniform vec2  uOffset;    // posición central del objeto en NDC
uniform vec2  uScale;     // tamaño (ancho, alto) del objeto
uniform float uRotation;  // ángulo de rotación en radianes
```

El vertex shader aplica esta transformación:
```
1. Escala el vértice:        s = aPos.xy * uScale
2. Rota el vértice:          r = rotación 2D de s por uRotation
3. Traslada al lugar final:  gl_Position = vec4(r + uOffset, z, 1.0)
```

El fragment shader solo recibe `uColor` (vec3 RGB) y pinta todos los fragmentos de ese color sólido.

**Código** → línea 122–134 (vertex shader) y 135–140 (fragment shader).

</details>

---

### P6. ¿Qué son las NDC y en qué rango trabaja tu juego?

<details>
<summary>Ver solución</summary>

**NDC = Normalized Device Coordinates** (Coordenadas de Dispositivo Normalizadas).

OpenGL trabaja siempre en el rango **-1 a +1** en X e Y, sin importar el tamaño real de la ventana:
- `x = -1` → borde izquierdo
- `x = +1` → borde derecho
- `y = -1` → borde inferior
- `y = +1` → borde superior

Por eso en tu juego todas las posiciones son números pequeños como `BIRD_X = -0.45f`, `SUELO_Y = -0.88f`, etc.

Tu ventana es `1100 x 720` pixels pero no usas pixels directamente — OpenGL convierte NDC a pixels internamente.

</details>

---

### P7. ¿Por qué compilas los shaders en tiempo de ejecución y no los pones en archivos `.glsl`?

<details>
<summary>Ver solución</summary>

Los shaders están como **Strings de Java** (text blocks con `"""..."""`) en el código, dentro de `crearShaders()` (línea 122).

**Ventajas para este proyecto:**
- No hay que leer archivos del disco en tiempo de ejecución
- Todo el código está en un solo archivo, más fácil de mantener para el examen
- Funciona aunque el directorio de trabajo cambie

El proceso siempre es el mismo independientemente de dónde viene el código:
```
glCreateShader → glShaderSource → glCompileShader → glAttachShader → glLinkProgram
```

Si la compilación falla, el código lanza `RuntimeException` con el error del shader (línea 158–160).

</details>

---

## BLOQUE 3 — Física y Lógica del Juego

---

### P8. Explica cómo funciona la gravedad y el salto del pájaro.

<details>
<summary>Ver solución</summary>

**Gravedad** (se aplica cada frame en `actualizarPajaros`):
```java
pajaro.velocidadY += GRAVEDAD * deltaTiempo;   // GRAVEDAD = -1.9f
pajaro.y          += pajaro.velocidadY * deltaTiempo;
```
La `velocidadY` empieza en 0, la gravedad la hace más negativa cada frame → el pájaro cae.

**Límite de velocidad de caída** (terminal velocity):
```java
if (pajaro.velocidadY < VELOCIDAD_MAX_CAIDA) pajaro.velocidadY = VELOCIDAD_MAX_CAIDA;
// VELOCIDAD_MAX_CAIDA = -1.8f
```

**Salto** (en `procesarInput`):
```java
pajaro.velocidadY = IMPULSO_SALTO;  // IMPULSO_SALTO = 0.85f
```
El salto simplemente reemplaza la velocidad con un valor positivo alto. La gravedad se encarga del resto.

**Edge detection** (evita salto continuo):
```java
if (teclaJumpPresionada && !pajaro.saltoPrevio) { ... }
pajaro.saltoPrevio = teclaJumpPresionada;
```
Solo salta en el frame en que la tecla **cambia** de suelto a presionado.

</details>

---

### P9. ¿Cómo funciona la detección de colisión con las tuberías?

<details>
<summary>Ver solución</summary>

Usa **AABB** (Axis-Aligned Bounding Box — cajas alineadas con los ejes):

```java
// Paso 1: ¿hay superposición en el eje X?
if (!(pajaroBordeDerecho > tuberiaIzquierda && pajaroBordeIzquierdo < tuberiaDerecha))
    return false;   // no hay superposición → no hay colisión

// Paso 2: ¿el pájaro está FUERA del hueco?
return pajaroBordeSuperior > tuberia.centroHuecoY + tuberia.alturaHueco * 0.5f  // golpea arriba
    || pajaroBordeInferior < tuberia.centroHuecoY - tuberia.alturaHueco * 0.5f; // golpea abajo
```

La lógica es: primero verifico que pájaro y tubería se crucen horizontalmente. Si no se cruzan, es imposible colisionar. Si se cruzan, verifico si el pájaro está dentro del hueco. Si está dentro → pasa sin problema. Si está fuera → colisión.

**Código** → línea 839–851 (`colisionaConTuberia`).

</details>

---

### P10. ¿Cómo funciona la dificultad progresiva?

<details>
<summary>Ver solución</summary>

Hay 5 niveles. Cada `PUNTOS_POR_NIVEL = 5` puntos sube el nivel.

Al subir de nivel (en `calcularNivel`, línea 700):
```java
velocidadTuberias   = VEL_BASE   + (nivelActual - 1) * VEL_PASO;
//                  = 0.62       + (nivel-1) * 0.15
tiempoEntreTuberias = SPAWN_BASE - (nivelActual - 1) * SPAWN_PASO;
//                  = 1.50       - (nivel-1) * 0.13   (menos tiempo = más tuberías)
alturaHueco         = GAP_ALTO_BASE - (nivelActual-1) * (GAP_ALTO_BASE - GAP_ALTO_MIN) / (NIVEL_MAX-1);
//                  = 0.48 → ... → 0.28               (hueco más chico)
```

| Nivel | Velocidad | Spawn cada | Tamaño hueco |
|-------|-----------|------------|--------------|
| 1     | 0.62      | 1.50s      | 0.48         |
| 2     | 0.77      | 1.37s      | 0.43         |
| 3     | 0.92      | 1.24s      | 0.38         |
| 4     | 1.07      | 1.11s      | 0.33         |
| 5     | 1.22      | 0.98s      | 0.28         |

**Importante:** `alturaHueco` se asigna por tubería **en el momento del spawn** (línea 836). Las tuberías ya en pantalla **no cambian** su hueco cuando sube el nivel.

</details>

---

## BLOQUE 4 — Renderizado y Gráficos

---

### P11. ¿Por qué dibujas el fondo antes que las tuberías, y las tuberías antes que los pájaros?

<details>
<summary>Ver solución</summary>

Usas el **Algoritmo del Pintor**: dibujas de atrás hacia adelante. Lo último que dibujas queda encima.

```
1. dibujarFondo()    ← cielo, montañas, suelo, nubes  (más atrás)
2. dibujarTuberias() ← tuberías verdes
3. dibujarPajaro()   ← pájaros (encima de tuberías)
4. dibujarHUD()      ← interfaz (encima de todo)
5. pantalla inicio/gameOver ← overlay final
```

Si lo hicieras al revés, las tuberías taparían los pájaros.

OpenGL no tiene profundidad Z automática en este juego (no usas `glEnable(GL_DEPTH_TEST)`), así que el orden de `glDrawArrays` determina quién está adelante.

**Código** → línea 661–695 (`dibujarEscena`).

</details>

---

### P12. ¿Cómo dibujas números en pantalla sin usar fuentes ni texto?

<details>
<summary>Ver solución</summary>

Usas un **display de 7 segmentos** dibujado con rectángulos.

Cada dígito tiene 7 posibles segmentos: arriba, derecha-arriba, derecha-abajo, abajo, izquierda-abajo, izquierda-arriba, y el del medio.

Una tabla `boolean[][]` de 10 filas (0-9) dice qué segmentos activar para cada dígito:
```java
boolean[][] tablaSegmentos = {
    {true, true, true, true, true, true, false},  // 0 — todos menos el del medio
    {false, true, true, false, false, false, false}, // 1 — solo los dos de la derecha
    ...
};
```

Para cada segmento activo llamas a `dibujarRectangulo()` con la posición y tamaño calculados.

`dibujarNumero()` convierte el número a String, separa cada dígito, y llama a `dibujarDigito()` para cada uno con un espaciado calculado.

**Código** → línea 327–366.

</details>

---

### P13. El pájaro se inclina cuando sube y cuando cae. ¿Cómo logras eso?

<details>
<summary>Ver solución</summary>

El ángulo de inclinación se calcula en `dibujarPajaro()` a partir de `velocidadY`:
```java
float anguloInclinacion = bird.alive
    ? clamp(velocidadY * 0.28f, -0.55f, 0.45f)  // vivo: inclina según velocidad
    : -0.60f;                                      // muerto: siempre mirando abajo
```

- `velocidadY > 0` (subiendo) → ángulo positivo → pico apunta hacia arriba
- `velocidadY < 0` (cayendo) → ángulo negativo → pico apunta hacia abajo
- `clamp` limita para que no rote demasiado

Luego pasas `anguloInclinacion` al uniform `uRotation` del shader, que aplica la rotación 2D:
```glsl
float c = cos(uRotation), ss = sin(uRotation);
vec2 r = vec2(s.x*c - s.y*ss, s.x*ss + s.y*c);
```

El ala también se anima con `Math.sin(anguloAla)` que crece con `deltaTiempo` (línea 798).

</details>

---

## BLOQUE 5 — Preguntas de Modificación en Vivo

> Estas preguntas el profesor te pide que **cambies el código en vivo**.

---

### P14. "Cambia la gravedad para que el juego sea más lento"

<details>
<summary>Ver solución</summary>

Línea 26 — cambia `GRAVEDAD`:
```java
// Antes:
private static final float GRAVEDAD = -1.9f;

// Más lento (menos gravedad):
private static final float GRAVEDAD = -0.9f;
```

También puedes reducir `IMPULSO_SALTO` para que el salto sea más corto:
```java
private static final float IMPULSO_SALTO = 0.55f;  // antes era 0.85f
```

</details>

---

### P15. "Agrega un tercer jugador con la tecla flecha derecha"

<details>
<summary>Ver solución</summary>

En la línea 69–72, el array `birds` se crea así:
```java
private final Bird[] birds = {
    new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE),
    new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP)
};
```

Agregar J3 (naranja, flecha derecha):
```java
private final Bird[] birds = {
    new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE),
    new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP),
    new Bird( 0.00f, 1.00f, 0.50f, 0.10f, "J3", GLFW.GLFW_KEY_RIGHT)
};
```

El resto del código itera con `for (Bird b : birds)` → funciona automáticamente con 3 jugadores.

</details>

---

### P16. "Haz que las tuberías empiecen más rápido desde el nivel 1"

<details>
<summary>Ver solución</summary>

Línea 38 — cambia `VEL_BASE`:
```java
// Antes:
private static final float VEL_BASE = 0.62f;

// Más rápido:
private static final float VEL_BASE = 1.00f;
```

O si quiere que arranquen con más tuberías, reduce `SPAWN_BASE`:
```java
// Antes:
private static final float SPAWN_BASE = 1.50f;

// Más tuberías desde el inicio:
private static final float SPAWN_BASE = 0.80f;
```

</details>

---

### P17. "Elimina la cuenta regresiva 3-2-1 para que el juego arranque directo"

<details>
<summary>Ver solución</summary>

En `procesarInput()` línea 743–746:
```java
// Antes:
if (!started && !enCuentaRegresiva) {
    enCuentaRegresiva     = true;
    tiempoCuentaRegresiva = 3.0f;
}

// Sin cuenta regresiva:
if (!started && !enCuentaRegresiva) {
    started = true;
}
```

Eso hace que al presionar la tecla por primera vez el juego arranque de inmediato sin pasar por el countdown.

</details>

---

## BLOQUE 6 — Preguntas Conceptuales Rápidas

---

### P18. ¿Qué hace `glfwPollEvents`?

<details>
<summary>Ver solución</summary>

Le dice a GLFW que procese todos los eventos pendientes del sistema operativo: teclas presionadas, movimiento del mouse, cierre de ventana, etc.

Sin `glfwPollEvents()`, el estado del teclado nunca se actualizaría y la ventana aparecería como "no responde" para el SO.

Se llama **al final** del loop (línea 881), después de dibujar.

</details>

---

### P19. ¿Por qué usas `Iterator` en `actualizarTuberias` en vez de un `for-each` normal?

<details>
<summary>Ver solución</summary>

Porque necesitas **eliminar elementos de la lista mientras la recorres**.

Con un `for-each` normal, borrar elementos durante la iteración lanza `ConcurrentModificationException`.

Con `Iterator` puedes llamar `iteradorTuberias.remove()` de forma segura:
```java
Iterator<Tuberia> iteradorTuberias = tuberias.iterator();
while (iteradorTuberias.hasNext()) {
    Tuberia tuberia = iteradorTuberias.next();
    // ...
    if (tuberia.x + TUBERIA_ANCHO * 0.5f < -1.3f)
        iteradorTuberias.remove();  // seguro
}
```

**Código** → línea 813–831.

</details>

---

### P20. ¿Qué pasa cuando un pájaro muere? ¿Desaparece de pantalla?

<details>
<summary>Ver solución</summary>

No desaparece. Al morir (`pajaro.matar()` línea 49 de Bird.java):
1. `alive = false`
2. `tiempoFlash = 0.5f` → el pájaro parpadea en blanco durante 0.5 segundos

Después del flash, el pájaro **sigue cayendo** con gravedad (sin terminal velocity ni colisiones) hasta salir de pantalla. En `actualizarPajaros()`:
```java
if (!pajaro.alive) {
    pajaro.velocidadY += GRAVEDAD * deltaTiempo;
    pajaro.y          += pajaro.velocidadY * deltaTiempo;
    continue;  // no procesa más lógica
}
```

En `dibujarEscena()` se salta si `b.y < -1.4f || b.y > 1.3f` (línea 675), así que eventualmente desaparece al salir del NDC.

</details>

---

*Fin del archivo — ¡Buena suerte en la defensa!*

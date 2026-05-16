# Entiende AppFlappyBird con Analogías
> Todo el código explicado con comparaciones del mundo real.

---

## 1. La ventana y GLFW — *El teatro y el escenario*

Imagina que OpenGL es un **actor muy talentoso** que sabe actuar perfectamente, pero **no sabe construir el teatro** donde va a actuar. GLFW es el **constructor del teatro**: levanta el escenario (la ventana), instala las luces (el contexto OpenGL) y pone la boletería (el teclado/mouse).

```java
GLFW.glfwInit();                          // construir el teatro
GLFW.glfwCreateWindow(1100, 720, "", ...); // levantar el escenario (1100x720 px)
GL.createCapabilities();                   // conectar al actor (OpenGL)
```

**Sin GLFW, OpenGL no tiene dónde actuar.**

---

## 2. VAO y VBO — *El sello de goma y la tinta*

Imagina que tienes que dibujar la misma figura (por ejemplo, un cuadrado) **miles de veces** en distintos lugares de una hoja.

- El **VBO** es la **forma del sello** guardada en la GPU — los vértices del cuadrado cargados una sola vez.
- El **VAO** es **cómo agarrar el sello** — guarda la configuración de cómo leer esos vértices.
- Cada `rect()` o `tri()` es **estampar el sello** en una posición diferente, con un color diferente.

```java
// Se crea el sello UNA SOLA VEZ al inicio:
crearQuadBase();      // sello cuadrado
crearTrianguloBase(); // sello triángulo

// Cada frame se estampa N veces con distintos parámetros:
rect(0.5f, 0.3f, 0.2f, 0.1f, 1f, 0f, 0f); // cuadrado rojo aquí
rect(-0.2f, 0.0f, 0.3f, 0.3f, 0f, 1f, 0f); // cuadrado verde allá
```

**La ventaja**: la forma del cuadrado está en la GPU **una sola vez**. Cambiar dónde y cómo aparece es casi gratis.

---

## 3. Shaders — *Las instrucciones para el empleado de la GPU*

La GPU tiene **miles de empleados** (núcleos) que trabajan en paralelo. Los shaders son las **instrucciones escritas** que les das:

### Vertex Shader — *El ubicador de mesas en un restaurante*
Recibe cada vértice (esquina de la figura) y decide **dónde colocarlo** en la pantalla.

```glsl
void main() {
    vec2 s = aPos.xy * uScale;          // 1. escalar (agrandar/achicar)
    // 2. rotar (girar alrededor del centro)
    vec2 r = vec2(s.x*cos(uRotation) - s.y*sin(uRotation),
                  s.x*sin(uRotation) + s.y*cos(uRotation));
    gl_Position = vec4(r + uOffset, ...); // 3. trasladar (mover al lugar final)
}
```

Es como decirle al empleado: *"Toma esta silla (vértice), agrándala, gírala y ponla en esa mesa (posición)"*.

### Fragment Shader — *El pintor de paredes*
Una vez que los vértices están ubicados, el fragment shader decide el **color de cada píxel** adentro de la figura.

```glsl
void main() {
    fragColor = vec4(uColor, 1.0); // pinta todo del color que le mando
}
```

Tan simple como decirle: *"Pintá todo de ese color"*.

---

## 4. Uniforms — *Los controles del tablero de mando*

Imagina una **máquina de hacer figuras** con palancas y diales:
- Una palanca controla **dónde aparece** (`uOffset`)
- Otra controla **el tamaño** (`uScale`)
- Otra controla **la rotación** (`uRotation`)
- Otra controla **el color** (`uColor`)

```java
// Antes de dibujar, configurás los diales:
GL20.glUniform2f(uOffsetLoc, x, y);   // "ponelo aquí"
GL20.glUniform2f(uScaleLoc,  w, h);   // "de este tamaño"
GL20.glUniform1f(uRotLoc,    rot);    // "girado así"
GL20.glUniform3f(uColorLoc,  r,g,b);  // "de este color"
// Luego apretás el botón:
GL11.glDrawArrays(...);               // "¡dibujá!"
```

Los uniforms son **iguales para todos los vértices** de ese dibujo — es la palanca que mueve toda la máquina a la vez.

---

## 5. El Sistema de Coordenadas NDC — *El mapa del escenario*

OpenGL no trabaja con píxeles, trabaja con **coordenadas normalizadas** que van de **-1 a +1**:

```
         +1 (arriba)
          |
-1 ------0------ +1
(izq)    |        (der)
         -1 (abajo)
```

Es como un **mapa cuadriculado** donde el centro del escenario es `(0, 0)`, el borde derecho es `x = 1`, el borde izquierdo es `x = -1`, arriba es `y = 1` y abajo es `y = -1`.

```java
BIRD_X = -0.45f  // el pájaro está un poco a la izquierda del centro
SUELO_Y = -0.88f // el suelo está casi al fondo
```

**¿Por qué no píxeles?** Porque así el código funciona igual en cualquier resolución de pantalla.

---

## 6. El Bucle Principal (loop) — *La cadena de montaje de una fábrica*

El juego es como una **fábrica que produce frames** (imágenes) sin parar, 60 veces por segundo:

```
┌─────────────────────────────────────────────┐
│  CADENA DE MONTAJE (se repite 60x/segundo)  │
│                                             │
│  1. LEER TECLADO  →  ¿alguien apretó algo? │
│  2. ACTUALIZAR    →  mover pájaros, física  │
│  3. DIBUJAR       →  pintar todo            │
│  4. MOSTRAR       →  glfwSwapBuffers        │
│  5. REPETIR       →  volver al paso 1       │
└─────────────────────────────────────────────┘
```

```java
while (!glfwWindowShouldClose(window)) {  // mientras la fábrica esté abierta
    procesarInput();   // leer teclado
    actualizar(dt);    // mover todo
    render(ahora);     // dibujar
    glfwSwapBuffers(window); // mostrar
    glfwPollEvents();        // escuchar al OS
}
```

---

## 7. Delta Time (dt) — *El velocímetro independiente del motor*

Imagina dos autos en una pista:
- Auto A tiene un motor potente → da 120 vueltas por minuto
- Auto B tiene un motor lento → da 30 vueltas por minuto

Si la velocidad del juego dependiera de las vueltas (FPS), en el Auto A el juego iría **4 veces más rápido**. Eso sería injusto.

**`dt`** es como un **velocímetro que normaliza la velocidad**: en vez de "mover X píxeles por frame", se mueve "X unidades por segundo", sin importar cuántos frames haya.

```java
float dt = ahora - ultimo;          // tiempo desde el frame anterior (en segundos)
dt = Math.min(dt, 0.033f);          // máximo 33ms (por si la PC se congela)

b.y += b.velY * dt;                 // mover el pájaro: velocidad × tiempo
b.velY += GRAVEDAD * dt;            // aplicar gravedad: aceleración × tiempo
```

Si el juego va a 60 FPS, `dt ≈ 0.016s`. Si va a 30 FPS, `dt ≈ 0.033s`. El pájaro **siempre cae a la misma velocidad real**.

---

## 8. La Clase Bird — *La ficha de jugador en un juego de mesa*

Imagina que estás jugando un juego de mesa y cada jugador tiene una **ficha con sus datos**:

```
┌──────────────────────────────────┐
│  FICHA DE JUGADOR                │
│  Nombre: J1                      │
│  Color: amarillo (0.97, 0.82, 0.15) │
│  Posición Y: 0.12                │
│  Velocidad: 0 m/s                │
│  Puntaje: 0                      │
│  ¿Vivo?: SÍ                      │
│  Teclas: [ESPACIO]               │
└──────────────────────────────────┘
```

```java
private static class Bird {
    float y, velY;          // posición y velocidad actual
    int   score;            // puntaje acumulado
    boolean alive;          // ¿sigue en juego?
    float wingAngle;        // ángulo del ala (para la animación)
    float flashTimer;       // tiempo restante del flash blanco al morir
    final int[] jumpKeys;   // teclas asignadas a este jugador
    final float cr, cg, cb; // color del pájaro (R, G, B)
}
```

Tener una clase `Bird` significa que **el mismo código** sirve para J1 y J2 sin duplicar nada. Si quisieras agregar un J3 solo agregarías una línea.

---

## 9. La Física del Pájaro — *Una pelota con un resorte hacia arriba*

El pájaro vive en un mundo donde:
- La **gravedad** jala constantemente hacia abajo (como en la vida real)
- El **salto** es un impulso brusco hacia arriba (como un resorte que se dispara)
- La **velocidad máxima de caída** evita que el pájaro caiga infinitamente rápido

```
Sin salto:                Con salto:
  velY = 0                 velY = +0.85 (impulso)
  ↓ gravedad               ↓ gravedad cada frame
  velY = -0.1              velY = +0.75
  velY = -0.2              velY = +0.65
  velY = -0.3    →→→       velY = +0.40
  ...                      velY = 0 (tope de la trayectoria)
  velY = -1.8 (límite)     velY = -0.10
                           velY = -0.20 ...
```

```java
b.velY += GRAVEDAD * dt;                  // gravedad jala hacia abajo cada frame
if (b.velY < VELOCIDAD_MAX_CAIDA)         // límite de velocidad de caída
    b.velY = VELOCIDAD_MAX_CAIDA;
b.y += b.velY * dt;                       // posición = posición + velocidad × tiempo

// Al saltar:
b.velY = IMPULSO_SALTO; // +0.85 hacia arriba (reemplaza la velocidad actual)
```

---

## 10. La Inclinación del Pájaro — *Un avión que inclina la nariz*

Cuando el pájaro sube rápido, su nariz apunta hacia arriba. Cuando cae, apunta hacia abajo. Esto se logra con el ángulo `tilt`:

```java
float tilt = clamp(velY * 0.28f, -0.55f, 0.45f);
//           velocidad × sensibilidad, limitado entre caer (-0.55) y subir (0.45)
```

El truco de `ro()` es como **rotar un reloj**: si el cuerpo del pájaro gira, el pico, el ala y el ojo también giran alrededor del mismo centro.

```java
// ro(x_local, y_local, angulo) → nueva posición rotada
p = ro(0.058f, -0.004f, tilt);  // el pico está 0.058 adelante y 0.004 abajo
// Después de rotar con tilt, el pico siempre apunta en la dirección correcta
tri(x+p[0], y+p[1], ...);       // dibuja el pico en la posición rotada
```

Es como pegar todas las partes del pájaro en una rueda y girar la rueda entera.

---

## 11. Detección de Flanco — *El botón del ascensor*

Cuando apretás el botón de un ascensor, sube **una vez** aunque lo tengas presionado 5 segundos. El ascensor detecta el "primer toque", no "está apretado".

En el juego, sin detección de flanco, mantener SPACE haría que el pájaro saltara **60 veces por segundo**. Con `prevJump` se detecta solo el momento exacto en que se aprieta:

```java
boolean jump = (teclaPresionada ahora);
if (jump && !b.prevJump) {   // ahora sí, antes no → es el primer frame
    b.velY = IMPULSO_SALTO;  // saltá UNA vez
}
b.prevJump = jump;           // guardar estado para el próximo frame
```

```
Frame 1: jump=true,  prevJump=false → SALTA ✓
Frame 2: jump=true,  prevJump=true  → no hace nada
Frame 3: jump=true,  prevJump=true  → no hace nada
Frame 4: jump=false, prevJump=true  → soltó la tecla
Frame 5: jump=true,  prevJump=false → SALTA ✓ (nuevo toque)
```

---

## 12. Colisión AABB — *¿Se solapan dos cajas?*

AABB significa "Axis-Aligned Bounding Box" — básicamente, verificar si **dos rectángulos se superponen**.

Imagina dos cajas de cartón vistas desde arriba. Se chocan si Y SOLO SI se superponen en X **Y** también en Y:

```
Tubería (vista de frente):
  ████████            ← tubería superior
  ████████
            [ pájaro ]
  ████████            ← tubería inferior
  ████████

¿El pájaro chocó?
  1. ¿El pájaro está en el rango X de la tubería? (¿está pasando por ahí?)
  2. ¿Está fuera del hueco? (¿está tocando una tubería en Y?)
  Si ambas son SÍ → colisión.
```

```java
// 1. ¿Hay overlap en X?
float bL = BIRD_X - BIRD_ANCHO*0.5f;  // borde izquierdo del pájaro
float bR = BIRD_X + BIRD_ANCHO*0.5f;  // borde derecho del pájaro
float pL = t.x - TUBERIA_ANCHO*0.5f;  // borde izquierdo de la tubería
float pR = t.x + TUBERIA_ANCHO*0.5f;  // borde derecho de la tubería
if (!(bR > pL && bL < pR)) return false; // no hay overlap en X → no choca

// 2. ¿Está FUERA del hueco en Y?
float bT = by + BIRD_ALTO*0.5f;                    // borde superior del pájaro
float bB = by - BIRD_ALTO*0.5f;                    // borde inferior del pájaro
return bT > t.gapCentroY + t.gapAlto*0.5f          // choca con tubería de arriba
    || bB < t.gapCentroY - t.gapAlto*0.5f;         // choca con tubería de abajo
```

---

## 13. El Painter's Algorithm — *Pintar un cuadro por capas*

Cuando un pintor hace un paisaje, primero pinta el cielo, luego las montañas, luego los árboles, y al final las personas en primer plano. Si pintara al revés, las montañas taparían a las personas.

OpenGL funciona igual: **lo que se dibuja después aparece encima**.

```java
void render() {
    renderFondo();          // 1° cielo, montañas, suelo, nubes (fondo)
    // tuberías             // 2° tuberías (encima del fondo)
    // pájaros              // 3° pájaros (encima de las tuberías)
    renderHUD();            // 4° barra superior (encima de todo el juego)
    renderPantallaGameOver(); // 5° overlay de game over (encima de absolutamente todo)
}
```

Si dibujara el HUD antes que el fondo, el fondo taparía el HUD y no lo verías.

---

## 14. La Dificultad Progresiva — *Una escalera mecánica que se acelera*

Imagina que estás en una escalera mecánica que va acelerando. Al inicio es cómoda, pero cuanto más tiempo pasás encima, más rápido va.

```java
// Cada vez que el mejor jugador suma 5 puntos, sube un nivel:
nivelActual         = maxScore / 5 + 1;

// El nivel controla 3 cosas:
velTuberias         = 0.62 + (nivel-1) * 0.15;   // escalera más rápida
tiempoEntreTuberias = 1.50 - (nivel-1) * 0.13;   // escalones más seguidos
gapAlto             = 0.48 - (nivel-1) * 0.05;   // escalones más angostos
```

| Nivel | Es como... |
|---|---|
| 1 | Escalera lenta, escalones anchos, fácil |
| 3 | Escalera rápida, hay que estar atento |
| 5 | Escalera muy rápida, hueco angosto, desafiante |

Además, cada tubería guarda su propio `gapAlto` al momento de crearse — así las tuberías que ya están en pantalla no cambian de tamaño de repente (sería injusto).

---

## 15. El Display de 7 Segmentos — *El reloj digital del microondas*

Para mostrar números sin usar fuentes de texto, el código dibuja cada dígito como un **reloj digital**: 7 segmentos que se encienden o apagan.

```
 _
|_|   ← el número 8 tiene todos los segmentos encendidos
|_|

 _
  |   ← el número 1 solo tiene los segmentos derechos
  |
```

```java
boolean[][] segs = {
//  top  topDer botDer bot  botIzq topIzq medio
   {true, true,  true, true, true,  true,  false}, // 0
   {false,true,  true, false,false, false, false},  // 1
   ...
};
// Cada true dibuja un rect() en la posición correcta del segmento
```

`dibujarNumero(42, x, y, ...)` dibuja el "4" y luego el "2" uno al lado del otro, calculando el espaciado automáticamente.

---

## 16. El Flash al Morir — *La luz de un faro*

Cuando el pájaro choca, parpadea en blanco rápidamente para dar feedback visual. Es como el flash de una cámara.

```java
// Al morir:
b.flashTimer = 0.5f;  // 0.5 segundos de flash

// Cada frame, el timer baja:
b.flashTimer = Math.max(0f, b.flashTimer - dt);

// Para parpadear: alternar blanco/normal 12 veces por segundo
boolean flash = b.flashTimer > 0f && ((int)(b.flashTimer * 12f) % 2) == 0;
//              ¿está en flash?    ¿es el semiciclo "encendido"?

float cr = flash ? 1f : bird.cr * dim;  // blanco si flash, color normal si no
```

`(int)(flashTimer * 12) % 2` es como un interruptor que se enciende y apaga 12 veces por segundo — el % 2 alterna entre 0 y 1.

---

## 17. El Countdown 3-2-1 — *El semáforo antes de una carrera*

Antes de que empiece la carrera, todos los autos esperan el semáforo: **rojo → amarillo → verde**. El countdown hace lo mismo:

```java
tiempoConteo = 3.0f;  // arranca en 3 segundos

// Cada frame:
tiempoConteo -= dt;   // el reloj corre
int num = (int)Math.ceil(tiempoConteo);  // 2.7s → muestra "3", 1.9s → "2", etc.

// El número pulsa: empieza grande y se achica cada segundo
float frac = tiempoConteo - (num - 1);  // 0..1 dentro del segundo actual
float size = 0.15f + frac * 0.38f;      // grande (0.53) al inicio, chico (0.15) al final

// Color del semáforo:
if (num == 3) { color = verde;   }
if (num == 2) { color = amarillo;}
if (num == 1) { color = rojo;    }
```

Cuando `tiempoConteo <= 0`, el semáforo se apaga y el juego empieza.

---

## Resumen de analogías

| Concepto técnico | Analogía |
|---|---|
| GLFW | Constructor del teatro |
| OpenGL | El actor que actúa en el teatro |
| VBO | La forma del sello de goma |
| VAO | Cómo agarrar el sello |
| Vertex Shader | El ubicador de mesas en un restaurant |
| Fragment Shader | El pintor que da color |
| Uniform | Palancas del tablero de mando |
| NDC | Mapa cuadriculado del escenario (-1 a +1) |
| Game Loop | Cadena de montaje de frames |
| Delta Time | Velocímetro independiente del motor |
| Clase Bird | Ficha de jugador en juego de mesa |
| Gravedad + Salto | Pelota con resorte |
| Inclinación (tilt) | Avión que inclina la nariz |
| Detección de flanco | Botón del ascensor |
| Colisión AABB | ¿Dos cajas se superponen? |
| Painter's Algorithm | Pintor que trabaja por capas |
| Dificultad progresiva | Escalera mecánica que se acelera |
| Display 7 segmentos | Reloj digital del microondas |
| Flash al morir | Flash de una cámara |
| Countdown 3-2-1 | Semáforo de largada |

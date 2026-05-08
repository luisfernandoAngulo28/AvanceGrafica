# Resumen del Parcial — Estado actual del proyecto

> Fecha parcial: **16 de mayo de 2026**
> Archivo principal: `AppFlappyBird.java`

---

## Checklist general

| # | Requerimiento | Estado |
|---|---|---|
| 2.1 | Pájaro con figuras geométricas + animación | ✅ Completo |
| 2.2 | Modo dos jugadores simultáneos | ✅ Completo |
| 2.3 | Velocidad progresiva por puntaje | ✅ Completo |
| 2.4 | Mejora de interfaz | ✅ Mayormente completo |
| E1 | Proyecto compilable con Maven | ✅ `mvn clean compile` funciona |
| E2 | Código comentado y organizado | ⚠️ Todo en una clase (ver nota) |
| E3 | Recursos incluidos en el repositorio | ⚠️ No hay sonido ni imágenes |
| E4 | README con controles e instrucciones | ❌ Pendiente |

---

## 2.1 — Pájaro compuesto por figuras geométricas

**Estado: ✅ Completo**

El pájaro se dibuja en `dibujarPajaro(x, y, velY, Bird)` usando 5 partes:

| Parte | Primitiva | Código |
|---|---|---|
| Cuerpo | `rect()` | `rect(x, y, BIRD_ANCHO, BIRD_ALTO, tilt, cr, cg, cb)` |
| Cola | `tri()` | `tri(x+p[0], y+p[1], 0.045f, 0.030f, tilt+PI, ...)` |
| Ala | `rect()` animada | `rect(x+p[0], y+p[1], 0.065f, 0.028f, wr, ...)` |
| Pico | `tri()` | `tri(x+p[0], y+p[1], 0.038f, 0.024f, tilt, ...)` |
| Ojo + pupila | 2× `rect()` | ojo blanco + pupila negra encima |

### ¿Cómo funciona la inclinación?
```java
float tilt = bird.alive ? clamp(velY * 0.28f, -0.55f, 0.45f) : -0.60f;
```
- Si `velY > 0` (subiendo) → inclina hacia arriba
- Si `velY < 0` (cayendo) → inclina hacia abajo
- Al morir → `-0.60f` fijo (boca abajo)

Todas las partes se posicionan con `ro(lx, ly, tilt)` (rotación 2D) para que el pájaro gire como una unidad:
```java
float[] p = ro(-0.057f, 0f, tilt);  // cola
tri(x+p[0], y+p[1], ...);
```

### ¿Cómo funciona el aleteo?
```java
b.wingAngle += dt * (Math.abs(b.velY) * 2.5f + 5f);
float wr = tilt + (float)Math.sin(wa) * 0.35f;  // rotación del ala
float woy = (float)Math.sin(wa) * 0.018f;        // desplazamiento vertical del ala
```
- El ala aletea más rápido cuanto mayor es la velocidad vertical
- Al morir el aleteo se detiene

---

## 2.2 — Modo dos jugadores

**Estado: ✅ Completo**

### Estructura Bird
```java
private static class Bird {
    float y, velY;
    int   score;
    boolean alive, prevJump;
    float wingAngle;
    final int[]  jumpKeys;   // SPACE para J1, W+ARRIBA para J2
    final float  cr, cg, cb; // color individual
    final String nombre;     // "J1" / "J2"
}
```

### Jugadores
| Jugador | Color | Tecla(s) | Posición inicial |
|---|---|---|---|
| J1 | Amarillo (0.97, 0.82, 0.15) | ESPACIO | y = +0.12 |
| J2 | Cyan (0.20, 0.80, 0.97) | W o FLECHA ARRIBA | y = -0.12 |

### Reglas
- Tuberías **compartidas** entre ambos jugadores
- El juego **continúa** mientras al menos uno esté vivo
- El juego termina cuando **ambos** están muertos
- Al morir un jugador: el pájaro sigue cayendo y aparece una franja lateral de su color
- El puntaje se muestra en el HUD con bloques individuales de color

### Detección de teclas (edge detection)
```java
if (jump && !b.prevJump) { ... }  // solo al primer frame del press
b.prevJump = jump;
```

---

## 2.3 — Velocidad progresiva

**Estado: ✅ Completo**

### Niveles
| Nivel | Puntaje mínimo | Velocidad tuberías | Intervalo spawn |
|---|---|---|---|
| 1 | 0 | 0.62 | 1.50 s |
| 2 | 5 | 0.77 | 1.37 s |
| 3 | 10 | 0.92 | 1.24 s |
| 4 | 15 | 1.07 | 1.11 s |
| 5 (máx) | 20 | 1.22 | 0.98 s |

### Cómo se calcula
```java
private void calcularNivel() {
    int maxScore = Math.max(birds[0].score, birds[1].score);
    int nuevo = Math.min(maxScore / PUNTOS_POR_NIVEL + 1, NIVEL_MAX);
    // PUNTOS_POR_NIVEL = 5, NIVEL_MAX = 5
    velTuberias         = VEL_BASE + (nivelActual - 1) * VEL_PASO;
    tiempoEntreTuberias = SPAWN_BASE - (nivelActual - 1) * SPAWN_PASO;
}
```
- Usa el **máximo** de los dos puntajes (no la suma) para ser justo con el jugador que va perdiendo

### Dónde se muestra
- Barra de nivel en el HUD (verde → amarillo → naranja → rojo → púrpura)
- Título de la ventana: `"Flappy Bird | J1: 7 | J2: 4 || Nivel 2/5 (vel=0.77)"`

---

## 2.4 — Mejoras de interfaz

**Estado: ✅ Mayormente completo**

### Lo que tenemos

| Elemento | Implementado |
|---|---|
| Cielo degradado (4 franjas) | ✅ |
| Sol con rayos triangulares | ✅ |
| Montañas en 2 capas (lejanas/cercanas) | ✅ |
| Gorros de nieve en los picos | ✅ |
| Suelo en 3 capas (tierra + pasto) | ✅ |
| Nubes animadas que se mueven | ✅ |
| Tuberías con sombra, brillo y capuchón | ✅ |
| Pantalla de inicio con pájaros animados y botón pulsante | ✅ |
| Pantalla de game over con barras, corona al ganador | ✅ |
| HUD con iconos, bloques de puntaje, barra de nivel | ✅ |
| Franja lateral cuando un jugador muere | ✅ |
| Sonido (salto, punto, game over) | ❌ No implementado |
| Texturas/sprites desde imágenes | ❌ No usado (todo primitivas) |

### Lo que falta (menor peso en la nota)
- **Sonido**: podría agregarse con `javax.sound.sampled` o `AudioInputStream`
- El enunciado dice "algunos elementos sugeridos (**no excluyentes**)" — lo visual ya cubre el criterio

---

## Entregables — Estado

| Entregable | Estado | Qué hacer |
|---|---|---|
| Proyecto Java compilable con Maven | ✅ `mvn clean compile` | — |
| Código fuente comentado | ⚠️ Comentado pero en 1 sola clase | Opcional: separar en clases |
| Recursos incluidos (imágenes, sonidos) | ⚠️ No hay recursos externos | Todo es primitivas OpenGL |
| README con controles e instrucciones | ❌ Pendiente | Crear `README.md` |

### Nota sobre la clase única
El enunciado **recomienda** separar en `Bird`, `Pipe`, `Renderer`, etc., pero no lo exige como obligatorio en los 4 requerimientos. Las clases internas `Bird` y `Tuberia` ya dan algo de estructura. El riesgo es en el criterio **"Calidad del código: organización en clases"** (5%).

---

## Criterios de evaluación — Proyección

| Criterio | Peso | Estado actual |
|---|---|---|
| Pájaro con figuras + animación coherente | 5% | ✅ Listo |
| Dos jugadores con controles independientes | 10% | ✅ Listo |
| Velocidad/dificultad progresiva visible | 5% | ✅ Listo |
| Interfaz: fondo, HUD, pantallas inicio/game over | 5% | ✅ Listo (sin sonido) |
| Calidad del código: clases, nombres, comentarios | 5% | ⚠️ Una sola clase |
| **Modificación en vivo el día del examen** | **35%** | Depende de la comprensión |
| **Explicación del estudiante** | **35%** | Depende de la comprensión |
| **TOTAL** | **100%** | |

> **El 70% de la nota es la defensa oral.** La solución vale el 30% restante.

---

## Controles del juego

| Acción | Jugador 1 | Jugador 2 |
|---|---|---|
| Saltar | ESPACIO | W o FLECHA ARRIBA |
| Reiniciar (game over) | ESPACIO o R | W o FLECHA ARRIBA |
| Salir | ESC | ESC |

---

## Comando para ejecutar

```bash
mvn clean compile
mvn exec:exec -DmainClass=com.graphics.AppFlappyBird
```

---

## Cosas pendientes antes del 16 de mayo

- [ ] Crear `README.md` con controles, instrucciones y descripción de cambios
- [ ] (Opcional) Agregar sonido con `javax.sound.sampled`
- [ ] (Opcional) Separar en clases para mejor nota de calidad de código
- [ ] Estudiar el código línea a línea para poder modificarlo en vivo
- [ ] Probar compilación en otra máquina

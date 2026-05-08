# Flappy Bird — OpenGL / LWJGL (Java)

**Asignatura:** Programacion Grafica  
**Parcial:** Primer Examen Parcial  
**Integrante:** Fernando  
**Fecha:** Mayo 2026

---

## Controles

| Accion | Jugador 1 | Jugador 2 |
|---|---|---|
| Saltar | `ESPACIO` | `W` o `FLECHA ARRIBA` |
| Reiniciar (tras game over) | `ESPACIO` o `R` | `W` o `FLECHA ARRIBA` |
| Salir | `ESC` | `ESC` |

---

## Instrucciones de compilacion y ejecucion

### Requisitos
- Java 17 o superior
- Maven 3.6 o superior
- Windows 10 o superior

### Compilar
```bash
mvn clean compile
```

### Ejecutar
```bash
mvn exec:exec -DmainClass=com.graphics.AppFlappyBird
```

### Compilar y ejecutar en un solo comando
```bash
mvn clean compile exec:exec -DmainClass=com.graphics.AppFlappyBird
```

---

## Descripcion de los cambios realizados

El proyecto parte de la clase base `AppFlappyBird.java` entregada por la catedra. Se realizaron las siguientes modificaciones:

### 1. Pajaro compuesto por figuras geometricas

El pajaro original era un simple rectangulo. Fue reemplazado por un personaje construido con cinco partes dibujadas con primitivas OpenGL:

- **Cuerpo**: rectangulo principal con el color del jugador
- **Pico**: triangulo apuntando hacia adelante, color naranja
- **Ala**: rectangulo animado que aletea en bucle segun la velocidad del pajaro
- **Cola**: triangulo pequeno en la parte trasera
- **Ojo + pupila**: dos rectangulos concentricos (blanco y negro)

El pajaro se inclina segun su velocidad vertical (`tilt = velY * 0.28`). Todas las partes rotan juntas usando la funcion `ro(lx, ly, tilt)` que aplica rotacion 2D antes de posicionar cada pieza. La animacion de aleteo usa `Math.sin(wingAngle)` y se acelera a mayor velocidad vertical.

### 2. Modo de dos jugadores simultaneos

Se agrego una clase interna `Bird` que encapsula todo el estado de un jugador: posicion, velocidad, puntaje, si esta vivo, angulo del ala, teclas de salto y color. El juego mantiene un arreglo `birds[]` con dos instancias.

- **Jugador 1**: pajaro amarillo, salta con `ESPACIO`
- **Jugador 2**: pajaro cyan, salta con `W` o `FLECHA ARRIBA`

Las tuberias son compartidas. Mientras al menos un jugador este vivo la partida continua. Cuando un jugador muere su pajaro sigue cayendo y aparece una franja lateral de su color en el borde de la pantalla. El juego termina cuando ambos han chocado.

### 3. Incremento progresivo de la velocidad

Se definen 5 niveles de dificultad. El nivel sube cada 5 puntos (usando el puntaje maximo entre los dos jugadores):

| Nivel | Velocidad tuberias | Intervalo de aparicion |
|---|---|---|
| 1 | 0.62 | 1.50 s |
| 2 | 0.77 | 1.37 s |
| 3 | 0.92 | 1.24 s |
| 4 | 1.07 | 1.11 s |
| 5 | 1.22 | 0.98 s |

El nivel actual se muestra en el titulo de la ventana y en el HUD mediante una barra de progreso con colores (verde → amarillo → naranja → rojo → purpura).

### 4. Mejoras de interfaz

Se reemplazaron los fondos y paneles planos por una escena completa dibujada con primitivas OpenGL:

- **Fondo**: cielo con degradado en cuatro franjas azules
- **Sol**: aproximado con quads solapados y rayos triangulares
- **Montanas**: dos capas (lejana mas clara, cercana mas oscura) con gorros de nieve
- **Suelo**: tres franjas (tierra, linea de horizonte, pasto)
- **Nubes**: cinco nubes con sombra que se desplazan de derecha a izquierda en bucle
- **Tuberias**: con sombra lateral, brillo y capuchon en la boca
- **HUD**: barra superior con iconos de pajaro, bloques de puntaje individuales y barra de nivel
- **Pantalla de inicio**: panel con borde, pajaros animados que flotan y boton pulsante
- **Pantalla de game over**: barras de puntaje, corona triangular para el ganador y barra del nivel alcanzado

---

## Estructura del proyecto

```
opengl-java-class/
├── pom.xml                          # Dependencias LWJGL (Windows)
├── README.md                        # Este archivo
├── PARCIAL.md                       # Enunciado del parcial
├── RESUMEN_PARCIAL.md               # Checklist de requerimientos
├── DEFENSA.md                       # Guia de preparacion para la defensa oral
├── GUIA.md                          # Explicacion de todos los archivos del proyecto
└── src/main/java/com/graphics/
    ├── AppFlappyBird.java           # Juego Flappy Bird (este parcial)
    ├── AppLaberinto.java
    ├── AppTriangle.java
    └── ...
```

---

## Tecnologias utilizadas

- **Java 17**
- **LWJGL 3** — Lightweight Java Game Library
- **OpenGL 3.3 Core Profile**
- **GLFW** — ventana y entrada de teclado
- **Maven** — sistema de build

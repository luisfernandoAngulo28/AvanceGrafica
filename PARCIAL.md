# PRIMER EXAMEN PARCIAL

**Asignatura:** Programacion Grafica
**Proyecto base:** Flappy Bird en OpenGL (LWJGL / Java)

| Campo | Detalle |
|---|---|
| Modalidad | Examen practico individual con defensa oral del codigo |
| Fecha del parcial | 16 de mayo de 2026 |
| Entrega previa | Codigo fuente compilable subido al repositorio antes del examen |
| Lenguaje / API | Java + LWJGL (OpenGL 3.3 core profile) |
| Tipo de evaluacion | Solucion + defensa del codigo + explicacion del estudiante |

---

## 1. Contexto del proyecto

Como base para el primer parcial se entrega la clase `AppFlappyBird.java`, una implementacion minima de un juego estilo Flappy Bird construida con LWJGL y OpenGL en perfil core 3.3. La aplicacion dibuja el escenario unicamente con un "quad base" reutilizado mediante uniforms de offset, escala y color, e incluye fisica de gravedad, salto, generacion de tuberias, deteccion de colisiones AABB y conteo de puntaje.

Sobre esta base el estudiante debera realizar un conjunto de modificaciones que demuestren su comprension del pipeline grafico, del manejo de estado del juego, de la entrada por teclado y de la organizacion del codigo en componentes reutilizables.

---

## 2. Requerimientos obligatorios

El proyecto entregado debe cumplir, como minimo, con los cuatro requerimientos siguientes. Cada uno sera revisado durante la defensa y se solicitaran modificaciones en vivo.

### 2.1 Pajaro compuesto por figuras geometricas

El pajaro actual es un simple rectangulo. El estudiante debera reemplazarlo por un personaje construido a partir de varias figuras geometricas dibujadas con OpenGL (rectangulos, triangulos, circulos aproximados con triangle fan, polilineas, etc.). El pajaro debera tener al menos:

- Cuerpo principal.
- Pico (figura distinguible, ej. triangulo).
- Al menos un ala visible (puede animarse en bucle).
- Cola.
- Ojo (con pupila o detalle interno).

La composicion debe mantenerse coherente cuando el pajaro sube, baja o se inclina segun su velocidad vertical. Se valorara que la animacion de aleteo este sincronizada con el salto.

### 2.2 Modo de dos jugadores simultaneos

El juego debera soportar dos jugadores jugando al mismo tiempo en la misma ventana, cada uno con su propio pajaro y sus propios controles independientes. Como referencia se sugiere:

- **Jugador 1:** tecla `ESPACIO` para saltar.
- **Jugador 2:** tecla `W` (o flecha `ARRIBA`) para saltar.

Cada pajaro debe tener su propia posicion, velocidad, estado de vivo/muerto y puntaje individual. Las tuberias son compartidas. El juego termina cuando ambos pajaros han chocado; mientras al menos uno siga vivo, la partida continua. El puntaje de cada jugador debe poder distinguirse visualmente (por color del pajaro, etiqueta o panel lateral).

### 2.3 Incremento progresivo de la velocidad

La dificultad debera aumentar de manera progresiva conforme los jugadores acumulan puntos. El estudiante debe modificar la velocidad de las tuberias y/o la frecuencia de aparicion (`TIEMPO_ENTRE_TUBERIAS`, `VELOCIDAD_TUBERIAS`) en funcion del puntaje alcanzado. Como minimo:

- Definir niveles o un crecimiento continuo claramente perceptible.
- Aplicar un limite superior razonable para que el juego siga siendo jugable.
- Reflejar el nivel/velocidad actual en la interfaz (titulo de la ventana o HUD).

### 2.4 Mejora de la interfaz del juego

La presentacion debera mejorarse de forma evidente respecto a la version base. Algunos elementos sugeridos (no excluyentes) son:

- Fondo con degradado, nubes, montanas o suelo dibujados con primitivas o texturas.
- Texturas o sprites cargados desde imagenes para el pajaro, las tuberias o el fondo.
- Sonido para el salto, el punto anotado y el game over (puede usarse `javax.sound`, OpenAL u otra libreria).
- Pantalla de inicio, pantalla de game over y HUD con puntaje legible.
- Animaciones adicionales (parallax, particulas, parpadeo del pajaro al perder, etc.).

Se valorara la coherencia estetica y que las mejoras no rompan la jugabilidad ni el rendimiento.

---

## 3. Entregables

1. Proyecto Java compilable (Maven o Gradle) que ejecute el juego con un comando claro.
2. Codigo fuente comentado, organizado en clases coherentes (no todo en una sola clase).
3. Recursos utilizados (imagenes, sonidos, shaders) incluidos en el repositorio.
4. `README` breve con: integrantes, controles de cada jugador, instrucciones de compilacion y ejecucion, y una descripcion corta de los cambios realizados.

---

## 4. Examen y defensa — 16 de mayo

El examen del primer parcial y la revision del proyecto se realizaran el **16 de mayo**. Durante la sesion se solicitara a cada estudiante modificar el juego en vivo (por ejemplo: cambiar un control, alterar la fisica, agregar una figura al pajaro, modificar la curva de dificultad o ajustar un shader) y posteriormente realizar una defensa del codigo entregado.

La defensa consistira en:

- Explicar la estructura general del proyecto y el flujo del bucle principal.
- Justificar las decisiones de diseno tomadas en cada uno de los cuatro requerimientos.
- Responder preguntas puntuales sobre fragmentos de codigo elegidos por el docente.
- Aplicar la modificacion solicitada en vivo y demostrar el resultado en ejecucion.

---

## 5. Criterios de evaluacion

| Criterio | Peso | Categoria |
|---|---|---|
| Pajaro construido con varias figuras geometricas (pico, alas, cola, ojo) y animacion coherente | 5% | Solucion |
| Modo de dos jugadores con controles independientes y manejo correcto del estado de cada pajaro | 10% | Solucion |
| Incremento progresivo y visible de la velocidad/dificultad segun el puntaje | 5% | Solucion |
| Mejora de la interfaz: fondo, sonido, imagenes, HUD, pantallas de inicio y game over | 5% | Solucion |
| Calidad del codigo: organizacion en clases, nombres claros, comentarios y reutilizacion | 5% | Solucion |
| Modificacion en vivo solicitada el dia del examen (correcta y funcional) | 35% | Defensa |
| Explicacion del estudiante: claridad, dominio del codigo y respuesta a preguntas | 35% | Defensa |
| **TOTAL** | **100%** | |

---

## 6. Reglas de honestidad academica

- El proyecto es **individual**. No se permite compartir codigo entre estudiantes.
- Si el estudiante no logra explicar partes sustanciales de su propio codigo durante la defensa, la nota correspondiente a ese requerimiento se anula.
- Se permite el uso de documentacion oficial de LWJGL/OpenGL y de bibliotecas estandar de Java.
- El uso de codigo generado por terceros (incluido IA) debe ser comprendido a profundidad; sera evaluado por la capacidad de modificarlo en vivo.

---

## 7. Recomendaciones

Se recomienda separar el proyecto en clases (por ejemplo: `Bird`, `Pipe`, `Renderer`, `InputManager`, `Game`). Esto facilita la introduccion del segundo jugador y la lectura del codigo durante la defensa. Tambien se recomienda probar la compilacion del proyecto en una maquina diferente antes del 16 de mayo para evitar fallos de ultimo momento.

**Exitos en el desarrollo del parcial.**

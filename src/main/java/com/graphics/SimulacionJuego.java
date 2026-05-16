package com.graphics;

public class SimulacionJuego {

    static final int ANCHO_PANTALLA = 40;
    static final int ALTO_PANTALLA = 12;

    public static void main(String[] args) throws InterruptedException {

        
        // CREAR PAJARO    
        Bird bird = new Bird(
                0.12f,
                0.97f, 0.82f, 0.15f,
                "J1",
                32
        );
  
        // CREAR TUBERIA        
        Tuberia tuberia = new Tuberia(
                1.2f,
                0.10f,
                0.30f
        );

        System.out.println("=== INICIO DEL JUEGO ===");

        // ==========================================
        // CICLO DEL JUEGO
        // ==========================================

        for (int frame = 1; frame <= 20; frame++) {

            // -----------------------------
            // MOVER TUBERIA
            // -----------------------------
            tuberia.x -= 0.1f;

            // -----------------------------
            // GRAVEDAD
            // -----------------------------
            bird.velocidadY -= 0.05f;
            bird.y += bird.velocidadY;

            // -----------------------------
            // SALTO EN FRAME 5
            // -----------------------------
            if (frame == 5) {
                bird.velocidadY = 0.20f;
                System.out.println("\n🐦 EL PAJARO SALTA!");
            }

            // -----------------------------
            // MOSTRAR ESTADO
            // -----------------------------
            System.out.println("\n=================================");
            System.out.println("FRAME " + frame);
            System.out.println("=================================");

            System.out.println(bird);
            System.out.println(tuberia);

            // Dibujar mundo
            dibujarPantalla(bird, tuberia);

            // -----------------------------
            // COLISION
            // -----------------------------
            if (colisiona(tuberia, bird)) {

                bird.matar();

                System.out.println("\n💀 EL PAJARO CHOCO!");
                break;
            }

            Thread.sleep(700);
        }

        System.out.println("\n=== FIN DE LA SIMULACION ===");
    }

    // ==================================================
    // DIBUJAR PANTALLA ASCII
    // ==================================================

    static void dibujarPantalla(Bird bird, Tuberia tuberia) {

        String[][] pantalla = new String[ALTO_PANTALLA][ANCHO_PANTALLA];

        // llenar espacio vacío
        for (int fila = 0; fila < ALTO_PANTALLA; fila++) {
            for (int col = 0; col < ANCHO_PANTALLA; col++) {
                pantalla[fila][col] = " ";
            }
        }

        // ==========================================
        // POSICION DEL PAJARO
        // ==========================================

        int birdX = 8;

        int birdY = convertirY(bird.y);

        if (birdY >= 0 && birdY < ALTO_PANTALLA) {
            pantalla[birdY][birdX] = "@>";
        }

        // ==========================================
        // POSICION DE LA TUBERIA
        // ==========================================

        int pipeX = convertirX(tuberia.x);

        int gapTop = convertirY(
                tuberia.centroHuecoY +
                        tuberia.alturaHueco / 2
        );

        int gapBottom = convertirY(
                tuberia.centroHuecoY -
                        tuberia.alturaHueco / 2
        );

        for (int fila = 0; fila < ALTO_PANTALLA; fila++) {

            boolean estaEnHueco =
                    fila >= gapTop &&
                            fila <= gapBottom;

            if (!estaEnHueco) {

                if (pipeX >= 0 &&
                        pipeX < ANCHO_PANTALLA) {

                    pantalla[fila][pipeX] = "|";
                    pantalla[fila][pipeX + 1] = "|";
                }
            }
        }

        // ==========================================
        // IMPRIMIR PANTALLA
        // ==========================================

        for (int fila = 0; fila < ALTO_PANTALLA; fila++) {

            System.out.print("|");

            for (int col = 0; col < ANCHO_PANTALLA; col++) {
                System.out.print(pantalla[fila][col]);
            }

            System.out.println("|");
        }
    }

    // ==================================================
    // CONVERTIR COORDENADAS OPENGL → CONSOLA
    // ==================================================

    static int convertirX(float x) {

        return (int) ((x + 1.2f) / 2.4f * ANCHO_PANTALLA);
    }

    static int convertirY(float y) {

        return (int) ((1 - (y + 1) / 2) * ALTO_PANTALLA);
    }

    // ==================================================
    // COLISION
    // ==================================================

    static boolean colisiona(Tuberia t, Bird b) {

        float birdTop = b.y + 0.05f;
        float birdBottom = b.y - 0.05f;

        float gapTop =
                t.centroHuecoY +
                        t.alturaHueco / 2;

        float gapBottom =
                t.centroHuecoY -
                        t.alturaHueco / 2;

        boolean tuberiaCerca =
                t.x < 0.2f;

        boolean chocoArriba =
                birdTop > gapTop;

        boolean chocoAbajo =
                birdBottom < gapBottom;

        return tuberiaCerca &&
                (chocoArriba || chocoAbajo);
    }
}
/*
=== INICIO DEL JUEGO ===

=================================
FRAME 1
=================================
Bird{nombre='J1', y=0.06999999, velocidadY=-0.05, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=1.1, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                                      |||
|                                      |||
|                                      |||
|                                      |||
|                                        |
|        @>                               |
|                                        |
|                                      |||
|                                      |||
|                                      |||
|                                      |||
|                                      |||

=================================
FRAME 2
=================================
Bird{nombre='J1', y=-0.030000009, velocidadY=-0.1, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=1.0, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                                    ||  |
|                                    ||  |
|                                    ||  |
|                                    ||  |
|                                        |
|                                        |
|        @>                               |
|                                    ||  |
|                                    ||  |
|                                    ||  |
|                                    ||  |
|                                    ||  |

=================================
FRAME 3
=================================
Bird{nombre='J1', y=-0.18, velocidadY=-0.15, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.9, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                                  ||    |
|                                  ||    |
|                                  ||    |
|                                  ||    |
|                                        |
|                                        |
|                                        |
|        @>                         ||    |
|                                  ||    |
|                                  ||    |
|                                  ||    |
|                                  ||    |

=================================
FRAME 4
=================================
Bird{nombre='J1', y=-0.38, velocidadY=-0.2, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.79999995, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                                 ||     |
|                                 ||     |
|                                 ||     |
|                                 ||     |
|                                        |
|                                        |
|                                        |
|                                 ||     |
|        @>                        ||     |
|                                 ||     |
|                                 ||     |
|                                 ||     |

? EL PAJARO SALTA!

=================================
FRAME 5
=================================
Bird{nombre='J1', y=-0.63, velocidadY=0.2, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.6999999, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                               ||       |
|                               ||       |
|                               ||       |
|                               ||       |
|                                        |
|                                        |
|                                        |
|                               ||       |
|                               ||       |
|        @>                      ||       |
|                               ||       |
|                               ||       |

=================================
FRAME 6
=================================
Bird{nombre='J1', y=-0.48, velocidadY=0.15, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.5999999, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                             ||         |
|                             ||         |
|                             ||         |
|                             ||         |
|                                        |
|                                        |
|                                        |
|                             ||         |
|        @>                    ||         |
|                             ||         |
|                             ||         |
|                             ||         |

=================================
FRAME 7
=================================
Bird{nombre='J1', y=-0.38, velocidadY=0.10000001, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.4999999, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                            ||          |
|                            ||          |
|                            ||          |
|                            ||          |
|                                        |
|                                        |
|                                        |
|                            ||          |
|        @>                   ||          |
|                            ||          |
|                            ||          |
|                            ||          |

=================================
FRAME 8
=================================
Bird{nombre='J1', y=-0.32999998, velocidadY=0.05000001, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.39999992, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                          ||            |
|                          ||            |
|                          ||            |
|                          ||            |
|                                        |
|                                        |
|                                        |
|        @>                 ||            |
|                          ||            |
|                          ||            |
|                          ||            |
|                          ||            |

=================================
FRAME 9
=================================
Bird{nombre='J1', y=-0.32999998, velocidadY=7.4505806E-9, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.29999992, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                         ||             |
|                         ||             |
|                         ||             |
|                         ||             |
|                                        |
|                                        |
|                                        |
|        @>                ||             |
|                         ||             |
|                         ||             |
|                         ||             |
|                         ||             |

=================================
FRAME 10
=================================
Bird{nombre='J1', y=-0.37999997, velocidadY=-0.049999993, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.19999993, centroHuecoY=0.1, alturaHueco=0.3, puntoContabilizado=false}
|                       ||               |
|                       ||               |
|                       ||               |
|                       ||               |
|                                        |
|                                        |
|                                        |
|                       ||               |
|        @>              ||               |
|                       ||               |
|                       ||               |
|                       ||               |

? EL PAJARO CHOCO!

=== FIN DE LA SIMULACION ===
PS D:\Universidad\Programacion Grafica\opengl-java-class> 
 */
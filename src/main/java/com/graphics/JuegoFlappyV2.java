package com.graphics;

import java.util.ArrayList;
import java.util.Random;

public class JuegoFlappyV2 {

    static final int ANCHO_PANTALLA = 50;
    static final int ALTO_PANTALLA = 12;

    static Random random = new Random();

    static Bird bird;
    static ArrayList<Tuberia> tuberias;

    static int frame = 1;

    public static void main(String[] args)
            throws InterruptedException {

        iniciarJuego();

        while (bird.alive) {

            mostrarFrame();

            actualizarFisica();

            procesarSalto();

            moverTuberias();

            actualizarScore();

            revisarColisiones();

            dibujarPantalla();

            Thread.sleep(500);

            frame++;
        }

        System.out.println("\n💀 GAME OVER");
    }

    // ======================================
    // INICIAR JUEGO
    // ======================================

    static void iniciarJuego() {

        bird = new Bird(
                0.0f,
                1f, 1f, 0f,
                "J1",
                32
        );

        tuberias =
                new ArrayList<>();

        crearTuberia(1.2f);
        crearTuberia(2.0f);
        crearTuberia(2.8f);

        System.out.println(
                "=== INICIO DEL JUEGO ==="
        );
    }

    // ======================================
    // MOSTRAR FRAME
    // ======================================

    static void mostrarFrame() {

        System.out.println("\n==================");
        System.out.println(
                "FRAME " + frame
        );
        System.out.println("==================");
    }

    // ======================================
    // FISICA
    // ======================================

    static void actualizarFisica() {

        bird.velocidadY -= 0.05f;

        bird.y += bird.velocidadY;
    }

    // ======================================
    // SALTO
    // ======================================

    static void procesarSalto() {

        if (frame == 5 ||
                frame == 15 ||
                frame == 22) {

            bird.velocidadY = 0.25f;

            System.out.println(
                    "🐦 SALTO!"
            );
        }
    }

    // ======================================
    // TUBERIAS
    // ======================================

    static void moverTuberias() {

        for (Tuberia t : tuberias) {

            t.x -= 0.08f;
        }

        // eliminar si sale de pantalla
        if (tuberias.get(0).x < -1.5f) {

            tuberias.remove(0);

            crearTuberia(2.5f);
        }
    }

    // ======================================
    // SCORE
    // ======================================

    static void actualizarScore() {

        for (Tuberia t : tuberias) {

            if (!t.puntoContabilizado
                    && t.x < 0f) {

                bird.score++;

                t.puntoContabilizado =
                        true;

                System.out.println(
                        "⭐ +1 PUNTO"
                );
            }
        }
    }

    // ======================================
    // COLISIONES
    // ======================================

    static void revisarColisiones() {

        for (Tuberia t : tuberias) {

            if (colisiona(t)) {

                bird.matar();

                break;
            }
        }
    }

    // ======================================
    // CREAR TUBERIA
    // ======================================

    static void crearTuberia(
            float x
    ) {

        float centroRandom =
                -0.5f
                        + random.nextFloat();

        tuberias.add(
                new Tuberia(
                        x,
                        centroRandom,
                        0.35f
                )
        );
    }

    // ======================================
    // COLISION
    // ======================================

    static boolean colisiona(
            Tuberia t
    ) {

        float birdTop =
                bird.y + 0.05f;

        float birdBottom =
                bird.y - 0.05f;

        float gapTop =
                t.centroHuecoY
                        + t.alturaHueco / 2;

        float gapBottom =
                t.centroHuecoY
                        - t.alturaHueco / 2;

        boolean cerca =
                Math.abs(t.x)
                        < 0.1f;

        boolean arriba =
                birdTop > gapTop;

        boolean abajo =
                birdBottom
                        < gapBottom;

        return cerca
                && (arriba || abajo);
    }

    // ======================================
    // DIBUJAR
    // ======================================

    static void dibujarPantalla() {

        System.out.println(bird);

        for (Tuberia t : tuberias) {

            System.out.println(t);
        }

        System.out.println(
                "PUNTAJE: "
                        + bird.score
        );
    }
}
/*=== INICIO DEL JUEGO ===

==================
FRAME 1
==================
Bird{nombre='J1', y=-0.05, velocidadY=-0.05, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=1.12, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.92, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.72, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 2
==================
Bird{nombre='J1', y=-0.15, velocidadY=-0.1, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=1.04, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.8399999, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.64, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 3
==================
Bird{nombre='J1', y=-0.3, velocidadY=-0.15, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.96, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.7599999, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.5600002, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 4
==================
Bird{nombre='J1', y=-0.5, velocidadY=-0.2, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.88, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.6799998, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.4800003, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 5
==================
? SALTO!
Bird{nombre='J1', y=-0.75, velocidadY=0.25, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.8, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.5999998, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.4000003, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 6
==================
Bird{nombre='J1', y=-0.55, velocidadY=0.2, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.72, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.5199997, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.3200004, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 7
==================
Bird{nombre='J1', y=-0.4, velocidadY=0.15, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.64000005, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.4399997, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.2400005, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 8
==================
Bird{nombre='J1', y=-0.3, velocidadY=0.10000001, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.56000006, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.3599997, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.1600006, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 9
==================
Bird{nombre='J1', y=-0.25, velocidadY=0.05000001, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.48000008, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.2799996, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.0800006, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 10
==================
Bird{nombre='J1', y=-0.25, velocidadY=7.4505806E-9, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.4000001, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.1999996, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=2.0000007, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 11
==================
Bird{nombre='J1', y=-0.29999998, velocidadY=-0.049999993, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.3200001, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.1199995, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.9200007, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 12
==================
Bird{nombre='J1', y=-0.39999998, velocidadY=-0.099999994, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.24000011, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.0399995, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.8400006, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 13
==================
Bird{nombre='J1', y=-0.54999995, velocidadY=-0.14999999, score=0, alive=true, tiempoFlash=0.0}
Tuberia{x=0.16000012, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=0.9599995, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.7600006, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

==================
FRAME 14
==================
Bird{nombre='J1', y=-0.74999994, velocidadY=-0.19999999, score=0, alive=false, tiempoFlash=0.5}
Tuberia{x=0.08000012, centroHuecoY=-0.062809885, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=0.8799995, centroHuecoY=0.023805678, alturaHueco=0.35, puntoContabilizado=false}
Tuberia{x=1.6800005, centroHuecoY=0.20629603, alturaHueco=0.35, puntoContabilizado=false}
PUNTAJE: 0

? GAME OVER
PS D:\Universidad\Programacion Grafica\opengl-java-class>  */
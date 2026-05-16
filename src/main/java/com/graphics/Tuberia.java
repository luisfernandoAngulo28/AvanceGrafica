package com.graphics;

/**
 * Representa un par de tuberias (superior e inferior) con su hueco central.
 * Cada tuberia guarda su propio alturaHueco para que las tuberias en pantalla
 * no cambien de tamano cuando el nivel sube.
 */
class Tuberia {

    float   x;           // posicion horizontal actual
    float   centroHuecoY;  // centro vertical del hueco
    float   alturaHueco;     // altura del hueco (se fija al momento del spawn)
    boolean puntoContabilizado;    // true cuando el pajaro ya paso y cobro el punto

    Tuberia(float x, float centroHuecoY, float alturaHueco) {
        this.x = x;
        this.centroHuecoY = centroHuecoY;
        this.alturaHueco = alturaHueco;
    }
    @Override
    public String toString() {
        return "Tuberia{" +
                "x=" + x +
                ", centroHuecoY=" + centroHuecoY +
                ", alturaHueco=" + alturaHueco +
                ", puntoContabilizado=" + puntoContabilizado +
                '}';
    }
    public static void main(String[] args) {
        Tuberia tuberia = new Tuberia(1.2f, 0.5f, 0.3f);
        System.out.println(tuberia);
        //Tuberia{x=1.2, centroHuecoY=0.5, alturaHueco=0.3, puntoContabilizado=false}
    }
    /*
        Pantalla del juego
    
            TUBERIA SUPERIOR
            ███████████
            ███████████
            ███████████
    
                HUECO
                (0.3)
    
    ---------------------- centroHuecoY = 0.5
    
            ███████████
            ███████████
            ███████████
            TUBERIA INFERIOR
    
    x = 1.2 → aparece lejos a la derecha
     */
}

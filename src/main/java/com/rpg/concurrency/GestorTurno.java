package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Administra la creacion, arranque y cancelacion del hilo temporizador de turno.
 */
public class GestorTurno {

    private final Thread hilo;

    public GestorTurno(int duracionSegundos, TurnoCallback callback) {
        this.hilo = new Thread(new TemporizadorTurnoTask(duracionSegundos, callback), "Temporizador-Turno");
    }

    public void iniciar() {
        hilo.start();
    }

    public void cancelar() {
        if (hilo != null && hilo.isAlive()) {
            hilo.interrupt();
        }
    }

    public boolean estaVivo() {
        return hilo != null && hilo.isAlive();
    }
}

package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/** Cuenta regresiva de turno en segundo plano. */
public class TemporizadorTurnoTask implements Runnable {

    private final int duracionSegundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int duracionSegundos, TurnoCallback callback) {
        if (duracionSegundos <= 0) {
            throw new IllegalArgumentException("La duracion debe ser positiva");
        }
        this.duracionSegundos = duracionSegundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        for (int i = duracionSegundos; i > 0; i--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (callback != null) {
            callback.onTiempoAgotado();
        }
    }
}

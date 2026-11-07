package com.rpg.concurrency;

/**
 * Hilo demonio que ejecuta tareas periodicas de guardado en segundo plano.
 */
public class AutoGuardadoDaemon implements Runnable {

    private final Runnable accionGuardado;
    private final int intervaloMs;
    private Thread hilo;

    public AutoGuardadoDaemon(Runnable accionGuardado, int intervaloMs) {
        if (accionGuardado == null) {
            throw new IllegalArgumentException("La accion de guardado no puede ser nula");
        }
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.accionGuardado = accionGuardado;
        this.intervaloMs = intervaloMs;
    }

    public synchronized void iniciar() {
        if (hilo != null && hilo.isAlive()) {
            return;
        }
        hilo = new Thread(this, "AutoGuardado-Daemon");
        hilo.setDaemon(true);
        hilo.start();
    }

    public synchronized void detener() {
        if (hilo != null && hilo.isAlive()) {
            hilo.interrupt();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
                accionGuardado.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[AutoGuardado] Error en respaldo: " + e.getMessage());
            }
        }
    }

    public boolean esDaemon() {
        return hilo != null && hilo.isDaemon();
    }

    public boolean estaVivo() {
        return hilo != null && hilo.isAlive();
    }
}

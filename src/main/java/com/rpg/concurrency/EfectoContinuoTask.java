package com.rpg.concurrency;

import com.rpg.core.FormatoCombate;
import com.rpg.core.RegistroCombate;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

/** Tarea para aplicar efectos periodicos en su propio hilo. */
public class EfectoContinuoTask implements Runnable {

    private final Personaje objetivo;
    private final String nombreEfecto;
    private final boolean esDanio;
    private final int valorPorTick;
    private int ticksRestantes;
    private final int intervaloMs;
    private final RegistroCombate registro;

    public EfectoContinuoTask(Personaje objetivo, EstadoEfecto efecto, int intervaloMs, RegistroCombate registro) {
        this.objetivo = objetivo;
        this.nombreEfecto = efecto.getNombre();
        this.esDanio = efecto.esDanio();
        this.valorPorTick = efecto.getValorPorTick();
        this.ticksRestantes = efecto.getTurnosRestantes();
        this.intervaloMs = intervaloMs;
        this.registro = registro;
    }

    public EfectoContinuoTask(Personaje objetivo, String nombreEfecto, boolean esDanio, int valorPorTick,
                              int ticksRestantes, int intervaloMs, RegistroCombate registro) {
        this.objetivo = objetivo;
        this.nombreEfecto = nombreEfecto;
        this.esDanio = esDanio;
        this.valorPorTick = valorPorTick;
        this.ticksRestantes = ticksRestantes;
        this.intervaloMs = intervaloMs;
        this.registro = registro;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && ticksRestantes > 0 && objetivo.estaVivo()) {
                Thread.sleep(intervaloMs);
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (esDanio) {
                    objetivo.recibirDanio(valorPorTick);
                    if (registro != null) {
                        registro.agregar("[Efecto continuo] %s sufre %d de dano por %s (%s)",
                                objetivo.getNombre(), valorPorTick, nombreEfecto, FormatoCombate.resumen(objetivo));
                    }
                } else {
                    int curado = objetivo.curar(valorPorTick);
                    if (registro != null) {
                        registro.agregar("[Efecto continuo] %s recupera %d de vida por %s (%s)",
                                objetivo.getNombre(), curado, nombreEfecto, FormatoCombate.resumen(objetivo));
                    }
                }
                ticksRestantes--;
            }
        } catch (InterruptedException e) {
            objetivo.quitarEfecto(nombreEfecto);
            Thread.currentThread().interrupt();
        } finally {
            if (ticksRestantes <= 0) {
                objetivo.quitarEfecto(nombreEfecto);
            }
        }
    }

    public String getNombreEfecto() {
        return nombreEfecto;
    }

    public int getIntervaloMs() {
        return intervaloMs;
    }
}

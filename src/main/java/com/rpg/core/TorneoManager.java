package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        Personaje[] finalistas = new Personaje[2];

        Thread hiloSemi1 = new Thread(() -> {
            finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1));
        }, "Semifinal-1");

        Thread hiloSemi2 = new Thread(() -> {
            finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3));
        }, "Semifinal-2");

        System.out.printf("[Ciclo de vida] Hilo Semifinal 1 antes de start(): %s%n", hiloSemi1.getState());
        System.out.printf("[Ciclo de vida] Hilo Semifinal 2 antes de start(): %s%n", hiloSemi2.getState());

        hiloSemi1.start();
        hiloSemi2.start();

        System.out.printf("[Ciclo de vida] Hilo Semifinal 1 en ejecucion: %s%n", hiloSemi1.getState());
        System.out.printf("[Ciclo de vida] Hilo Semifinal 2 en ejecucion: %s%n", hiloSemi2.getState());

        try {
            hiloSemi1.join();
            hiloSemi2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Torneo interrumpido mientras se esperaba a las semifinales.");
        }

        System.out.printf("[Ciclo de vida] Hilo Semifinal 1 despues de join(): %s%n", hiloSemi1.getState());
        System.out.printf("[Ciclo de vida] Hilo Semifinal 2 despues de join(): %s%n", hiloSemi2.getState());

        return simularFinal(finalistas[0], finalistas[1]);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}

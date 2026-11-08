package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Interfaz de usuario por consola en la arquitectura MVC.
 * Gestiona toda la interaccion con el usuario sin sentencias SQL.
 */
public class HeroeUI {

    private final HeroeController controller;
    private final Consola consola;

    public HeroeUI(HeroeController controller) {
        this(controller, new Scanner(System.in));
    }

    public HeroeUI(HeroeController controller, Scanner scanner) {
        this.controller = controller;
        this.consola = new Consola(scanner);
    }

    /**
     * Bucle principal de la vista con el menu interactivo.
     */
    public void iniciar() {
        int opcion = -1;

        try {
            while (opcion != 0) {
                mostrarMenu();
                opcion = consola.leerEntero("Seleccione una opcion: ", 0, 6);

                switch (opcion) {
                    case 1 -> menuInsertar();
                    case 2 -> menuListar();
                    case 3 -> menuBuscarPorTipo();
                    case 4 -> menuActualizarNivel();
                    case 5 -> menuEliminar();
                    case 6 -> menuCargarYCombatir();
                    case 0 -> System.out.println("\nSaliendo del sistema de heroes. Hasta pronto.");
                    default -> System.out.println("Error: Opcion no valida. Intente nuevamente.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta pronto.");
        }
    }

    private void mostrarMenu() {
        System.out.println("\n=============================================");
        System.out.println("   REGISTRO DE HEROES - PERSISTENCIA (BD)   ");
        System.out.println("=============================================");
        System.out.println("1. Insertar heroe");
        System.out.println("2. Listar todos los heroes");
        System.out.println("3. Buscar heroes por tipo");
        System.out.println("4. Actualizar nivel de heroe");
        System.out.println("5. Eliminar heroe");
        System.out.println("6. Cargar heroe desde BD y jugar combate rapido");
        System.out.println("0. Salir");
        System.out.println("=============================================");
    }

    private void menuInsertar() {
        System.out.println("\n--- REGISTRAR NUEVO HEROE ---");
        String nombre = consola.leerLinea("Ingrese el nombre del heroe: ");
        String tipo = consola.leerLinea("Ingrese el tipo de heroe (guerrero o mago): ").toLowerCase();

        int vidaMaxima = 100;
        if (tipo.equals("guerrero")) {
            vidaMaxima = 120;
        } else if (tipo.equals("mago")) {
            vidaMaxima = 80;
        }

        String entradaNivel = consola.leerLinea("Ingrese el nivel inicial (por defecto 1): ");
        int nivel = 1;
        if (!entradaNivel.isEmpty()) {
            try {
                nivel = Integer.parseInt(entradaNivel);
            } catch (NumberFormatException e) {
                System.out.println("Error: Nivel invalido, se asignara nivel 1 por defecto.");
                nivel = 1;
            }
        }

        System.out.println("Vida maxima asignada para " + tipo + ": " + vidaMaxima + " puntos.");
        String respuesta = controller.registrarHeroe(nombre, tipo, vidaMaxima, nivel);
        System.out.println(respuesta);
    }

    private void menuListar() {
        System.out.println("\n--- LISTA GENERAL DE HEROES REGISTRADOS ---");
        List<Heroe> heroes = controller.listarHeroes();

        if (heroes.isEmpty()) {
            System.out.println("No hay heroes registrados actualmente en la base de datos.");
            return;
        }

        imprimirTablaHeroes(heroes);
    }

    private void menuBuscarPorTipo() {
        System.out.println("\n--- BUSCAR HEROES POR TIPO ---");
        String tipo = consola.leerLinea("Ingrese el tipo a filtrar (guerrero o mago): ");

        List<Heroe> heroes = controller.buscarPorTipo(tipo);
        if (heroes.isEmpty()) {
            System.out.println("No se encontraron heroes del tipo '" + tipo + "'.");
            return;
        }

        imprimirTablaHeroes(heroes);
    }

    private void menuActualizarNivel() {
        System.out.println("\n--- ACTUALIZAR NIVEL DE UN HEROE ---");
        int id = consola.leerEntero("Ingrese el ID del heroe: ", 1, Integer.MAX_VALUE);
        int nuevoNivel = consola.leerEntero("Ingrese el nuevo nivel (minimo 1): ", 1, Integer.MAX_VALUE);

        String respuesta = controller.actualizarNivel(id, nuevoNivel);
        System.out.println(respuesta);
    }

    private void menuEliminar() {
        System.out.println("\n--- ELIMINAR HEROE ---");
        int id = consola.leerEntero("Ingrese el ID del heroe a eliminar: ", 1, Integer.MAX_VALUE);

        String respuesta = controller.eliminarHeroe(id);
        System.out.println(respuesta);
    }

    private void menuCargarYCombatir() {
        System.out.println("\n--- CARGAR HEROE DESDE BD PARA COMBATE RAPIDO ---");
        List<Heroe> heroes = controller.listarHeroes();
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes en la base de datos para cargar.");
            return;
        }

        imprimirTablaHeroes(heroes);

        int id = consola.leerEntero("Ingrese el ID del heroe que desea usar: ", 1, Integer.MAX_VALUE);

        Heroe heroe = controller.buscarPorId(id);
        if (heroe == null) {
            System.out.println("no existe un heroe con ese id");
            return;
        }

        Personaje personaje = controller.crearPersonajeDesdeHeroe(heroe);
        if (personaje == null) {
            System.out.println("Error: No se pudo instanciar el combatiente a partir del tipo registrado.");
            return;
        }

        System.out.printf("%nHeroe cargado con exito: %s [%s] (Vida: %d, Nivel: %d)%n",
                heroe.getNombre(), heroe.getTipo(), heroe.getVidaMaxima(), heroe.getNivel());
        System.out.println("Iniciando arena de combate rapido...");

        MotorJuego motor = new MotorJuego(this.consola);
        motor.iniciarCombateRapidoConHeroe(personaje);
    }

    private void imprimirTablaHeroes(List<Heroe> heroes) {
        System.out.printf("%-6s | %-18s | %-12s | %-12s | %-6s%n",
                "ID", "NOMBRE", "TIPO", "VIDA MAXIMA", "NIVEL");
        System.out.println("-----------------------------------------------------------------");
        for (Heroe h : heroes) {
            System.out.printf("%-6d | %-18s | %-12s | %-12d | %-6d%n",
                    h.getId(), h.getNombre(), h.getTipo(), h.getVidaMaxima(), h.getNivel());
        }
        System.out.println("-----------------------------------------------------------------");
    }
}

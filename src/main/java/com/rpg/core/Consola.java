package com.rpg.core;

import com.rpg.concurrency.GestorTurno;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Entrada por teclado con validacion y temporizador. */
public class Consola {

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private final Scanner scanner;
    private final BlockingQueue<String> colaEntrada = new LinkedBlockingQueue<>();
    private final Thread hiloLector;

    public Consola(Scanner scanner) {
        this.scanner = scanner;
        this.hiloLector = new Thread(() -> {
            try {
                while (this.scanner.hasNextLine()) {
                    String linea = this.scanner.nextLine();
                    colaEntrada.offer(linea);
                }
            } catch (Exception ignored) {
            }
        }, "Consola-Lector-Continuo");
        this.hiloLector.setDaemon(true);
        this.hiloLector.start();
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        try {
            String linea = colaEntrada.take();
            return linea.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Lectura interrumpida");
        }
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    public Integer leerEnteroConTimeout(String mensaje, int minimo, int maximo, int segundos, GestorTurno gestor) {
        while (true) {
            System.out.print(mensaje);
            String texto;
            try {
                texto = colaEntrada.poll(segundos, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (gestor != null) {
                    gestor.cancelar();
                }
                return null;
            }

            if (texto == null) {
                if (gestor != null) {
                    gestor.cancelar();
                }
                return null;
            }

            if (gestor != null) {
                gestor.cancelar();
            }

            texto = texto.trim();
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }
}

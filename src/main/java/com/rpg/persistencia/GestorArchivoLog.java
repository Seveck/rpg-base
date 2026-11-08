package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Gestor del historial de combate en formato de texto plano. */
public class GestorArchivoLog {

    private static final String CARPETA_DEFECTO = "partidas";
    private static final String ARCHIVO_DEFECTO = "partidas/combates.txt";

    private final File archivoLog;

    public GestorArchivoLog() {
        this(ARCHIVO_DEFECTO);
    }

    public GestorArchivoLog(String rutaArchivo) {
        this.archivoLog = new File(rutaArchivo);
        asegurarDirectorio(archivoLog.getParentFile());
    }

    public static boolean asegurarDirectorio(File carpeta) {
        if (carpeta != null && !carpeta.exists()) {
            return carpeta.mkdirs();
        }
        return false;
    }

    public void mostrarInformacionArchivo() {
        System.out.println("=== Informacion del Archivo de Log ===");
        System.out.printf("Ruta relativa: %s%n", archivoLog.getPath());
        System.out.printf("Ruta absoluta: %s%n", archivoLog.getAbsolutePath());
        System.out.printf("Nombre del archivo: %s%n", archivoLog.getName());
        System.out.printf("Existe actualmente: %s%n", archivoLog.exists() ? "SI" : "NO");
        if (archivoLog.exists()) {
            System.out.printf("Tamano en bytes: %d%n", archivoLog.length());
        }
    }

    public boolean crearArchivoSiNoExiste() {
        try {
            asegurarDirectorio(archivoLog.getParentFile());
            return archivoLog.createNewFile();
        } catch (IOException e) {
            System.err.println("No se pudo crear el archivo de log: " + e.getMessage());
            return false;
        }
    }

    public synchronized void agregarLog(String textoLog) {
        if (textoLog == null || textoLog.isBlank()) {
            return;
        }
        asegurarDirectorio(archivoLog.getParentFile());
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivoLog, true))) {
            escritor.write("--- Registro de Combate (" + java.time.LocalDateTime.now() + ") ---");
            escritor.newLine();
            String[] lineas = textoLog.split("\\R");
            for (String linea : lineas) {
                escritor.write(linea);
                escritor.newLine();
            }
            escritor.newLine();
            escritor.flush();
        } catch (IOException e) {
            System.err.println("Error al escribir el registro de combate: " + e.getMessage());
        }
    }

    public synchronized List<String> leerHistorial() {
        List<String> lineas = new ArrayList<>();
        if (!archivoLog.exists()) {
            System.out.println("No se encontro el archivo de historial de combates.");
            return lineas;
        }

        try (BufferedReader lector = new BufferedReader(new FileReader(archivoLog))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.add(linea);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Archivo no encontrado: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de historial: " + e.getMessage());
        }
        return lineas;
    }

    public File getArchivo() {
        return archivoLog;
    }
}

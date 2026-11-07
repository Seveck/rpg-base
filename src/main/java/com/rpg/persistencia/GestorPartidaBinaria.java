package com.rpg.persistencia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Gestor de persistencia binaria mediante serializacion de objetos. */
public class GestorPartidaBinaria {

    private static final String ARCHIVO_DEFECTO = "partidas/partida.dat";
    private static final Object CANDADO_DISCO = new Object();

    private final File archivoPartida;

    public GestorPartidaBinaria() {
        this(ARCHIVO_DEFECTO);
    }

    public GestorPartidaBinaria(String ruta) {
        this.archivoPartida = new File(ruta);
        GestorArchivoLog.asegurarDirectorio(archivoPartida.getParentFile());
    }

    public boolean guardar(EstadoPartida estado) {
        synchronized (CANDADO_DISCO) {
            GestorArchivoLog.asegurarDirectorio(archivoPartida.getParentFile());
            File archivoTemporal = new File(archivoPartida.getAbsolutePath() + ".tmp");

            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivoTemporal))) {
                out.writeObject(estado);
                out.flush();
            } catch (NotSerializableException e) {
                System.err.println("Error de serializacion: clase u objeto no serializable -> " + e.getMessage());
                return false;
            } catch (IOException e) {
                System.err.println("Error de entrada/salida al guardar la partida: " + e.getMessage());
                return false;
            }

            try {
                Files.move(archivoTemporal.toPath(), archivoPartida.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return true;
            } catch (IOException e) {
                if (archivoTemporal.renameTo(archivoPartida)) {
                    return true;
                }
                System.err.println("No se pudo renombrar el archivo temporal de guardado: " + e.getMessage());
                return false;
            }
        }
    }

    public EstadoPartida cargar() {
        synchronized (CANDADO_DISCO) {
            if (!archivoPartida.exists()) {
                System.out.println("No existe ninguna partida guardada en: " + archivoPartida.getPath());
                return null;
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivoPartida))) {
                Object obj = in.readObject();
                if (obj instanceof EstadoPartida estado) {
                    return estado;
                } else {
                    System.err.println("El archivo no contiene un formato de partida valido.");
                    return null;
                }
            } catch (FileNotFoundException e) {
                System.out.println("Archivo de partida no encontrado: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println("No se encontro la definicion de clase de la partida guardada: " + e.getMessage());
            } catch (NotSerializableException e) {
                System.err.println("Objeto no serializable detectado al cargar: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error de lectura o archivo de partida corrupto: " + e.getMessage());
            }
            return null;
        }
    }

    public boolean borrarConFile() {
        synchronized (CANDADO_DISCO) {
            if (!archivoPartida.exists()) {
                return false;
            }
            return archivoPartida.delete();
        }
    }

    public boolean borrarConNIO() {
        synchronized (CANDADO_DISCO) {
            try {
                return Files.deleteIfExists(archivoPartida.toPath());
            } catch (IOException e) {
                System.err.println("Error al borrar archivo con NIO: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean existePartida() {
        synchronized (CANDADO_DISCO) {
            return archivoPartida.exists();
        }
    }

    public File getArchivo() {
        return archivoPartida;
    }
}

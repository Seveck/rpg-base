package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/*
 * Explicacion de la URL de conexion JDBC:
 * Formato general: jdbc:mysql://localhost:3306/rpg_grupal_db
 *
 * 1. Protocolo: jdbc indica la API de Java para bases de datos relacionales.
 * 2. Subprotocolo o Driver: mysql especifica el controlador para el motor de base de datos.
 * 3. Host o Servidor: localhost o 127.0.0.1 es la direccion de red del servidor.
 * 4. Puerto: 3306 es el puerto TCP donde escucha MySQL.
 * 5. Esquema o Base de datos: rpg_grupal_db es la base de datos destino.
 */
public class ConexionDB {

    private static final String ARCHIVO_CONFIG = "config.txt";

    // Constructor privado para evitar instanciacion
    private ConexionDB() {
    }

    /**
     * Lee el archivo config.txt y establece la conexion con MySQL.
     * Retorna una instancia valida de Connection o null si hubo fallos de configuracion/conexion.
     */
    public static Connection getConexion() {
        File archivo = new File(ARCHIVO_CONFIG);
        if (!archivo.exists()) {
            File alt = new File("IdeaProjects/rpg-base", ARCHIVO_CONFIG);
            if (alt.exists()) {
                archivo = alt;
            } else {
                alt = new File("rpg-base", ARCHIVO_CONFIG);
                if (alt.exists()) {
                    archivo = alt;
                }
            }
        }

        if (!archivo.exists()) {
            System.out.println("Error: El archivo " + ARCHIVO_CONFIG + " no existe en la raiz del proyecto.");
            System.out.println("Cree el archivo config.txt basandose en config.example.txt.");
            return null;
        }

        String url = null;
        String usuario = null;
        String clave = null;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                String[] partes = linea.split("=", 2);
                if (partes.length == 2) {
                    String nombreClave = partes[0].trim();
                    String valor = partes[1].trim();

                    switch (nombreClave) {
                        case "url" -> url = valor;
                        case "usuario" -> usuario = valor;
                        case "clave" -> clave = valor;
                        default -> {
                            // Ignorar claves no reconocidas
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: No se encontro el archivo de configuracion " + ARCHIVO_CONFIG + ".");
            return null;
        } catch (IOException e) {
            System.out.println("Error al leer el archivo " + ARCHIVO_CONFIG + ": " + e.getMessage());
            return null;
        }

        if (url == null || usuario == null || clave == null) {
            System.out.println("Error: El archivo " + ARCHIVO_CONFIG + " debe contener las claves 'url', 'usuario' y 'clave'.");
            return null;
        }

        try {
            return DriverManager.getConnection(url, usuario, clave);
        } catch (SQLException e) {
            System.out.println("Error al conectar con la base de datos MySQL: " + e.getMessage());
            return null;
        }
    }
}

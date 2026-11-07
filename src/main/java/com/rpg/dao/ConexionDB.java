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
 * EXPLICACION DE LA URL DE CONEXION JDBC:
 * Formato general: jdbc:mysql://localhost:3306/rpg_grupal_db
 *
 * 1. Protocolo (jdbc):
 *    Indica la API estandar de Java para conectar con bases de datos relacionales (Java Database Connectivity).
 *
 * 2. Subprotocolo / Driver (mysql):
 *    Especifica el motor de base de datos concreto con el cual comunicarse. Le indica al DriverManager
 *    que debe cargar y delegar en el controlador de MySQL (com.mysql.cj.jdbc.Driver provisto por mysql-connector-j).
 *
 * 3. Host / Servidor (localhost o 127.0.0.1):
 *    Direccion de red o nombre de host de la maquina donde se encuentra ejecutandose el servidor de MySQL.
 *
 * 4. Puerto (3306):
 *    Puerto TCP estandar en el cual el servicio de base de datos MySQL o MariaDB escucha las conexiones entrantes.
 *
 * 5. Esquema / Base de datos (rpg_grupal_db):
 *    Nombre de la base de datos especifica a la que la aplicacion se conectara e interactuara con sus tablas.
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

package com.rpg.controller;

import com.rpg.dao.HeroeDAO;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.util.Collections;
import java.util.List;

/**
 * Controlador de la arquitectura MVC.
 * Coordina el flujo de datos entre la View y el DAO,
 * aplicando todas las reglas de validacion del negocio.
 */
public class HeroeController {

    private final HeroeDAO heroeDAO;

    public HeroeController(HeroeDAO heroeDAO) {
        this.heroeDAO = heroeDAO;
    }

    /**
     * Valida los datos y registra un nuevo heroe a traves del DAO.
     */
    public String registrarHeroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "Error de validacion: El nombre del heroe no puede estar vacio.";
        }

        if (tipo == null || tipo.trim().isEmpty()) {
            return "Error de validacion: El tipo no puede estar vacio.";
        }

        String tipoNorm = tipo.trim().toLowerCase();
        if (!tipoNorm.equals("guerrero") && !tipoNorm.equals("mago")) {
            return "Error de validacion: El tipo debe ser 'guerrero' o 'mago'.";
        }

        if (nivel < 1) {
            return "Error de validacion: El nivel debe ser mayor o igual a 1.";
        }

        if (vidaMaxima <= 0) {
            return "Error de validacion: La vida maxima debe ser mayor a 0.";
        }

        Heroe heroe = new Heroe(nombre.trim(), tipoNorm, vidaMaxima, nivel);
        int filas = heroeDAO.insertar(heroe);

        if (filas > 0) {
            return "Heroe registrado con exito con ID: " + heroe.getId();
        } else {
            return "No se pudo registrar el heroe. Verifique si el nombre ya existe o la conexion a la BD.";
        }
    }

    /**
     * Retorna la lista de todos los heroes registrados.
     */
    public List<Heroe> listarHeroes() {
        return heroeDAO.listar();
    }

    /**
     * Busca heroes por su tipo con validacion previa.
     */
    public List<Heroe> buscarPorTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            System.out.println("Error de validacion: Debe indicar un tipo.");
            return Collections.emptyList();
        }

        String tipoNorm = tipo.trim().toLowerCase();
        if (!tipoNorm.equals("guerrero") && !tipoNorm.equals("mago")) {
            System.out.println("Error de validacion: El tipo a buscar debe ser 'guerrero' o 'mago'.");
            return Collections.emptyList();
        }

        return heroeDAO.buscarPorTipo(tipoNorm);
    }

    /**
     * Actualiza el nivel de un heroe validando datos y evaluando filas afectadas.
     */
    public String actualizarNivel(int id, int nivel) {
        if (id <= 0) {
            return "Error de validacion: ID invalido.";
        }
        if (nivel < 1) {
            return "Error de validacion: El nivel debe ser mayor o igual a 1.";
        }

        int filas = heroeDAO.actualizarNivel(id, nivel);
        if (filas == 0) {
            return "no existe un heroe con ese id";
        }
        return "Nivel actualizado correctamente.";
    }

    /**
     * Elimina un heroe por ID validando datos y evaluando filas afectadas.
     */
    public String eliminarHeroe(int id) {
        if (id <= 0) {
            return "Error de validacion: ID invalido.";
        }

        int filas = heroeDAO.eliminar(id);
        if (filas == 0) {
            return "no existe un heroe con ese id";
        }
        return "Heroe eliminado con exito.";
    }

    /**
     * Busca un heroe por ID en la BD.
     */
    public Heroe buscarPorId(int id) {
        if (id <= 0) {
            return null;
        }
        return heroeDAO.buscarPorId(id);
    }

    /**
     * Instancia un Personaje jugable a partir de los datos del heroe.
     */
    public Personaje crearPersonajeDesdeHeroe(Heroe heroe) {
        if (heroe == null) {
            return null;
        }
        if ("guerrero".equalsIgnoreCase(heroe.getTipo())) {
            return new Guerrero(heroe.getNombre());
        } else if ("mago".equalsIgnoreCase(heroe.getTipo())) {
            return new Mago(heroe.getNombre());
        }
        return null;
    }
}

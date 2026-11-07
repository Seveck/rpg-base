package com.rpg.model;

/**
 * Entidad POJO que representa a un Heroe en el sistema.
 * Mapea las columnas de la tabla 'heroes' en la base de datos.
 */
public class Heroe {

    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    public Heroe() {
        this.nivel = 1;
    }

    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        setNombre(nombre);
        setTipo(tipo);
        setVidaMaxima(vidaMaxima);
        setNivel(nivel);
    }

    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        this.id = id;
        setNombre(nombre);
        setTipo(tipo);
        setVidaMaxima(vidaMaxima);
        setNivel(nivel);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del heroe no puede estar vacio.");
        }
        this.nombre = nombre.trim();
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo no puede estar vacio.");
        }
        String tipoNormalizado = tipo.trim().toLowerCase();
        if (!tipoNormalizado.equals("guerrero") && !tipoNormalizado.equals("mago")) {
            throw new IllegalArgumentException("El tipo debe ser 'guerrero' o 'mago'.");
        }
        this.tipo = tipoNormalizado;
    }

    public int getVidaMaxima() {
        return this.vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor a 0.");
        }
        this.vidaMaxima = vidaMaxima;
    }

    public int getNivel() {
        return this.nivel;
    }

    public void setNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser mayor o igual a 1.");
        }
        this.nivel = nivel;
    }

    @Override
    public String toString() {
        return String.format("Heroe [ID: %d | Nombre: %s | Tipo: %s | Vida Maxima: %d | Nivel: %d]",
                this.id, this.nombre, this.tipo, this.vidaMaxima, this.nivel);
    }
}

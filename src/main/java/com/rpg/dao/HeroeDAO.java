package com.rpg.dao;

import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa DAO (Data Access Object) para gestionar la persistencia
 * de la entidad Heroe en la base de datos MySQL.
 * Toda operacion utiliza PreparedStatement y try-with-resources.
 */
public class HeroeDAO {

    /**
     * Inserta un nuevo heroe en la base de datos.
     * Utiliza executeUpdate y asigna el ID generado al objeto heroe.
     * Retorna el numero de filas afectadas.
     */
    public int insertar(Heroe heroe) {
        String sql = "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";

        try (Connection con = ConexionDB.getConexion()) {
            if (con == null) {
                return 0;
            }

            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, heroe.getNombre());
                ps.setString(2, heroe.getTipo());
                ps.setInt(3, heroe.getVidaMaxima());
                ps.setInt(4, heroe.getNivel());

                int filas = ps.executeUpdate();
                if (filas > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            heroe.setId(rs.getInt(1));
                        }
                    }
                }
                return filas;
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar el heroe en la base de datos: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Obtiene la lista completa de heroes registrados en la base de datos.
     * Utiliza executeQuery y recorre con while (rs.next()).
     */
    public List<Heroe> listar() {
        List<Heroe> heroes = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes ORDER BY id";

        try (Connection con = ConexionDB.getConexion()) {
            if (con == null) {
                return heroes;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Heroe heroe = new Heroe(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getInt("vida_maxima"),
                            rs.getInt("nivel")
                    );
                    heroes.add(heroe);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar la lista de heroes: " + e.getMessage());
        }

        return heroes;
    }

    /**
     * Busca heroes filtrados por su tipo (guerrero o mago).
     * Utiliza executeQuery con parametro ? para prevenir inyeccion SQL.
     */
    public List<Heroe> buscarPorTipo(String tipo) {
        List<Heroe> heroes = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE LOWER(tipo) = LOWER(?) ORDER BY id";

        try (Connection con = ConexionDB.getConexion()) {
            if (con == null) {
                return heroes;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, tipo);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Heroe heroe = new Heroe(
                                rs.getInt("id"),
                                rs.getString("nombre"),
                                rs.getString("tipo"),
                                rs.getInt("vida_maxima"),
                                rs.getInt("nivel")
                        );
                        heroes.add(heroe);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar heroes por tipo: " + e.getMessage());
        }

        return heroes;
    }

    /**
     * Busca un heroe por su identificador primario.
     */
    public Heroe buscarPorId(int id) {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE id = ?";

        try (Connection con = ConexionDB.getConexion()) {
            if (con == null) {
                return null;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Heroe(
                                rs.getInt("id"),
                                rs.getString("nombre"),
                                rs.getString("tipo"),
                                rs.getInt("vida_maxima"),
                                rs.getInt("nivel")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar heroe por id: " + e.getMessage());
        }

        return null;
    }

    /**
     * Actualiza el nivel de un heroe existente por su ID.
     * Retorna el numero de filas afectadas (0 si no existe el ID).
     */
    public int actualizarNivel(int id, int nivel) {
        String sql = "UPDATE heroes SET nivel = ? WHERE id = ?";

        try (Connection con = ConexionDB.getConexion()) {
            if (con == null) {
                return 0;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, nivel);
                ps.setInt(2, id);

                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error al actualizar el nivel del heroe: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Elimina un heroe de la base de datos por su ID.
     * Retorna el numero de filas afectadas (0 si no existe el ID).
     */
    public int eliminar(int id) {
        String sql = "DELETE FROM heroes WHERE id = ?";

        try (Connection con = ConexionDB.getConexion()) {
            if (con == null) {
                return 0;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);

                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar el heroe: " + e.getMessage());
            return 0;
        }
    }
}

package com.rpg;

import com.rpg.controller.HeroeController;
import com.rpg.dao.ConexionDB;
import com.rpg.dao.HeroeDAO;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Pruebas de integracion automatizadas para JDBC, DAO, Controller y MySQL.
 */
public class VerificacionJDBCTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   VERIFICACION AUTOMATIZADA: JDBC Y PERSISTENCIA  ");
        System.out.println("==================================================");

        int total = 0;
        int exitos = 0;

        total++;
        if (probarConexionDB()) exitos++;

        total++;
        if (probarInsercionYPersistencia()) exitos++;

        total++;
        if (probarListarYBuscarPorTipo()) exitos++;

        total++;
        if (probarActualizacionNivel()) exitos++;

        total++;
        if (probarValidacionesYMensajes()) exitos++;

        total++;
        if (probarIntegracionJuego()) exitos++;

        total++;
        if (probarEliminacion()) exitos++;

        System.out.println("==================================================");
        System.out.printf("RESULTADO PRUEBAS JDBC: %d de %d APROBADAS%n", exitos, total);
        System.out.println("==================================================");

        if (exitos != total) {
            System.exit(1);
        }
    }

    private static boolean probarConexionDB() {
        System.out.print("[TEST 1] Conexion con MySQL via config.txt... ");
        try (Connection con = ConexionDB.getConexion()) {
            if (con != null && !con.isClosed()) {
                System.out.println("OK (Conexion activa y valida)");
                return true;
            }
        } catch (Exception e) {
            System.out.println("FALLO: " + e.getMessage());
            return false;
        }
        System.out.println("FALLO: Conexion nula o cerrada");
        return false;
    }

    private static boolean probarInsercionYPersistencia() {
        System.out.print("[TEST 2] Insercion y persistencia en MySQL... ");
        HeroeDAO dao = new HeroeDAO();
        String nombrePrueba = "HeroeTest_" + System.currentTimeMillis();
        Heroe heroe = new Heroe(nombrePrueba, "guerrero", 120, 1);

        int filas = dao.insertar(heroe);
        if (filas <= 0 || heroe.getId() <= 0) {
            System.out.println("FALLO al insertar heroe en DAO");
            return false;
        }

        // Verificacion directa en base de datos via SQL nativo
        String sql = "SELECT nombre, tipo, vida_maxima, nivel FROM heroes WHERE id = ?";
        try (Connection con = ConexionDB.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, heroe.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean ok = rs.getString("nombre").equals(nombrePrueba)
                            && rs.getString("tipo").equalsIgnoreCase("guerrero")
                            && rs.getInt("vida_maxima") == 120
                            && rs.getInt("nivel") == 1;
                    if (ok) {
                        System.out.println("OK (Persistido con ID " + heroe.getId() + ")");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("FALLO en verificacion SQL: " + e.getMessage());
            return false;
        }

        System.out.println("FALLO: Registro no encontrado en MySQL");
        return false;
    }

    private static boolean probarListarYBuscarPorTipo() {
        System.out.print("[TEST 3] Listar y buscar por tipo con PreparedStatement... ");
        HeroeDAO dao = new HeroeDAO();
        List<Heroe> todos = dao.listar();
        if (todos.isEmpty()) {
            System.out.println("FALLO: La lista de heroes esta vacia");
            return false;
        }

        List<Heroe> guerreros = dao.buscarPorTipo("guerrero");
        for (Heroe g : guerreros) {
            if (!g.getTipo().equalsIgnoreCase("guerrero")) {
                System.out.println("FALLO: Filtro por tipo retorno un tipo invalido");
                return false;
            }
        }

        System.out.println("OK (Total: " + todos.size() + ", Guerreros: " + guerreros.size() + ")");
        return true;
    }

    private static boolean probarActualizacionNivel() {
        System.out.print("[TEST 4] Actualizacion de nivel en MySQL... ");
        HeroeDAO dao = new HeroeDAO();
        String nombre = "NivelTest_" + System.currentTimeMillis();
        Heroe h = new Heroe(nombre, "mago", 80, 1);
        dao.insertar(h);

        int filas = dao.actualizarNivel(h.getId(), 5);
        if (filas != 1) {
            System.out.println("FALLO: Filas afectadas != 1 al actualizar nivel");
            return false;
        }

        Heroe actualizado = dao.buscarPorId(h.getId());
        if (actualizado == null || actualizado.getNivel() != 5) {
            System.out.println("FALLO: El nivel persistido en BD no es 5");
            return false;
        }

        dao.eliminar(h.getId());
        System.out.println("OK (Nivel actualizado a 5 y comprobado)");
        return true;
    }

    private static boolean probarValidacionesYMensajes() {
        System.out.print("[TEST 5] Validaciones de Controller y mensaje 'no existe un heroe con ese id'... ");
        HeroeDAO dao = new HeroeDAO();
        HeroeController controller = new HeroeController(dao);

        String errNombre = controller.registrarHeroe("", "guerrero", 120, 1);
        if (!errNombre.contains("Error de validacion")) {
            System.out.println("FALLO: No valido nombre vacio");
            return false;
        }

        String errTipo = controller.registrarHeroe("TestVal", "arquero", 100, 1);
        if (!errTipo.contains("Error de validacion")) {
            System.out.println("FALLO: No valido tipo invalido");
            return false;
        }

        String msgIdInexistente = controller.actualizarNivel(999999, 10);
        if (!msgIdInexistente.equals("no existe un heroe con ese id")) {
            System.out.println("FALLO: Mensaje esperado 'no existe un heroe con ese id' pero fue: " + msgIdInexistente);
            return false;
        }

        String msgEliminarInexistente = controller.eliminarHeroe(999999);
        if (!msgEliminarInexistente.equals("no existe un heroe con ese id")) {
            System.out.println("FALLO: Mensaje de eliminar no coincide con 'no existe un heroe con ese id'");
            return false;
        }

        System.out.println("OK");
        return true;
    }

    private static boolean probarIntegracionJuego() {
        System.out.print("[TEST 6] Integracion: cargar heroe desde BD a entidad del simulador... ");
        HeroeDAO dao = new HeroeDAO();
        HeroeController controller = new HeroeController(dao);

        Heroe hGuerrero = new Heroe("Conan", "guerrero", 120, 3);
        Personaje pGuerrero = controller.crearPersonajeDesdeHeroe(hGuerrero);
        if (!(pGuerrero instanceof Guerrero) || !pGuerrero.getNombre().equals("Conan")) {
            System.out.println("FALLO al crear Guerrero");
            return false;
        }

        Heroe hMago = new Heroe("Merlin", "mago", 80, 2);
        Personaje pMago = controller.crearPersonajeDesdeHeroe(hMago);
        if (!(pMago instanceof Mago) || !pMago.getNombre().equals("Merlin")) {
            System.out.println("FALLO al crear Mago");
            return false;
        }

        System.out.println("OK (Entidades instanciadas correctamente)");
        return true;
    }

    private static boolean probarEliminacion() {
        System.out.print("[TEST 7] Eliminacion de heroe en MySQL... ");
        HeroeDAO dao = new HeroeDAO();
        String nombre = "EliminarTest_" + System.currentTimeMillis();
        Heroe h = new Heroe(nombre, "guerrero", 120, 1);
        dao.insertar(h);

        int filas = dao.eliminar(h.getId());
        if (filas != 1) {
            System.out.println("FALLO: Filas afectadas != 1 al eliminar");
            return false;
        }

        Heroe buscado = dao.buscarPorId(h.getId());
        if (buscado != null) {
            System.out.println("FALLO: El heroe aun existe tras ser eliminado");
            return false;
        }

        System.out.println("OK (Heroe eliminado de BD exitosamente)");
        return true;
    }
}

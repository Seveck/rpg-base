package com.rpg.core;

import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final PerfilJugador perfil = new PerfilJugador();
    private final Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final com.rpg.persistencia.GestorArchivoLog gestorLog = new com.rpg.persistencia.GestorArchivoLog();
    private final com.rpg.persistencia.GestorPartidaBinaria gestorPersistencia = new com.rpg.persistencia.GestorPartidaBinaria();
    private final com.rpg.concurrency.AutoGuardadoDaemon autoGuardadoDaemon;

    private static final int SEGUNDOS_TURNO = 10;
    private Personaje heroeActual;
    private String ultimoGuardado = "";

    public MotorJuego(Consola consola) {
        this.consola = consola;
        this.autoGuardadoDaemon = new com.rpg.concurrency.AutoGuardadoDaemon(this::guardarProgresoSilencioso, 5000);
        this.autoGuardadoDaemon.iniciar();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        if (heroeActual == null) {
            heroeActual = crearJugador();
        } else {
            heroeActual.restaurarVida();
        }
        Personaje jugador = heroeActual;
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);

        cerrarCombate(jugador, enemigo, huyo);
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        guardarProgreso();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (accion == null) {
                registro.agregar("%s tardo demasiado y pierde el turno.", jugador.getNombre());
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * Pide la accion al jugador protegiendo con un temporizador de turno.
     * Si el tiempo se agota antes de responder, se pierde el turno.
     */
    private Accion pedirAccion() {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");

        TurnoCallback callback = () -> {
            System.out.println("\n[Tiempo agotado! Pierdes el turno]");
        };

        com.rpg.concurrency.GestorTurno gestor = new com.rpg.concurrency.GestorTurno(SEGUNDOS_TURNO, callback);
        gestor.iniciar();

        Integer opcion = consola.leerEnteroConTimeout("Accion: ", 0, 4, SEGUNDOS_TURNO, gestor);

        if (opcion == null) {
            callback.onTiempoAgotado();
            return null; // tiempo agotado
        }
        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d turnos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
        gestorLog.agregarLog(registro.getTexto());
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado y persistencia

    /**
     * Guarda las estadisticas en memoria y en disco (serializacion de EstadoPartida).
     */
    public synchronized void guardarProgreso() {
        guardarProgresoSilencioso();
        System.out.println("[Progreso guardado en memoria y en disco]");
    }

    public synchronized void guardarProgresoSilencioso() {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();

        if (heroeActual != null) {
            com.rpg.persistencia.EstadoPartida estado = new com.rpg.persistencia.EstadoPartida(
                    heroeActual, perfil, gremio, bestiario);
            gestorPersistencia.guardar(estado);
        }
    }

    public void cargarPartida() {
        System.out.println("\n=== CARGAR PARTIDA ===");
        com.rpg.persistencia.EstadoPartida estado = gestorPersistencia.cargar();
        if (estado != null) {
            this.heroeActual = estado.getJugador();
            System.out.println("Partida cargada exitosamente.");
            System.out.printf("Heroe: %s (%s, Vida: %d/%d)%n",
                    heroeActual.getNombre(), heroeActual.getTipo(), heroeActual.getVida(), heroeActual.getVidaMaxima());
            System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                    estado.getPerfil().getVictorias(), estado.getPerfil().getDerrotas(), estado.getPerfil().getLogros());
            System.out.printf("Gremio: %s (%d miembros)%n",
                    estado.getGremio().getNombre(), estado.getGremio().getMiembros().size());
            System.out.printf("Verificacion transient: notaSesionVolatil = %s%n",
                    estado.getNotaSesionVolatil());
            boolean circularOk = estado.getGremio().getMiembros().contains(estado.getJugador());
            System.out.printf("Relacion circular Personaje <-> Gremio conservada: %s%n", circularOk ? "SI" : "NO");
        }
    }

    public void mostrarHistorial() {
        System.out.println("\n=== HISTORIAL DE COMBATES (.txt) ===");
        List<String> lineas = gestorLog.leerHistorial();
        if (lineas.isEmpty()) {
            System.out.println("No hay registros en el archivo.");
        } else {
            for (String linea : lineas) {
                System.out.println(linea);
            }
        }
    }

    public void borrarPartida() {
        System.out.println("\n=== BORRAR PARTIDA GUARDADA ===");
        if (!gestorPersistencia.existePartida()) {
            System.out.println("No existe ninguna partida guardada para borrar.");
            return;
        }
        int confirmacion = consola.leerEntero("Seguro que deseas borrar la partida? (1. Si, 0. No): ", 0, 1);
        if (confirmacion == 1) {
            boolean borrado = gestorPersistencia.borrarConNIO();
            if (borrado) {
                this.heroeActual = null;
                this.ultimoGuardado = "";
                System.out.println("Partida guardada borrada con exito.");
            } else {
                System.out.println("No se pudo borrar el archivo de partida.");
            }
        } else {
            System.out.println("Operacion cancelada.");
        }
    }

    public void mostrarInformacionArchivos() {
        System.out.println("\n=== INFORMACION DE ARCHIVOS ===");
        gestorLog.mostrarInformacionArchivo();
        System.out.println();
        java.io.File binario = gestorPersistencia.getArchivo();
        System.out.println("=== Informacion del Archivo de Partida Binaria ===");
        System.out.printf("Ruta relativa: %s%n", binario.getPath());
        System.out.printf("Ruta absoluta: %s%n", binario.getAbsolutePath());
        System.out.printf("Nombre del archivo: %s%n", binario.getName());
        System.out.printf("Existe actualmente: %s%n", binario.exists() ? "SI" : "NO");
        if (binario.exists()) {
            System.out.printf("Tamano en bytes: %d%n", binario.length());
        }
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }

    public Personaje getHeroeActual() {
        return heroeActual;
    }

    public void setHeroeActual(Personaje heroeActual) {
        this.heroeActual = heroeActual;
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Bestiario getBestiario() {
        return bestiario;
    }
}

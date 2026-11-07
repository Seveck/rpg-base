package com.rpg;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.TemporizadorTurnoTask;
import com.rpg.core.Bestiario;
import com.rpg.core.TurnoCallback;
import com.rpg.entities.Arma;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorArchivoLog;
import com.rpg.persistencia.GestorPartidaBinaria;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Suite de pruebas integrales para concurrencia y persistencia. */
public class VerificacionIntegralTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  INICIANDO VERIFICACION INTEGRAL RPG (TALLERES)  ");
        System.out.println("==================================================");

        int totalPruebas = 0;
        int exitosas = 0;

        // 1. Temporizador de turno y cancelacion por interrupcion
        totalPruebas++;
        if (probarTemporizadorYCancelacion()) exitosas++;

        // 2. EfectoContinuoTask e interrupcion limpia
        totalPruebas++;
        if (probarEfectoContinuoTask()) exitosas++;

        // 3. Condicion de carrera y sincronizacion de Personaje
        totalPruebas++;
        if (probarSincronizacionPersonaje()) exitosas++;

        // 4. Investigacion obligatoria: Hilos Daemon vs Usuario y Excepcion IllegalThreadStateException
        totalPruebas++;
        if (probarInvestigacionHilosDaemon()) exitosas++;

        // 5. Investigacion obligatoria: ReentrantLock vs synchronized
        totalPruebas++;
        if (probarInvestigacionReentrantLock()) exitosas++;

        // 6. Archivos: Log de combate con BufferedWriter y BufferedReader
        totalPruebas++;
        if (probarGestorArchivoLog()) exitosas++;

        // 7. Archivos: Serializacion, ciclos (Personaje <-> Gremio) y campos transient
        totalPruebas++;
        if (probarSerializacionYGrafo()) exitosas++;

        // 8. Archivos: Borrado con File.delete() y Files.deleteIfExists(Path)
        totalPruebas++;
        if (probarBorradoArchivos()) exitosas++;

        System.out.println("\n==================================================");
        System.out.printf("  RESUMEN FINAL: %d / %d PRUEBAS EXITOSAS%n", exitosas, totalPruebas);
        System.out.println("==================================================");

        if (exitosas != totalPruebas) {
            System.exit(1);
        }
    }

    private static boolean probarTemporizadorYCancelacion() {
        System.out.println("\n[TEST 1] TemporizadorTurnoTask y GestorTurno...");
        AtomicBoolean tiempoAgotado = new AtomicBoolean(false);
        TurnoCallback callback = () -> tiempoAgotado.set(true);

        // Caso A: Cancelacion a tiempo
        GestorTurno gestor = new GestorTurno(2, callback);
        gestor.iniciar();
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}
        gestor.cancelar(); // interrumpe
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}

        if (tiempoAgotado.get()) {
            System.err.println("FALLO: El temporizador debio ser cancelado y no disparar el callback.");
            return false;
        }

        // Caso B: Expiracion completa
        GestorTurno gestorExpiracion = new GestorTurno(1, callback);
        gestorExpiracion.iniciar();
        try {
            Thread.sleep(1300);
        } catch (InterruptedException ignored) {}

        if (!tiempoAgotado.get()) {
            System.err.println("FALLO: El callback debio ejecutarse al expirar el tiempo.");
            return false;
        }

        System.out.println("OK: Temporizador y cancelacion por interrupcion validados.");
        return true;
    }

    private static boolean probarEfectoContinuoTask() {
        System.out.println("\n[TEST 2] EfectoContinuoTask e interrupcion limpia...");
        Guerrero g = new Guerrero("GuerreroPrueba");
        int vidaInicial = g.getVida();
        EstadoEfecto veneno = new EstadoEfecto("VenenoPrueba", TipoEfecto.DANIO_CONTINUO, 5, 10);
        g.aplicarEfecto(veneno);

        EfectoContinuoTask tarea = new EfectoContinuoTask(g, veneno, 100, null);
        Thread hiloEfecto = new Thread(tarea, "Efecto-Veneno");
        g.registrarHiloEfecto(veneno.getNombre(), hiloEfecto);
        hiloEfecto.start();

        try {
            Thread.sleep(250); // Deja que aplique 1 o 2 ticks
        } catch (InterruptedException ignored) {}

        if (g.getVida() >= vidaInicial) {
            System.err.println("FALLO: El efecto continuo debio reducir la vida del objetivo.");
            return false;
        }

        // Interrupcion del efecto (simula uso de antidoto)
        g.quitarEfecto(veneno.getNombre());
        try {
            hiloEfecto.join(500);
        } catch (InterruptedException ignored) {}

        if (hiloEfecto.isAlive()) {
            System.err.println("FALLO: El hilo de efecto continuo debio terminar limpio tras interrupt().");
            return false;
        }

        System.out.println("OK: EfectoContinuoTask responde a ticks y finaliza limpio con interrupt().");
        return true;
    }

    private static boolean probarSincronizacionPersonaje() {
        System.out.println("\n[TEST 3] Condicion de carrera y sincronizacion de Personaje...");
        Guerrero g = new Guerrero("HeroeSincronizado");
        // Ajustamos la vida a la mitad (60/120) para que las curaciones no topen con vidaMaxima
        g.setVida(60);
        int vidaInicial = g.getVida();
        int numHilos = 40;
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFin = new CountDownLatch(numHilos * 2);

        // Mitad de hilos aplican danio (5 c/u), mitad curan (5 c/u)
        for (int i = 0; i < numHilos; i++) {
            new Thread(() -> {
                try {
                    latchInicio.await();
                    g.recibirDanio(5);
                } catch (Exception ignored) {
                } finally {
                    latchFin.countDown();
                }
            }).start();

            new Thread(() -> {
                try {
                    latchInicio.await();
                    g.curar(5);
                } catch (Exception ignored) {
                } finally {
                    latchFin.countDown();
                }
            }).start();
        }

        latchInicio.countDown(); // Despierta a todos simultaneamente
        try {
            latchFin.await();
        } catch (InterruptedException ignored) {}

        if (g.getVida() != vidaInicial) {
            System.err.printf("FALLO en consistencia: vida esperada %d pero fue %d.%n", vidaInicial, g.getVida());
            return false;
        }

        System.out.printf("OK: %d hilos concurrentes finalizaron sin perdida de actualizaciones (Vida: %d/%d).%n",
                numHilos * 2, g.getVida(), g.getVidaMaxima());
        return true;
    }

    private static boolean probarInvestigacionHilosDaemon() {
        System.out.println("\n[TEST 4] Investigacion: Hilos Daemon vs Usuario y IllegalThreadStateException...");
        AtomicBoolean lanzoExcepcionEsperada = new AtomicBoolean(false);

        Thread hilo = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        });

        hilo.start();
        try {
            // Intentar llamar a setDaemon(true) DESPUES de start() DEBE lanzar IllegalThreadStateException
            hilo.setDaemon(true);
        } catch (IllegalThreadStateException e) {
            lanzoExcepcionEsperada.set(true);
        }

        if (!lanzoExcepcionEsperada.get()) {
            System.err.println("FALLO: setDaemon despues de start() no lanzo IllegalThreadStateException.");
            return false;
        }

        // Probar AutoGuardadoDaemon creado correctamente
        AutoGuardadoDaemon daemon = new AutoGuardadoDaemon(() -> {}, 1000);
        daemon.iniciar();
        boolean esDaemon = daemon.esDaemon();
        daemon.detener();

        if (!esDaemon) {
            System.err.println("FALLO: AutoGuardadoDaemon no configuro el hilo como daemon.");
            return false;
        }

        System.out.println("OK: Demostrado que setDaemon tras start lanza IllegalThreadStateException y daemon funciona.");
        return true;
    }

    private static boolean probarInvestigacionReentrantLock() {
        System.out.println("\n[TEST 5] Investigacion: ReentrantLock en Personaje...");
        Mago mago = new Mago("MagoLock");
        int vidaAntes = mago.getVida();
        mago.recibirDanioConLock(15);

        if (mago.getVida() != vidaAntes - 15) {
            System.err.println("FALLO: recibirDanioConLock no aplico el danio esperado.");
            return false;
        }

        System.out.println("OK: ReentrantLock aplicado correctamente con try-finally.");
        return true;
    }

    private static boolean probarGestorArchivoLog() {
        System.out.println("\n[TEST 6] Persistencia: GestorArchivoLog (BufferedWriter/Reader)...");
        File logPrueba = new File("partidas/test_combates.txt");
        if (logPrueba.exists()) logPrueba.delete();

        GestorArchivoLog gestor = new GestorArchivoLog(logPrueba.getPath());
        gestor.mostrarInformacionArchivo();

        gestor.agregarLog("Línea 1: Combate de prueba");
        gestor.agregarLog("Línea 2: Thor vence a Ogro");

        var lineas = gestor.leerHistorial();
        if (lineas.size() < 2) {
            System.err.println("FALLO: No se leyeron las lineas esperadas del log.");
            return false;
        }

        logPrueba.delete();
        System.out.println("OK: Escritura en modo append y lectura linea por linea validadas.");
        return true;
    }

    private static boolean probarSerializacionYGrafo() {
        System.out.println("\n[TEST 7] Persistencia: Serializacion, ciclos (Personaje <-> Gremio) y transient...");
        File datPrueba = new File("partidas/test_partida.dat");
        if (datPrueba.exists()) datPrueba.delete();

        GestorPartidaBinaria gestor = new GestorPartidaBinaria(datPrueba.getPath());

        Guerrero heroe = new Guerrero("Arthur");
        heroe.equiparArma(new Arma("Excalibur", 15, RaridadItem.LEGENDARIO));
        try {
            heroe.getInventario().agregar(new Pocion("Elixir Mayor", 50, RaridadItem.RARO));
        } catch (Exception ignored) {}

        Gremio gremio = new Gremio("Mesa Redonda");
        heroe.unirseAGremio(gremio); // Ciclo: heroe referencia a gremio, gremio contiene a heroe

        PerfilJugador perfil = new PerfilJugador();
        perfil.registrarVictoria();
        perfil.desbloquearLogro("Leyenda");
        perfil.registrarPartidaSesion(); // campo transient = 1

        EstadoPartida estadoOriginal = new EstadoPartida(heroe, perfil, gremio, new Bestiario());
        estadoOriginal.setNotaSesionVolatil("Temporal");

        boolean guardadoOk = gestor.guardar(estadoOriginal);
        if (!guardadoOk) {
            System.err.println("FALLO al guardar partida con ObjectOutputStream.");
            return false;
        }

        EstadoPartida estadoCargado = gestor.cargar();
        if (estadoCargado == null) {
            System.err.println("FALLO al cargar partida con ObjectInputStream.");
            return false;
        }

        // Verificacion 1: Datos basicos
        if (!estadoCargado.getJugador().getNombre().equals("Arthur") ||
            !estadoCargado.getJugador().getArma().getNombre().equals("Excalibur")) {
            System.err.println("FALLO: Los datos recuperados del heroe no coinciden.");
            return false;
        }

        // Verificacion 2: Ciclo Personaje <-> Gremio
        boolean gremioContieneHeroe = estadoCargado.getGremio().getMiembros().contains(estadoCargado.getJugador());
        boolean heroeReferenciaGremio = estadoCargado.getJugador().getGremio() == estadoCargado.getGremio();
        if (!gremioContieneHeroe || !heroeReferenciaGremio) {
            System.err.println("FALLO: La relacion circular Personaje <-> Gremio no se reconstituyo correctamente.");
            return false;
        }

        // Verificacion 3: transient reseteado a null o valor por defecto
        if (estadoCargado.getNotaSesionVolatil() != null) {
            System.err.println("FALLO: El campo transient no volvio a su valor nulo por defecto.");
            return false;
        }
        if (estadoCargado.getPerfil().getPartidasJugadasSesion() != 0) {
            System.err.println("FALLO: El campo transient en PerfilJugador no se reinicio a 0.");
            return false;
        }

        datPrueba.delete();
        System.out.println("OK: Grafo circular, herencia serializable y campos transient comprobados exitosamente.");
        return true;
    }

    private static boolean probarBorradoArchivos() {
        System.out.println("\n[TEST 8] Persistencia: Borrado con File.delete() y Files.deleteIfExists()...");
        File f1 = new File("partidas/test_del1.dat");
        File f2 = new File("partidas/test_del2.dat");

        try {
            f1.createNewFile();
            f2.createNewFile();
        } catch (Exception ignored) {}

        GestorPartidaBinaria gestor1 = new GestorPartidaBinaria(f1.getPath());
        boolean b1 = gestor1.borrarConFile();

        GestorPartidaBinaria gestor2 = new GestorPartidaBinaria(f2.getPath());
        boolean b2 = gestor2.borrarConNIO();

        if (!b1 || !b2) {
            System.err.println("FALLO: No se borraron los archivos correctamente.");
            return false;
        }

        System.out.println("OK: Borrado con File.delete() y Files.deleteIfExists(Path) verificado.");
        return true;
    }
}

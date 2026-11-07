package com.rpg.persistencia;

import com.rpg.core.Bestiario;
import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;

import java.io.Serializable;

/** Contenedor del estado persistible de una partida RPG. */
public class EstadoPartida implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Personaje jugador;
    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final Bestiario bestiario;
    private final long timestampGuardado;

    private transient String notaSesionVolatil = "Sesion en curso";

    public EstadoPartida(Personaje jugador, PerfilJugador perfil, Gremio gremio, Bestiario bestiario) {
        this.jugador = jugador;
        this.perfil = perfil;
        this.gremio = gremio;
        this.bestiario = bestiario;
        this.timestampGuardado = System.currentTimeMillis();
    }

    public Personaje getJugador() {
        return jugador;
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

    public long getTimestampGuardado() {
        return timestampGuardado;
    }

    public String getNotaSesionVolatil() {
        return notaSesionVolatil;
    }

    public void setNotaSesionVolatil(String notaSesionVolatil) {
        this.notaSesionVolatil = notaSesionVolatil;
    }
}

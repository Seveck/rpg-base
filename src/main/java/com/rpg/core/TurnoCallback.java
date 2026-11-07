package com.rpg.core;

/**
 * Notificacion cuando el tiempo asignado a un turno se agota.
 */
@FunctionalInterface
public interface TurnoCallback {
    void onTiempoAgotado();
}

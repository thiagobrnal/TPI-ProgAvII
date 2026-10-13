package com.gestionamateur.tpi.progav.modelo;

public class TiposDatos {

    public enum Rol {
        ADMINISTRADOR("Administrador del Sistema"),
        ORGANIZADOR("Organizador del Torneo"),
        ARBITRO("Árbitro Oficial"),
        CAPITAN("Capitán de Equipo"),
        PUBLICO("Público General/Invitado");

        private final String descripcion;
        
        Rol(String descripcion) { this.descripcion = descripcion; }
        @Override public String toString() { return descripcion; }
    }
    
    public enum FormatoTorneo {
        LIGA("Formato de Liga (Todos vs Todos)"),
        ELIMINATORIA("Eliminatoria Directa (Playoffs)"),
        GRUPOS("Fase de Grupos");

        private final String descripcion;
        
        FormatoTorneo(String descripcion) { this.descripcion = descripcion; }
        @Override public String toString() { return descripcion; }
    }
    
    public enum EstadoJugador {
        ACTIVO("Activo y Habilitado"),
        SUSPENDIDO("Suspendido Disciplinariamente"),
        LESIONADO("Lesionado / Parte Médico"),
        INHABILITADO("Inhabilitado (Faltas o Deudas)");

        private final String descripcion;
        
        EstadoJugador(String descripcion) { this.descripcion = descripcion; }
        @Override public String toString() { return descripcion; }
    }
    
    public enum EstadoPartido {
        PROGRAMADO("Programado"),
        EN_CURSO("En Curso"),
        FINALIZADO("Finalizado"),
        SUSPENDIDO("Suspendido por Incidencias"),
        POSTERGADO("Postergado / Reprogramado");

        private final String descripcion;
        
        EstadoPartido(String descripcion) { this.descripcion = descripcion; }
        @Override public String toString() { return descripcion; }
    }
    
    public enum TipoAccion {
        GOL("Gol Anotado"),
        TARJETA_AMARILLA("Tarjeta Amarilla"),
        TARJETA_ROJA("Tarjeta Roja"),
        CAMBIO("Sustitución de Jugador");

        private final String descripcion;
        
        TipoAccion(String descripcion) { this.descripcion = descripcion; }
        @Override public String toString() { return descripcion; }
    }
}
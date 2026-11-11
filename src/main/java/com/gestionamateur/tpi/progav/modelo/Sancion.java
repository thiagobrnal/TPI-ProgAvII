package com.gestionamateur.tpi.progav.modelo;

import java.util.Date;
import java.util.logging.Logger;

public class Sancion {
    
    // 1. Logger para auditar las medidas disciplinarias
    private static final Logger LOGGER = Logger.getLogger(Sancion.class.getName());

    private Long id;
    private Date fechaInicio;
    private Date fechaFin;
    private Date fechaPublicacionFormal;
    private String motivo;
    private String gravedad;
    private Jugador jugadorSancionado;

    // Atributos de compatibilidad con modelo relacional consolidado INF-03
    private Partido partidoOrigen;
    private Torneo torneo;
    private String alcance = "TORNEO";
    private String tipoOrigen = "ADMINISTRATIVA";
    private int partidosSuspension = 1;
    private int fechasCumplidas = 0;
    private String estado = "PENDIENTE";

    // Constructor vacío
    public Sancion() {
        this.alcance = "TORNEO";
        this.tipoOrigen = "ADMINISTRATIVA";
        this.partidosSuspension = 1;
        this.fechasCumplidas = 0;
        this.estado = "PENDIENTE";
    }

    // 2. Constructor original compatible
    public Sancion(Date fechaInicio, Date fechaFin, String motivo, String gravedad, Jugador jugadorSancionado) throws TorneoExcepcion {
        setFechaInicio(fechaInicio);
        setFechaFin(fechaFin);
        setMotivo(motivo);
        setGravedad(gravedad);
        setJugadorSancionado(jugadorSancionado);
        this.alcance = "TORNEO";
        this.tipoOrigen = "ADMINISTRATIVA";
        this.partidosSuspension = 1;
        this.fechasCumplidas = 0;
        this.estado = "PENDIENTE";
        
        LOGGER.info("Se ha aplicado una sanción de gravedad '" + this.gravedad + "' al jugador ID " + this.jugadorSancionado.getId());
    }

    // 3. Constructor completo consolidado
    public Sancion(Date fechaInicio, Date fechaFin, String motivo, String gravedad, Jugador jugadorSancionado,
                   Partido partidoOrigen, Torneo torneo, String alcance, String tipoOrigen,
                   int partidosSuspension, int fechasCumplidas, String estado) throws TorneoExcepcion {
        setFechaInicio(fechaInicio);
        setFechaFin(fechaFin);
        setMotivo(motivo);
        setGravedad(gravedad);
        setJugadorSancionado(jugadorSancionado);
        setPartidoOrigen(partidoOrigen);
        setTorneo(torneo);
        setAlcance(alcance);
        setTipoOrigen(tipoOrigen);
        setPartidosSuspension(partidosSuspension);
        setFechasCumplidas(fechasCumplidas);
        setEstado(estado);

        LOGGER.info("Se ha registrado una sanción disciplinaria (" + this.tipoOrigen + ") para el jugador ID " + this.jugadorSancionado.getId());
    }

    public Sancion(Date fechaInicio, Date fechaFin, String motivo, String gravedad, Jugador jugadorSancionado,
                   Partido partidoOrigen, Torneo torneo, String alcance, String tipoOrigen,
                   int partidosSuspension, int fechasCumplidas, String estado, Date fechaPublicacionFormal) throws TorneoExcepcion {
        this(fechaInicio, fechaFin, motivo, gravedad, jugadorSancionado, partidoOrigen, torneo, alcance, tipoOrigen, partidosSuspension, fechasCumplidas, estado);
        setFechaPublicacionFormal(fechaPublicacionFormal);
    }

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public Date getFechaInicio() { return fechaInicio; }
    
    public void setFechaInicio(Date fechaInicio) throws TorneoExcepcion { 
        if (fechaInicio == null) {
            LOGGER.warning("Validación fallida: Intento de registrar sanción sin fecha de inicio.");
            throw new TorneoExcepcion("La fecha de inicio de la sanción es obligatoria.");
        }
        this.fechaInicio = fechaInicio; 
    }
    
    public Date getFechaFin() { return fechaFin; }
    
    public void setFechaFin(Date fechaFin) throws TorneoExcepcion { 
        if (fechaFin != null && this.fechaInicio != null && fechaFin.before(this.fechaInicio)) {
            LOGGER.severe("Incoherencia temporal: Fecha de fin de sanción anterior a la de inicio.");
            throw new TorneoExcepcion("La fecha de finalización de la sanción no puede ser anterior a la fecha en la que inició.");
        }
        this.fechaFin = fechaFin; 
    }

    public Date getFechaPublicacionFormal() { return fechaPublicacionFormal; }
    public void setFechaPublicacionFormal(Date fechaPublicacionFormal) throws TorneoExcepcion {
        if (fechaPublicacionFormal != null && this.fechaInicio != null && fechaPublicacionFormal.before(this.fechaInicio)) {
            throw new TorneoExcepcion("La fecha de publicación formal no puede ser anterior a la fecha de inicio.");
        }
        this.fechaPublicacionFormal = fechaPublicacionFormal;
    }
    
    public String getMotivo() { return motivo; }
    
    public void setMotivo(String motivo) throws TorneoExcepcion { 
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new TorneoExcepcion("El motivo de la sanción es obligatorio según el reglamento.");
        }
        if (motivo.trim().length() < 10) {
            throw new TorneoExcepcion("El motivo de la sanción debe ser detallado y contener al menos 10 caracteres.");
        }
        this.motivo = motivo; 
    }
    
    public String getGravedad() { return gravedad; }
    
    public void setGravedad(String gravedad) throws TorneoExcepcion { 
        if (gravedad == null || gravedad.trim().isEmpty()) {
            throw new TorneoExcepcion("Debe clasificar la gravedad de la sanción (Ej: Leve, Moderada, Grave).");
        }
        this.gravedad = gravedad; 
    }
    
    public Jugador getJugadorSancionado() { return jugadorSancionado; }
    
    public void setJugadorSancionado(Jugador jugadorSancionado) throws TorneoExcepcion { 
        if (jugadorSancionado == null) {
            LOGGER.severe("Validación fallida: Sanción fantasma. No se asignó a ningún jugador.");
            throw new TorneoExcepcion("Toda sanción debe estar vinculada obligatoriamente a un jugador válido.");
        }
        this.jugadorSancionado = jugadorSancionado; 
    }

    public Partido getPartidoOrigen() { return partidoOrigen; }
    public void setPartidoOrigen(Partido partidoOrigen) { this.partidoOrigen = partidoOrigen; }

    public Torneo getTorneo() { return torneo; }
    public void setTorneo(Torneo torneo) { this.torneo = torneo; }

    public String getAlcance() { return alcance; }
    public void setAlcance(String alcance) throws TorneoExcepcion {
        if (alcance == null || (!alcance.equals("TORNEO") && !alcance.equals("GLOBAL"))) {
            throw new TorneoExcepcion("Alcance de sanción inválido: debe ser 'TORNEO' o 'GLOBAL'.");
        }
        this.alcance = alcance;
    }

    public String getTipoOrigen() { return tipoOrigen; }
    public void setTipoOrigen(String tipoOrigen) throws TorneoExcepcion {
        if (tipoOrigen == null || (!tipoOrigen.equals("ROJA_DIRECTA") && !tipoOrigen.equals("DOBLE_AMARILLA")
                && !tipoOrigen.equals("ACUMULACION_AMARILLAS") && !tipoOrigen.equals("ADMINISTRATIVA"))) {
            throw new TorneoExcepcion("Tipo de origen inválido: debe ser ROJA_DIRECTA, DOBLE_AMARILLA, ACUMULACION_AMARILLAS o ADMINISTRATIVA.");
        }
        this.tipoOrigen = tipoOrigen;
    }

    public int getPartidosSuspension() { return partidosSuspension; }
    public void setPartidosSuspension(int partidosSuspension) throws TorneoExcepcion {
        if (partidosSuspension < 1) {
            throw new TorneoExcepcion("La cantidad de partidos de suspensión debe ser mayor o igual a 1.");
        }
        this.partidosSuspension = partidosSuspension;
    }

    public int getFechasCumplidas() { return fechasCumplidas; }
    public void setFechasCumplidas(int fechasCumplidas) throws TorneoExcepcion {
        if (fechasCumplidas < 0) {
            throw new TorneoExcepcion("Las fechas cumplidas no pueden ser negativas.");
        }
        if (fechasCumplidas > this.partidosSuspension) {
            throw new TorneoExcepcion("Las fechas cumplidas (" + fechasCumplidas + ") no pueden exceder los partidos de suspensión (" + this.partidosSuspension + ").");
        }
        this.fechasCumplidas = fechasCumplidas;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) throws TorneoExcepcion {
        if (estado == null || (!estado.equals("PENDIENTE") && !estado.equals("ACTIVA")
                && !estado.equals("CUMPLIDA") && !estado.equals("ANULADA"))) {
            throw new TorneoExcepcion("Estado de sanción inválido: debe ser PENDIENTE, ACTIVA, CUMPLIDA o ANULADA.");
        }
        this.estado = estado;
    }
}

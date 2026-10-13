package com.gestionamateur.tpi.progav.modelo;

import java.util.Date;
import java.util.logging.Logger;

public class Sancion {
    
    // 1. Logger para auditar las medidas disciplinarias
    private static final Logger LOGGER = Logger.getLogger(Sancion.class.getName());

    private Long id;
    private Date fechaInicio;
    private Date fechaFin;
    private String motivo;
    private String gravedad;
    private Jugador jugadorSancionado;

    // Constructor vacío (obligatorio para JPA)
    public Sancion() {}

    // 2. Constructor completo: Nace con todos los datos validados
    public Sancion(Date fechaInicio, Date fechaFin, String motivo, String gravedad, Jugador jugadorSancionado) throws TorneoExcepcion {
        setFechaInicio(fechaInicio);
        setFechaFin(fechaFin);
        setMotivo(motivo);
        setGravedad(gravedad);
        setJugadorSancionado(jugadorSancionado);
        
        LOGGER.info("Se ha aplicado una sanción de gravedad '" + this.gravedad + "' al jugador ID " + this.jugadorSancionado.getId());
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
    
    // 3. Validación cronológica para evitar viajes en el tiempo
    public void setFechaFin(Date fechaFin) throws TorneoExcepcion { 
        if (fechaFin == null) {
            LOGGER.warning("Validación fallida: Intento de registrar sanción sin fecha de fin.");
            throw new TorneoExcepcion("Debe especificar la fecha de finalización de la sanción.");
        }
        if (this.fechaInicio != null && fechaFin.before(this.fechaInicio)) {
            LOGGER.severe("Incoherencia temporal: Fecha de fin de sanción anterior a la de inicio.");
            throw new TorneoExcepcion("La fecha de finalización de la sanción no puede ser anterior a la fecha en la que inició.");
        }
        this.fechaFin = fechaFin; 
    }
    
    public String getMotivo() { return motivo; }
    
    public void setMotivo(String motivo) throws TorneoExcepcion { 
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new TorneoExcepcion("El motivo de la sanción es obligatorio según el reglamento.");
        }
        if (motivo.length() < 10) {
            throw new TorneoExcepcion("El motivo debe explicar claramente la infracción cometida (mínimo 10 caracteres).");
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
}